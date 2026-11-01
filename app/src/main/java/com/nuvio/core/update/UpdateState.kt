package com.nuvio.core.update

import java.io.File

enum class UpdateStatus {
    IDLE,
    CHECKING,
    UP_TO_DATE,
    UPDATE_AVAILABLE,
    DOWNLOADING,
    VERIFYING,
    READY_TO_INSTALL,
    INSTALLING,
    FAILED
}

data class UpdateState(
    val status: UpdateStatus = UpdateStatus.IDLE,
    val updateInfo: UpdateInfo? = null,
    val currentVersionName: String = "1.2.1",
    val currentVersionCode: Long = 5L,
    val downloadProgressPercent: Int = 0,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val downloadSpeed: Long = 0L,
    val downloadedApkFile: File? = null,
    val errorMessage: String? = null,
    val lastCheckedTimestamp: Long = 0L,
    val showUpdateDialog: Boolean = false
)
