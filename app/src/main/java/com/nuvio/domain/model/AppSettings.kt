package com.nuvio.domain.model

data class StorageMetrics(
    val downloadsSizeBytes: Long = 0L,
    val stagingSizeBytes: Long = 0L,
    val cacheSizeBytes: Long = 0L,
    val availableBytes: Long = 0L,
    val totalBytes: Long = 0L
)

data class AppSettings(
    // 1. Downloads
    val concurrentDownloads: Int = 3,
    val connectionsPerDownload: Int = 4,
    val downloadLocation: String = "",
    val wifiOnly: Boolean = false,
    val autoResumeOnWifi: Boolean = true,
    val retryFailed: Boolean = true,
    val maxRetries: Int = 3,
    val askBeforeDownload: Boolean = false,
    val keepDownloadHistory: Boolean = true,
    val preventDuplicateFilenames: Boolean = true,
    val speedLimitKbps: Int = 0, // 0 = unlimited

    // 2. Video & Audio
    val preferredVideoQuality: String = "Highest Available", // Highest Available, 1080p, 720p, 480p, 360p
    val defaultVideoFormat: String = "mp4", // mp4, mkv, webm
    val askQualityBeforeDownload: Boolean = true,
    val preferOriginalFormat: Boolean = true,
    val downloadSubtitles: Boolean = false,
    val preferredAudioQuality: String = "320 kbps", // 320 kbps, 256 kbps, 128 kbps
    val defaultAudioFormat: String = "mp3", // mp3, m4a

    // 3. Notifications
    val notifyStarted: Boolean = true,
    val notifyProgress: Boolean = true,
    val notifyCompleted: Boolean = true,
    val notifyFailed: Boolean = true,
    val notifyUpdates: Boolean = true,

    // 4. Browser
    val browserHomepage: String = "https://www.google.com",
    val searchEngine: String = "Google", // Google, Bing, DuckDuckGo
    val desktopSite: Boolean = false,
    val askBeforeDownloadMedia: Boolean = true,
    val javaScriptEnabled: Boolean = true,

    // 5. Media Player
    val rememberPlaybackPosition: Boolean = true,
    val autoPlayNext: Boolean = false,
    val backgroundAudio: Boolean = true,
    val defaultPlaybackSpeed: Float = 1.0f,
    val keepScreenOn: Boolean = true,

    // 6. Appearance
    val darkMode: Boolean? = null, // null = system default, true = dark, false = light
    val dynamicColor: Boolean = true,
    val language: String = "System Default",

    // 7. Privacy
    val clearHistoryOnExit: Boolean = false,
    val doNotTrack: Boolean = true,

    // 8. Updates
    val autoCheckUpdates: Boolean = true,
    val wifiOnlyUpdates: Boolean = true,

    // 9. Networking
    val userAgent: String = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
)
