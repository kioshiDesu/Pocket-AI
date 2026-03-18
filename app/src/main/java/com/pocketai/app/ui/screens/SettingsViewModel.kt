package com.pocketai.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocketai.app.domain.model.ModelConfig
import com.pocketai.app.domain.model.ModelState
import com.pocketai.app.inference.LlmInferenceEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val llmInferenceEngine: LlmInferenceEngine
) : ViewModel() {

    val modelState: StateFlow<ModelState> = llmInferenceEngine.modelState

    private val _modelConfig = MutableStateFlow(llmInferenceEngine.getModelConfig())
    val modelConfig: StateFlow<ModelConfig?> = _modelConfig.asStateFlow()

    fun saveSettings(
        temperature: Float,
        maxTokens: Int,
        systemPrompt: String
    ) {
        val currentConfig = llmInferenceEngine.getModelConfig() ?: ModelConfig()
        val newConfig = currentConfig.copy(
            temperature = temperature,
            maxTokens = maxTokens,
            systemPrompt = systemPrompt
        )
        llmInferenceEngine.updateConfig(newConfig)
        _modelConfig.value = newConfig
    }

    fun unloadModel() {
        llmInferenceEngine.unloadModel()
    }
}
