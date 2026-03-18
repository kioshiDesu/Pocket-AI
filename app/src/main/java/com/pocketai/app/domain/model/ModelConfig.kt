package com.pocketai.app.domain.model

data class ModelConfig(
    val modelPath: String = "",
    val modelName: String = "",
    val temperature: Float = 0.7f,
    val maxTokens: Int = 1024,
    val topK: Int = 40,
    val topP: Float = 0.95f,
    val systemPrompt: String = "You are a helpful AI assistant."
)

data class LlmResponse(
    val text: String,
    val isComplete: Boolean,
    val error: String? = null
)

sealed class ModelState {
    object NotLoaded : ModelState()
    object Loading : ModelState()
    data class Loaded(val modelName: String) : ModelState()
    data class Error(val message: String) : ModelState()
}
