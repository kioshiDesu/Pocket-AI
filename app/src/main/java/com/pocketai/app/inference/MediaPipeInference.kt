package com.pocketai.app.inference

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInference.LlmInferenceOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File

data class InferenceState(
    val isLoaded: Boolean = false,
    val modelName: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val modelPath: String = ""
)

data class ChatMessage(
    val id: Long = System.currentTimeMillis(),
    val content: String,
    val isFromUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

class MediaPipeInference(private val context: Context) {

    private var llmInference: LlmInference? = null

    private val _state = MutableStateFlow(InferenceState())
    val state: StateFlow<InferenceState> = _state.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    suspend fun loadModel(modelPath: String, modelName: String): Result<Unit> = 
        withContext(Dispatchers.IO) {
            try {
                _state.value = InferenceState(isLoading = true, modelName = modelName, modelPath = modelPath)

                val modelFile = File(modelPath)
                if (!modelFile.exists()) {
                    _state.value = InferenceState(error = "Model file not found: $modelPath")
                    return@withContext Result.failure(Exception("Model file not found"))
                }

                llmInference?.close()

                val options = LlmInferenceOptions.builder()
                    .setModelPath(modelPath)
                    .setMaxTokens(2048)
                    .setTopK(40)
                    .setTopP(0.95f)
                    .setTemperature(0.7f)
                    .build()

                llmInference = LlmInference.createFromFile(context, options)

                _state.value = InferenceState(
                    isLoaded = true,
                    modelName = modelName,
                    modelPath = modelPath
                )

                Result.success(Unit)

            } catch (e: Exception) {
                val error = "Failed to load model: ${e.message}"
                _state.value = InferenceState(error = error)
                Result.failure(Exception(error))
            }
        }

    suspend fun generateResponse(
        prompt: String,
        systemPrompt: String = "",
        onToken: (String) -> Unit = {}
    ): String = withContext(Dispatchers.IO) {
        val inference = llmInference
            ?: return@withContext "Error: No model loaded. Please download and load a model first."

        try {
            val messages = _messages.value.toMutableList()
            messages.add(ChatMessage(content = prompt, isFromUser = true))
            _messages.value = messages

            val chatPrompt = buildChatPrompt(messages, systemPrompt)
            val responseBuilder = StringBuilder()

            inference.generateResponse(chatPrompt) { partialResult ->
                responseBuilder.append(partialResult)
                onToken(partialResult)
            }

            val response = responseBuilder.toString().trim()

            val updatedMessages = _messages.value.toMutableList()
            updatedMessages.add(ChatMessage(content = response, isFromUser = false))
            _messages.value = updatedMessages

            response

        } catch (e: Exception) {
            "Error generating response: ${e.message}"
        }
    }

    private fun buildChatPrompt(messages: List<ChatMessage>, systemPrompt: String): String {
        return buildString {
            if (systemPrompt.isNotBlank()) {
                appendLine(systemPrompt)
                appendLine()
            }

            val recentMessages = messages.takeLast(20)
            for (msg in recentMessages) {
                if (msg.isFromUser) {
                    appendLine("User: ${msg.content}")
                } else {
                    appendLine("Assistant: ${msg.content}")
                }
            }
            append("Assistant:")
        }
    }

    fun clearChat() {
        _messages.value = emptyList()
    }

    fun unloadModel() {
        llmInference?.close()
        llmInference = null
        _state.value = InferenceState()
    }

    fun isModelLoaded(): Boolean = _state.value.isLoaded

    fun getCurrentState(): InferenceState = _state.value
}
