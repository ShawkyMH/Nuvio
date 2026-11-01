package com.nuvio.engine.browser

data class MediaCandidate(
    val url: String,
    val mimeType: String?,
    val pageTitle: String?,
    val detectedAt: Long = System.currentTimeMillis()
)

object MediaSniffer {

    private val MEDIA_EXTENSIONS = listOf(
        ".mp4", ".mkv", ".mp3", ".m4a", ".webm", ".flv", ".avi",
        ".m3u8", ".mpd"
    )

    fun isMediaStream(url: String, mimeType: String?): Boolean {
        if (mimeType != null) {
            val lowerMime = mimeType.lowercase()
            if (lowerMime.startsWith("video/") || lowerMime.startsWith("audio/")) return true
            if (lowerMime.contains("mpegurl") || lowerMime.contains("dash+xml")) return true
        }

        val cleanUrl = url.substringBefore('?').substringBefore('#').lowercase()
        return MEDIA_EXTENSIONS.any { cleanUrl.endsWith(it) }
    }
}
