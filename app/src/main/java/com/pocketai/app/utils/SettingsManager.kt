package com.pocketai.app.utils

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

private val Context.dataStore by preferencesDataStore(name = "pocketai_settings")

class SettingsManager(private val context: Context) {

    companion object {
        private val HF_AUTH_TOKEN = stringPreferencesKey("hf_auth_token")
        private val SYSTEM_PROMPT = stringPreferencesKey("system_prompt")
        private val LAST_MODEL_PATH = stringPreferencesKey("last_model_path")
        private val LAST_MODEL_NAME = stringPreferencesKey("last_model_name")
    }

    val hfAuthToken: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[HF_AUTH_TOKEN] ?: ""
    }

    val systemPrompt: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[SYSTEM_PROMPT] ?: "You are a helpful AI assistant. Answer concisely and accurately."
    }

    val lastModelPath: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[LAST_MODEL_PATH] ?: ""
    }

    val lastModelName: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[LAST_MODEL_NAME] ?: ""
    }

    fun saveHfAuthToken(token: String) {
        runBlocking {
            context.dataStore.edit { prefs ->
                prefs[HF_AUTH_TOKEN] = token
            }
        }
    }

    fun saveSystemPrompt(prompt: String) {
        runBlocking {
            context.dataStore.edit { prefs ->
                prefs[SYSTEM_PROMPT] = prompt
            }
        }
    }

    fun saveLastModel(path: String, name: String) {
        runBlocking {
            context.dataStore.edit { prefs ->
                prefs[LAST_MODEL_PATH] = path
                prefs[LAST_MODEL_NAME] = name
            }
        }
    }

    fun getHfAuthTokenSync(): String {
        return runBlocking {
            hfAuthToken.first()
        }
    }

    fun getSystemPromptSync(): String {
        return runBlocking {
            systemPrompt.first()
        }
    }
}
