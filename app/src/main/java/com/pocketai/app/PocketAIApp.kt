package com.pocketai.app

import android.app.Application
import com.pocketai.app.data.download.DownloadManager
import com.pocketai.app.data.repository.HuggingFaceRepository
import com.pocketai.app.inference.MediaPipeInference
import com.pocketai.app.utils.SettingsManager

class PocketAIApp : Application() {

    lateinit var downloadManager: DownloadManager
        private set

    lateinit var huggingFaceRepo: HuggingFaceRepository
        private set

    lateinit var inferenceEngine: MediaPipeInference
        private set

    lateinit var settingsManager: SettingsManager
        private set

    override fun onCreate() {
        super.onCreate()
        downloadManager = DownloadManager(this)
        huggingFaceRepo = HuggingFaceRepository()
        inferenceEngine = MediaPipeInference(this)
        settingsManager = SettingsManager(this)
    }
}
