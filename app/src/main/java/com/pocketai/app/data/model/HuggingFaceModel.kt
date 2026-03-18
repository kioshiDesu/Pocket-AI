package com.pocketai.app.data.model

import com.google.gson.annotations.SerializedName

data class HuggingFaceModel(
    val id: String,
    val author: String? = null,
    val downloads: Long = 0,
    val likes: Long = 0,
    val lastModified: String? = null,
    val tags: List<String> = emptyList(),
    @SerializedName("modelId") val modelId: String? = null,
    val siblings: List<ModelFile>? = null,
    val cardData: CardData? = null,
    val sha: String? = null
) {
    val name: String get() = id.split("/").lastOrNull() ?: id
    val displayName: String get() = id
}

data class ModelFile(
    val rfilename: String,
    val size: Long = 0,
    val blobId: String? = null
) {
    val isTaskFile: Boolean get() = rfilename.endsWith(".task")
    val isGgufFile: Boolean get() = rfilename.endsWith(".gguf")
    val isSupported: Boolean get() = isTaskFile || isGgufFile
    val sizeFormatted: String get() = formatFileSize(size)

    private fun formatFileSize(bytes: Long): String = when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
        else -> "${bytes / (1024 * 1024 * 1024)} GB"
    }
}

data class CardData(
    val language: List<String>? = null,
    val license: String? = null,
    val tags: List<String>? = null,
    val datasets: List<String>? = null
)

data class HuggingFaceSearchResponse(
    val models: List<HuggingFaceModel>,
    val numItemsOnPage: Int = 0,
    val numTotalItems: Int = 0
)

enum class ModelSort {
    TRENDING,
    MOST_DOWNLOADED,
    MOST_LIKED,
    RECENTLY_UPDATED,
    ALPHABETICAL
}

enum class ModelFilter {
    ALL,
    TASK_ONLY,
    GGUF_ONLY,
    GGUF_AND_TASK
}

data class ModelSearchParams(
    val query: String = "",
    val sort: ModelSort = ModelSort.TRENDING,
    val filter: ModelFilter = ModelFilter.TASK_ONLY,
    val limit: Int = 20,
    val page: Int = 0
)
