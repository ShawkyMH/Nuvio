package com.nuvio.domain.repository

import com.nuvio.domain.model.AppSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun getSettings(): Flow<AppSettings>
    suspend fun updateSettings(settings: AppSettings)
    suspend fun updateConcurrentDownloads(count: Int)
    suspend fun updateConnectionsPerDownload(count: Int)
    suspend fun updateWifiOnly(wifiOnly: Boolean)
    suspend fun updateDarkMode(darkMode: Boolean?)
    suspend fun updateDynamicColor(enabled: Boolean)
}
