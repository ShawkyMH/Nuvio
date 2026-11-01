package com.nuvio.domain.model

data class DownloadTask(
    val id: String,
    val url: String,
    val title: String,
    val filePath: String,
    val mimeType: String = "application/octet-stream",
    val totalBytes: Long = -1L,
    val downloadedBytes: Long = 0L,
    val status: TaskStatus = TaskStatus.CREATED,
    val supportsRanges: Boolean = false,
    val etag: String? = null,
    val lastModified: String? = null,
    val connectionCount: Int = 1,
    val downloadSpeed: Long = 0L, // bytes per second
    val isMediaExtraction: Boolean = false,
    val formatId: String? = null,
    val thumbnailUri: String? = null,
    val errorMessage: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
) {
    val progress: Float
        get() = if (totalBytes > 0) (downloadedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f) else 0f

    val progressPercent: Int
        get() = (progress * 100).toInt()
}
