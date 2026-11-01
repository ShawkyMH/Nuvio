package com.nuvio.core.update

data class UpdateInfo(
    val tagName: String,
    val versionName: String,
    val versionCode: Long,
    val releaseTitle: String,
    val releaseNotes: String,
    val publishedAt: String,
    val isPrerelease: Boolean,
    val apkDownloadUrl: String,
    val apkFileName: String,
    val apkSizeBytes: Long,
    val checksumDownloadUrl: String? = null,
    val expectedSha256: String? = null
)
