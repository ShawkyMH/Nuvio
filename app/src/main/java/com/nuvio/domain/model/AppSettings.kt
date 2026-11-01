package com.nuvio.domain.model

data class AppSettings(
    val concurrentDownloads: Int = 3,
    val connectionsPerDownload: Int = 4,
    val downloadLocation: String = "",
    val wifiOnly: Boolean = false,
    val speedLimitKbps: Int = 0, // 0 = unlimited
    val darkMode: Boolean? = null, // null = system default
    val dynamicColor: Boolean = true,
    val autoResumeOnWifi: Boolean = true,
    val userAgent: String = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
)
