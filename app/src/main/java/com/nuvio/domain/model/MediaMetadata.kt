package com.nuvio.domain.model

enum class FormatType {
    VIDEO,
    AUDIO
}

data class FormatOption(
    val id: String,
    val label: String,
    val formatType: FormatType,
    val resolution: String? = null,
    val bitrateKbps: Int = 0,
    val container: String = "mp4",
    val estimatedBytes: Long = -1L,
    val isDirectStream: Boolean = true,
    val streamUrl: String? = null
)

data class MediaMetadata(
    val url: String,
    val title: String,
    val thumbnailUrl: String? = null,
    val durationSeconds: Long = 0L,
    val sourceName: String = "Web",
    val formats: List<FormatOption> = emptyList()
)
