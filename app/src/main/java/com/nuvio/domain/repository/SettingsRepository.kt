package com.nuvio.domain.repository

import com.nuvio.domain.model.AppSettings
import com.nuvio.domain.model.StorageMetrics
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface SettingsRepository {
    val settingsFlow: StateFlow<AppSettings>
    fun getSettings(): Flow<AppSettings>
    suspend fun updateSettings(settings: AppSettings)
    suspend fun updateConcurrentDownloads(count: Int)
    suspend fun updateConnectionsPerDownload(count: Int)
    suspend fun updateWifiOnly(wifiOnly: Boolean)
    suspend fun updateDarkMode(darkMode: Boolean?)
    suspend fun updateDynamicColor(enabled: Boolean)

    // Storage and Cleaning
    suspend fun getStorageMetrics(): StorageMetrics
    suspend fun clearStagingFiles(): Long
    suspend fun clearCache(): Long
    suspend fun clearPlaybackHistory()
}
