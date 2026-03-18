package com.pocketai.app.ui.screens

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocketai.app.data.repository.ChatRepository
import com.pocketai.app.domain.model.Conversation
import com.pocketai.app.domain.model.Message
import com.pocketai.app.domain.model.ModelState
import com.pocketai.app.inference.LlmInferenceEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatUiState(
    val conversation: Conversation? = null,
    val messages: List<Message> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val llmInferenceEngine: LlmInferenceEngine,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    val modelState: StateFlow<ModelState> = llmInferenceEngine.modelState

    private var currentConversationId: Long = 0

    fun loadConversation(conversationId: Long) {
        currentConversationId = conversationId
        viewModelScope.launch {
            val conversation = chatRepository.getConversationById(conversationId)
            _uiState.update { it.copy(conversation = conversation) }

            chatRepository.getMessagesForConversation(conversationId).collect { messages ->
                _uiState.update { it.copy(messages = messages) }
            }
        }
    }

    fun sendMessage(userInput: String) {
        if (userInput.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val userMessage = Message(
                conversationId = currentConversationId,
                content = userInput,
                isFromUser = true
            )
            chatRepository.addMessage(userMessage)

            val modelState = llmInferenceEngine.modelState.value
            if (modelState !is ModelState.Loaded) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Please load a model first"
                    )
                }
                return@launch
            }

            val systemPrompt = llmInferenceEngine.getModelConfig()?.systemPrompt 
                ?: "You are a helpful AI assistant."

            val recentMessages = chatRepository.getRecentMessages(currentConversationId, 10)
            val conversationHistory = if (recentMessages.isNotEmpty()) {
                recentMessages.joinToString("\n") { msg ->
                    "${if (msg.isFromUser) "User" else "Assistant"}: ${msg.content}"
                }
            } else ""

            val enhancedPrompt = if (conversationHistory.isNotEmpty()) {
                "Previous conversation:\n$conversationHistory\n\nUser: $userInput\nAssistant:"
            } else {
                "User: $userInput\nAssistant:"
            }

            val response = llmInferenceEngine.generateResponse(
                prompt = enhancedPrompt,
                systemPrompt = systemPrompt
            )

            if (response.isComplete && response.text.isNotBlank()) {
                val assistantMessage = Message(
                    conversationId = currentConversationId,
                    content = response.text,
                    isFromUser = false
                )
                chatRepository.addMessage(assistantMessage)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        conversation = it.conversation?.copy(
                            title = if (it.conversation?.title == "New Conversation") 
                                userInput.take(30) 
                            else it.conversation?.title ?: "New Conversation"
                        )
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = response.error ?: "Failed to generate response"
                    )
                }
            }
        }
    }

    fun loadModelFromUri(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                val documentFile = DocumentFile.fromSingleUri(context, uri)
                val fileName = documentFile?.name ?: "model.task"

                val cacheDir = context.cacheDir
                val modelFile = java.io.File(cacheDir, fileName)

                context.contentResolver.openInputStream(uri)?.use { input ->
                    java.io.FileOutputStream(modelFile).use { output ->
                        input.copyTo(output)
                    }
                }

                val result = llmInferenceEngine.loadModel(
                    modelPath = modelFile.absolutePath,
                    modelName = fileName
                )

                result.onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Failed to load model: ${e.message}"
                        )
                    }
                }

                if (result.isSuccess) {
                    _uiState.update { it.copy(isLoading = false) }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Error loading model: ${e.message}"
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
