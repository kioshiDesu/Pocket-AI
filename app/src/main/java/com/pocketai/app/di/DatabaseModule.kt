package com.pocketai.app.di

import android.content.Context
import androidx.room.Room
import com.pocketai.app.data.local.ConversationDao
import com.pocketai.app.data.local.MessageDao
import com.pocketai.app.data.local.PocketAIDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): PocketAIDatabase {
        return Room.databaseBuilder(
            context,
            PocketAIDatabase::class.java,
            "pocketai_database"
        ).build()
    }

    @Provides
    fun provideConversationDao(database: PocketAIDatabase): ConversationDao {
        return database.conversationDao()
    }

    @Provides
    fun provideMessageDao(database: PocketAIDatabase): MessageDao {
        return database.messageDao()
    }
}
