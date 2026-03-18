package com.pocketai.app.data.repository

import com.pocketai.app.data.local.ConversationDao
import com.pocketai.app.data.local.MessageDao
import com.pocketai.app.domain.model.Conversation
import com.pocketai.app.domain.model.Message
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao
) {
    fun getAllConversations(): Flow<List<Conversation>> = 
        conversationDao.getAllConversations()

    suspend fun getConversationById(id: Long): Conversation? =
        conversationDao.getConversationById(id)

    suspend fun createConversation(title: String = "New Conversation"): Long {
        val conversation = Conversation(title = title)
        return conversationDao.insertConversation(conversation)
    }

    suspend fun updateConversation(conversation: Conversation) =
        conversationDao.updateConversation(conversation)

    suspend fun deleteConversation(conversationId: Long) =
        conversationDao.deleteConversationById(conversationId)

    suspend fun deleteAllConversations() =
        conversationDao.deleteAllConversations()

    fun getMessagesForConversation(conversationId: Long): Flow<List<Message>> =
        messageDao.getMessagesForConversation(conversationId)

    suspend fun getMessagesForConversationSync(conversationId: Long): List<Message> =
        messageDao.getMessagesForConversationSync(conversationId)

    suspend fun addMessage(message: Message): Long {
        val result = messageDao.insertMessage(message)
        conversationDao.getConversationById(message.conversationId)?.let { conv ->
            conversationDao.updateConversation(
                conv.copy(updatedAt = System.currentTimeMillis())
            )
        }
        return result
    }

    suspend fun deleteMessagesForConversation(conversationId: Long) =
        messageDao.deleteMessagesForConversation(conversationId)

    suspend fun getRecentMessages(conversationId: Long, limit: Int = 10): List<Message> =
        messageDao.getRecentMessages(conversationId, limit)
}
