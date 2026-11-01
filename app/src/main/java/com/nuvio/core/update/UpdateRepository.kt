package com.nuvio.core.update

import kotlinx.coroutines.flow.StateFlow
import java.io.File

interface UpdateRepository {
    val updateState: StateFlow<UpdateState>

    suspend fun checkForUpdates(manual: Boolean = false, includePrerelease: Boolean = false)
    suspend fun downloadUpdate(updateInfo: UpdateInfo)
    fun installDownloadedUpdate(): InstallationResult
    fun dismissUpdateDialog()
    fun cancelDownload()
}
