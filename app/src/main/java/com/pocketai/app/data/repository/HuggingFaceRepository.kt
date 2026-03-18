package com.pocketai.app.data.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.pocketai.app.data.model.HuggingFaceModel
import com.pocketai.app.data.model.ModelFile
import com.pocketai.app.data.model.ModelFilter
import com.pocketai.app.data.model.ModelSearchParams
import com.pocketai.app.data.model.ModelSort
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class HuggingFaceRepository {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    private val baseUrl = "https://huggingface.co/api"
    private val downloadBaseUrl = "https://huggingface.co"

    suspend fun searchModels(params: ModelSearchParams): Result<List<HuggingFaceModel>> = 
        withContext(Dispatchers.IO) {
            try {
                val url = buildSearchUrl(params)
                val request = Request.Builder()
                    .url(url)
                    .addHeader("Accept", "application/json")
                    .build()

                val response = httpClient.newCall(request).execute()

                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("API error: ${response.code}"))
                }

                val json = response.body?.string() ?: "[]"
                val type = object : TypeToken<List<HuggingFaceModel>>() {}.type
                val models: List<HuggingFaceModel> = gson.fromJson(json, type)

                val filteredModels = filterModels(models, params.filter)

                Result.success(filteredModels)

            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun getModelDetails(modelId: String, authToken: String? = null): Result<HuggingFaceModel> =
        withContext(Dispatchers.IO) {
            try {
                val encodedId = modelId.replace("/", "%2F")
                val url = "$baseUrl/models/$encodedId"

                val requestBuilder = Request.Builder()
                    .url(url)
                    .addHeader("Accept", "application/json")

                authToken?.let { requestBuilder.addHeader("Authorization", "Bearer $it") }

                val response = httpClient.newCall(requestBuilder.build()).execute()

                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("API error: ${response.code}"))
                }

                val json = response.body?.string() ?: return@withContext Result.failure(Exception("Empty response"))
                val model: HuggingFaceModel = gson.fromJson(json, HuggingFaceModel::class.java)

                Result.success(model)

            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    fun getModelFileDownloadUrl(modelId: String, fileName: String): String {
        return "$downloadBaseUrl/$modelId/resolve/main/$fileName"
    }

    fun getGemmaModels(): List<HuggingFaceModel> = listOf(
        HuggingFaceModel(
            id = "litert-community/Gemma3-1B-IT",
            downloads = 50000,
            likes = 200,
            tags = listOf("text-generation", "gemma", "on-device"),
            siblings = listOf(
                ModelFile(rfilename = "gemma3-1b-it-int4.task", size = 670000000),
                ModelFile(rfilename = "gemma3-1b-it-int8.task", size = 1200000000)
            )
        ),
        HuggingFaceModel(
            id = "litert-community/Gemma3-2B-IT",
            downloads = 30000,
            likes = 150,
            tags = listOf("text-generation", "gemma", "on-device"),
            siblings = listOf(
                ModelFile(rfilename = "gemma3-2b-it-int4.task", size = 1400000000),
                ModelFile(rfilename = "gemma3-2b-it-int8.task", size = 2800000000)
            )
        ),
        HuggingFaceModel(
            id = "google/gemma-3-1b-it",
            downloads = 100000,
            likes = 500,
            tags = listOf("text-generation", "gemma"),
            siblings = listOf(
                ModelFile(rfilename = "model-q4_0.gguf", size = 800000000),
                ModelFile(rfilename = "model-q4_k_m.gguf", size = 850000000)
            )
        )
    )

    private fun buildSearchUrl(params: ModelSearchParams): String {
        val sortParam = when (params.sort) {
            ModelSort.TRENDING -> "trending"
            ModelSort.MOST_DOWNLOADED -> "downloads"
            ModelSort.MOST_LIKED -> "likes"
            ModelSort.RECENTLY_UPDATED -> "lastModified"
            ModelSort.ALPHABETICAL -> "name"
        }

        return buildString {
            append("$baseUrl/models?")
            append("sort=$sortParam")
            append("&limit=${params.limit}")
            append("&full=true")

            if (params.query.isNotBlank()) {
                append("&search=${params.query.replace(" ", "+")}")
            }

            when (params.filter) {
                ModelFilter.TASK_ONLY -> append("&tags=task")
                ModelFilter.GGUF_ONLY -> append("&tags=gguf")
                ModelFilter.GGUF_AND_TASK -> append("&tags=task,gguf")
                ModelFilter.ALL -> {}
            }
        }
    }

    private fun filterModels(models: List<HuggingFaceModel>, filter: ModelFilter): List<HuggingFaceModel> {
        return when (filter) {
            ModelFilter.ALL -> models
            ModelFilter.TASK_ONLY -> models.filter { model ->
                model.siblings?.any { it.isTaskFile } == true ||
                model.tags.any { it.contains("task") || it.contains("on-device") }
            }
            ModelFilter.GGUF_ONLY -> models.filter { model ->
                model.siblings?.any { it.isGgufFile } == true ||
                model.tags.any { it.contains("gguf") }
            }
            ModelFilter.GGUF_AND_TASK -> models.filter { model ->
                model.siblings?.any { it.isSupported } == true ||
                model.tags.any { it.contains("gguf") || it.contains("task") }
            }
        }
    }
}
