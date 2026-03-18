package com.pocketai.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.pocketai.app.domain.model.Conversation
import com.pocketai.app.domain.model.Message

@Database(
    entities = [Conversation::class, Message::class],
    version = 1,
    exportSchema = false
)
abstract class PocketAIDatabase : RoomDatabase() {
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
}
