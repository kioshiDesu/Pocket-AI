package com.pocketai.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocketai.app.data.repository.ChatRepository
import com.pocketai.app.domain.model.Conversation
import com.pocketai.app.domain.model.ModelState
import com.pocketai.app.inference.LlmInferenceEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val llmInferenceEngine: LlmInferenceEngine
) : ViewModel() {

    val conversations: StateFlow<List<Conversation>> = chatRepository
        .getAllConversations()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val modelState: StateFlow<ModelState> = llmInferenceEngine.modelState

    fun createConversation(title: String = "New Conversation"): Long {
        var newId = 0L
        viewModelScope.launch {
            newId = chatRepository.createConversation(title)
        }
        return newId
    }

    fun deleteConversation(conversationId: Long) {
        viewModelScope.launch {
            chatRepository.deleteConversation(conversationId)
        }
    }
}
