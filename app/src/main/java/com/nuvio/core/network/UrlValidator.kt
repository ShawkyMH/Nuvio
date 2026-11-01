package com.nuvio.core.network

import java.net.URI

object UrlValidator {

    private val BLOCKED_HOSTS = setOf(
        "localhost",
        "127.0.0.1",
        "0.0.0.0",
        "::1"
    )

    fun isValidUrl(url: String): Boolean {
        if (url.isBlank()) return false
        return runCatching {
            val uri = URI(url.trim())
            val scheme = uri.scheme?.lowercase()
            val host = uri.host?.lowercase()

            if (scheme != "http" && scheme != "https") return@runCatching false
            if (host.isNullOrBlank()) return@runCatching false

            // Block SSRF to loopback and private link-local ranges
            if (BLOCKED_HOSTS.contains(host)) return@runCatching false
            if (host.startsWith("169.254.") || host.startsWith("10.") || host.startsWith("192.168.")) {
                return@runCatching false
            }

            true
        }.getOrDefault(false)
    }

    fun isMediaSource(url: String): Boolean {
        if (!isValidUrl(url)) return false
        val lower = url.lowercase()
        return lower.contains("youtube.com") ||
                lower.contains("youtu.be") ||
                lower.contains("vimeo.com") ||
                lower.contains("tiktok.com") ||
                lower.contains("instagram.com") ||
                lower.contains("twitter.com") ||
                lower.contains("x.com") ||
                lower.endsWith(".mp4") ||
                lower.endsWith(".mkv") ||
                lower.endsWith(".mp3") ||
                lower.endsWith(".m4a") ||
                lower.endsWith(".webm") ||
                lower.endsWith(".m3u8") ||
                lower.endsWith(".mpd")
    }

    fun extractFileNameFromUrl(url: String, fallback: String = "download"): String {
        return runCatching {
            val uri = URI(url.trim())
            val path = uri.path ?: ""
            val lastSegment = path.substringAfterLast('/')
            if (lastSegment.isNotBlank() && lastSegment.contains('.')) {
                lastSegment.substringBefore('?').substringBefore('#')
            } else {
                fallback
            }
        }.getOrDefault(fallback)
    }
}
