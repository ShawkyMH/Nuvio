package com.nuvio.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.nuvio.domain.model.AppSettings
import com.nuvio.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsRepositoryImpl(context: Context) : SettingsRepository {

    private val prefs: SharedPreferences = context.getSharedPreferences("nuvio_settings", Context.MODE_PRIVATE)
    private val _settingsFlow = MutableStateFlow(readSettings())

    override fun getSettings(): Flow<AppSettings> = _settingsFlow.asStateFlow()

    override suspend fun updateSettings(settings: AppSettings) {
        prefs.edit().apply {
            putInt(KEY_CONCURRENT_DOWNLOADS, settings.concurrentDownloads)
            putInt(KEY_CONNECTIONS_PER_DOWNLOAD, settings.connectionsPerDownload)
            putString(KEY_DOWNLOAD_LOCATION, settings.downloadLocation)
            putBoolean(KEY_WIFI_ONLY, settings.wifiOnly)
            putInt(KEY_SPEED_LIMIT_KBPS, settings.speedLimitKbps)
            if (settings.darkMode != null) {
                putBoolean(KEY_DARK_MODE, settings.darkMode)
            } else {
                remove(KEY_DARK_MODE)
            }
            putBoolean(KEY_DYNAMIC_COLOR, settings.dynamicColor)
            putBoolean(KEY_AUTO_RESUME_WIFI, settings.autoResumeOnWifi)
            apply()
        }
        _settingsFlow.value = settings
    }

    override suspend fun updateConcurrentDownloads(count: Int) {
        val updated = _settingsFlow.value.copy(concurrentDownloads = count)
        updateSettings(updated)
    }

    override suspend fun updateConnectionsPerDownload(count: Int) {
        val updated = _settingsFlow.value.copy(connectionsPerDownload = count)
        updateSettings(updated)
    }

    override suspend fun updateWifiOnly(wifiOnly: Boolean) {
        val updated = _settingsFlow.value.copy(wifiOnly = wifiOnly)
        updateSettings(updated)
    }

    override suspend fun updateDarkMode(darkMode: Boolean?) {
        val updated = _settingsFlow.value.copy(darkMode = darkMode)
        updateSettings(updated)
    }

    override suspend fun updateDynamicColor(enabled: Boolean) {
        val updated = _settingsFlow.value.copy(dynamicColor = enabled)
        updateSettings(updated)
    }

    private fun readSettings(): AppSettings {
        val darkModeVal = if (prefs.contains(KEY_DARK_MODE)) prefs.getBoolean(KEY_DARK_MODE, false) else null
        return AppSettings(
            concurrentDownloads = prefs.getInt(KEY_CONCURRENT_DOWNLOADS, 3),
            connectionsPerDownload = prefs.getInt(KEY_CONNECTIONS_PER_DOWNLOAD, 4),
            downloadLocation = prefs.getString(KEY_DOWNLOAD_LOCATION, "") ?: "",
            wifiOnly = prefs.getBoolean(KEY_WIFI_ONLY, false),
            speedLimitKbps = prefs.getInt(KEY_SPEED_LIMIT_KBPS, 0),
            darkMode = darkModeVal,
            dynamicColor = prefs.getBoolean(KEY_DYNAMIC_COLOR, true),
            autoResumeOnWifi = prefs.getBoolean(KEY_AUTO_RESUME_WIFI, true)
        )
    }

    companion object {
        private const val KEY_CONCURRENT_DOWNLOADS = "concurrent_downloads"
        private const val KEY_CONNECTIONS_PER_DOWNLOAD = "connections_per_download"
        private const val KEY_DOWNLOAD_LOCATION = "download_location"
        private const val KEY_WIFI_ONLY = "wifi_only"
        private const val KEY_SPEED_LIMIT_KBPS = "speed_limit_kbps"
        private const val KEY_DARK_MODE = "dark_mode"
        private const val KEY_DYNAMIC_COLOR = "dynamic_color"
        private const val KEY_AUTO_RESUME_WIFI = "auto_resume_wifi"
    }
}
