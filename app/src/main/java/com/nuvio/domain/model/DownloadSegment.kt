package com.nuvio.domain.model

data class DownloadSegment(
    val id: String,
    val taskId: String,
    val segmentIndex: Int,
    val startByte: Long,
    val endByte: Long,
    val downloadedBytes: Long = 0L,
    val status: SegmentStatus = SegmentStatus.PENDING
) {
    val totalBytes: Long
        get() = (endByte - startByte + 1).coerceAtLeast(0)

    val isCompleted: Boolean
        get() = downloadedBytes >= totalBytes && totalBytes > 0
}
