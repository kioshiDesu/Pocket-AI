package com.pocketai.app.data.download

import android.content.Context
import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

data class DownloadProgress(
    val fileName: String,
    val bytesDownloaded: Long,
    val totalBytes: Long,
    val isComplete: Boolean,
    val error: String? = null
) {
    val progress: Float get() = if (totalBytes > 0) bytesDownloaded.toFloat() / totalBytes else 0f
    val progressPercent: Int get() = (progress * 100).toInt()
    val sizeFormatted: String get() = "${formatBytes(bytesDownloaded)} / ${formatBytes(totalBytes)}"

    private fun formatBytes(bytes: Long): String = when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
        else -> String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
    }
}

data class DownloadedModel(
    val fileName: String,
    val filePath: String,
    val fileSize: Long,
    val downloadDate: Long,
    val modelName: String
)

class DownloadManager(private val context: Context) {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val _downloadProgress = MutableStateFlow<DownloadProgress?>(null)
    val downloadProgress: StateFlow<DownloadProgress?> = _downloadProgress.asStateFlow()

    private val _downloadedModels = MutableStateFlow<List<DownloadedModel>>(emptyList())
    val downloadedModels: StateFlow<List<DownloadedModel>> = _downloadedModels.asStateFlow()

    private val modelsDir: File
        get() {
            val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "models")
            if (!dir.exists()) dir.mkdirs()
            return dir
        }

    init {
        refreshDownloadedModels()
    }

    fun refreshDownloadedModels() {
        val models = modelsDir.listFiles()?.filter { it.isFile }?.map { file ->
            DownloadedModel(
                fileName = file.name,
                filePath = file.absolutePath,
                fileSize = file.length(),
                downloadDate = file.lastModified(),
                modelName = file.nameWithoutExtension
            )
        }?.sortedByDescending { it.downloadDate } ?: emptyList()
        _downloadedModels.value = models
    }

    suspend fun downloadModel(
        url: String,
        fileName: String,
        authToken: String? = null
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val outputFile = File(modelsDir, fileName)

            if (outputFile.exists()) {
                _downloadProgress.value = DownloadProgress(
                    fileName = fileName,
                    bytesDownloaded = outputFile.length(),
                    totalBytes = outputFile.length(),
                    isComplete = true
                )
                refreshDownloadedModels()
                return@withContext Result.success(outputFile)
            }

            val tempFile = File(modelsDir, "$fileName.tmp")

            val requestBuilder = Request.Builder().url(url)
            authToken?.let { requestBuilder.addHeader("Authorization", "Bearer $it") }

            val response = httpClient.newCall(requestBuilder.build()).execute()

            if (!response.isSuccessful) {
                val error = "Download failed: HTTP ${response.code}"
                _downloadProgress.value = DownloadProgress(
                    fileName = fileName,
                    bytesDownloaded = 0,
                    totalBytes = 0,
                    isComplete = false,
                    error = error
                )
                return@withContext Result.failure(Exception(error))
            }

            val totalBytes = response.body?.contentLength() ?: -1
            var bytesDownloaded = 0L

            response.body?.byteStream()?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        bytesDownloaded += bytesRead

                        _downloadProgress.value = DownloadProgress(
                            fileName = fileName,
                            bytesDownloaded = bytesDownloaded,
                            totalBytes = totalBytes,
                            isComplete = false
                        )
                    }

                    output.flush()
                }
            }

            tempFile.renameTo(outputFile)

            _downloadProgress.value = DownloadProgress(
                fileName = fileName,
                bytesDownloaded = bytesDownloaded,
                totalBytes = totalBytes,
                isComplete = true
            )

            refreshDownloadedModels()
            Result.success(outputFile)

        } catch (e: Exception) {
            _downloadProgress.value = DownloadProgress(
                fileName = fileName,
                bytesDownloaded = 0,
                totalBytes = 0,
                isComplete = false,
                error = e.message
            )
            Result.failure(e)
        }
    }

    fun deleteModel(filePath: String): Boolean {
        val file = File(filePath)
        return if (file.exists()) {
            val deleted = file.delete()
            if (deleted) refreshDownloadedModels()
            deleted
        } else false
    }

    fun clearProgress() {
        _downloadProgress.value = null
    }
}
