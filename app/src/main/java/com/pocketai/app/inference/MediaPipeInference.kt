package com.pocketai.app.inference

import android.content.Context
import com.pocketai.app.utils.SettingsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class InferenceState(
    val isLoaded: Boolean = false,
    val modelName: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val modelPath: String = "",
    val aiProvider: String = "offline"
)

data class ChatMessage(
    val id: Long = System.currentTimeMillis(),
    val content: String,
    val isFromUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

class MediaPipeInference(private val context: Context) {

    private val settingsManager = SettingsManager(context)
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val _state = MutableStateFlow(InferenceState())
    val state: StateFlow<InferenceState> = _state.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    suspend fun loadModel(modelPath: String, modelName: String): Result<Unit> = 
        withContext(Dispatchers.IO) {
            try {
                val provider = settingsManager.getAiProviderSync()
                _state.value = InferenceState(
                    isLoaded = true,
                    modelName = modelName,
                    modelPath = modelPath,
                    aiProvider = provider
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
        val aiProvider = settingsManager.getAiProviderSync()
        
        // Add user message
        val messages = _messages.value.toMutableList()
        messages.add(ChatMessage(content = prompt, isFromUser = true))
        _messages.value = messages

        try {
            val response = when (aiProvider) {
                "openrouter" -> generateOpenRouterResponse(prompt, systemPrompt, onToken)
                "ollama" -> generateOllamaResponse(prompt, systemPrompt, onToken)
                else -> generateOfflineResponse(prompt, systemPrompt, onToken)
            }

            // Add AI response
            val updatedMessages = _messages.value.toMutableList()
            updatedMessages.add(ChatMessage(content = response, isFromUser = false))
            _messages.value = updatedMessages

            response

        } catch (e: Exception) {
            "Error generating response: ${e.message}"
        }
    }

    private fun generateOfflineResponse(
        prompt: String,
        systemPrompt: String,
        onToken: (String) -> Unit
    ): String {
        // Simulate AI response for offline/local models
        val response = if (_state.value.isLoaded) {
            "This is a simulated response from ${_state.value.modelName}. To enable real AI inference:\n" +
                    "1. Uncomment MediaPipe in build.gradle.kts\n" +
                    "2. Download a compatible .task model file\n" +
                    "3. The MediaPipe library will handle the actual inference"
        } else {
            "Error: No model loaded. Please download and load a model first."
        }

        // Simulate streaming
        for (char in response) {
            onToken(char.toString())
            Thread.sleep(10)
        }

        return response
    }

    private fun generateOpenRouterResponse(
        prompt: String,
        systemPrompt: String,
        onToken: (String) -> Unit
    ): String {
        val apiKey = settingsManager.getOpenRouterApiKeySync()
        if (apiKey.isEmpty()) {
            return "Error: OpenRouter API key not set. Please configure it in Settings."
        }

        try {
            val json = JSONObject().apply {
                put("model", "openai/gpt-3.5-turbo")
                put("messages", listOf(
                    JSONObject().apply {
                        put("role", "system")
                        put("content", systemPrompt.ifEmpty { "You are a helpful AI assistant." })
                    },
                    JSONObject().apply {
                        put("role", "user")
                        put("content", prompt)
                    }
                ))
            }

            val request = Request.Builder()
                .url("https://openrouter.ai/api/v1/chat/completions")
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(json.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string()

            if (!response.isSuccessful || responseBody == null) {
                return "Error: API request failed with code ${response.code}"
            }

            val jsonResponse = JSONObject(responseBody)
            val content = jsonResponse
                .getJSONObject("choices")
                .getJSONArray("messages")
                .getJSONObject(0)
                .getString("content")

            // Simulate streaming
            for (char in content) {
                onToken(char.toString())
                Thread.sleep(10)
            }

            return content

        } catch (e: Exception) {
            return "Error calling OpenRouter: ${e.message}"
        }
    }

    private fun generateOllamaResponse(
        prompt: String,
        systemPrompt: String,
        onToken: (String) -> Unit
    ): String {
        val ollamaHost = settingsManager.getOllamaHostSync()
        
        try {
            val json = JSONObject().apply {
                put("model", "llama2")
                put("prompt", prompt)
                put("system", systemPrompt.ifEmpty { "You are a helpful AI assistant." })
                put("stream", true)
            }

            val request = Request.Builder()
                .url("$ollamaHost/api/generate")
                .addHeader("Content-Type", "application/json")
                .post(json.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string()

            if (!response.isSuccessful || responseBody == null) {
                return "Error: Ollama request failed with code ${response.code}"
            }

            // Parse streaming response
            val lines = responseBody.split("\n")
            val responseBuilder = StringBuilder()
            
            for (line in lines) {
                if (line.isNotEmpty()) {
                    try {
                        val jsonResponse = JSONObject(line)
                        val responseText = jsonResponse.optString("response", "")
                        if (responseText.isNotEmpty()) {
                            responseBuilder.append(responseText)
                            onToken(responseText)
                        }
                    } catch (e: Exception) {
                        // Skip parsing errors
                    }
                }
            }

            return responseBuilder.toString()

        } catch (e: Exception) {
            return "Error calling Ollama: ${e.message}"
        }
    }

    fun clearChat() {
        _messages.value = emptyList()
    }

    fun unloadModel() {
        _state.value = InferenceState(aiProvider = settingsManager.getAiProviderSync())
    }

    fun isModelLoaded(): Boolean = _state.value.isLoaded

    fun getCurrentState(): InferenceState = _state.value
}
