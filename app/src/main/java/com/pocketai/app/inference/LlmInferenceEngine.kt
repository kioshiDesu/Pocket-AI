package com.pocketai.app.inference

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInference.LlmInferenceOptions
import com.pocketai.app.domain.model.LlmResponse
import com.pocketai.app.domain.model.ModelConfig
import com.pocketai.app.domain.model.ModelState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LlmInferenceEngine @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var llmInference: LlmInference? = null
    
    private val _modelState = MutableStateFlow<ModelState>(ModelState.NotLoaded)
    val modelState: StateFlow<ModelState> = _modelState.asStateFlow()

    private var currentConfig: ModelConfig? = null

    suspend fun loadModel(modelPath: String, modelName: String): Result<Unit> = 
        withContext(Dispatchers.IO) {
            try {
                _modelState.value = ModelState.Loading
                
                llmInference?.close()
                
                val modelFile = File(modelPath)
                if (!modelFile.exists()) {
                    val error = "Model file not found: $modelPath"
                    _modelState.value = ModelState.Error(error)
                    return@withContext Result.failure(Exception(error))
                }

                val options = LlmInferenceOptions.builder()
                    .setModelPath(modelPath)
                    .setMaxTokens(1024)
                    .setTopK(40)
                    .setTopP(0.95f)
                    .setTemperature(0.7f)
                    .build()

                llmInference = LlmInference.createFromFile(context, options)
                
                currentConfig = currentConfig?.copy(modelPath = modelPath, modelName = modelName)
                    ?: ModelConfig(modelPath = modelPath, modelName = modelName)
                
                _modelState.value = ModelState.Loaded(modelName)
                Result.success(Unit)
            } catch (e: Exception) {
                val error = "Failed to load model: ${e.message}"
                _modelState.value = ModelState.Error(error)
                Result.failure(e)
            }
        }

    suspend fun generateResponse(
        prompt: String,
        systemPrompt: String = "You are a helpful AI assistant.",
        onChunk: (String) -> Unit = {}
    ): LlmResponse = withContext(Dispatchers.IO) {
        val inference = llmInference
        if (inference == null) {
            return@withContext LlmResponse(
                text = "",
                isComplete = false,
                error = "Model not loaded"
            )
        }

        try {
            val fullPrompt = if (systemPrompt.isNotEmpty()) {
                "$systemPrompt\n\nUser: $prompt\nAssistant:"
            } else {
                "User: $prompt\nAssistant:"
            }

            val result = StringBuilder()
            
            inference.generateResponse(fullPrompt) { partialResult ->
                result.append(partialResult)
                onChunk(partialResult)
            }

            LlmResponse(
                text = result.toString(),
                isComplete = true
            )
        } catch (e: Exception) {
            LlmResponse(
                text = "",
                isComplete = false,
                error = e.message ?: "Unknown error"
            )
        }
    }

    suspend fun generateResponseWithContext(
        prompt: String,
        context: String,
        systemPrompt: String = "You are a helpful AI assistant.",
        onChunk: (String) -> Unit = {}
    ): LlmResponse = withContext(Dispatchers.IO) {
        val inference = llmInference
        if (inference == null) {
            return@withContext LlmResponse(
                text = "",
                isComplete = false,
                error = "Model not loaded"
            )
        }

        try {
            val contextPrompt = """
                Based on the following context, answer the user's question.
                
                Context:
                $context
                
                User Question: $prompt
                
                Answer:
            """.trimIndent()

            val fullPrompt = if (systemPrompt.isNotEmpty()) {
                "$systemPrompt\n\n$contextPrompt"
            } else {
                contextPrompt
            }

            val result = StringBuilder()
            
            inference.generateResponse(fullPrompt) { partialResult ->
                result.append(partialResult)
                onChunk(partialResult)
            }

            LlmResponse(
                text = result.toString(),
                isComplete = true
            )
        } catch (e: Exception) {
            LlmResponse(
                text = "",
                isComplete = false,
                error = e.message ?: "Unknown error"
            )
        }
    }

    fun unloadModel() {
        llmInference?.close()
        llmInference = null
        _modelState.value = ModelState.NotLoaded
        currentConfig = null
    }

    fun getModelConfig(): ModelConfig? = currentConfig

    fun updateConfig(config: ModelConfig) {
        currentConfig = config
    }

    fun isModelLoaded(): Boolean = llmInference != null
}
