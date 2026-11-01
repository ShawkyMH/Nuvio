package com.nuvio.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.nuvio.core.database.PlaybackHistoryDao
import com.nuvio.core.storage.FileManager
import com.nuvio.domain.model.AppSettings
import com.nuvio.domain.model.StorageMetrics
import com.nuvio.domain.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class SettingsRepositoryImpl(
    private val context: Context,
    private val fileManager: FileManager? = null,
    private val playbackHistoryDao: PlaybackHistoryDao? = null
) : SettingsRepository {

    private val prefs: SharedPreferences = context.getSharedPreferences("nuvio_settings", Context.MODE_PRIVATE)
    private val _settingsFlow = MutableStateFlow(readSettings())

    override val settingsFlow: StateFlow<AppSettings> = _settingsFlow.asStateFlow()
    override fun getSettings(): Flow<AppSettings> = settingsFlow

    override suspend fun updateSettings(settings: AppSettings) {
        prefs.edit().apply {
            // Downloads
            putInt(KEY_CONCURRENT_DOWNLOADS, settings.concurrentDownloads)
            putInt(KEY_CONNECTIONS_PER_DOWNLOAD, settings.connectionsPerDownload)
            putString(KEY_DOWNLOAD_LOCATION, settings.downloadLocation)
            putBoolean(KEY_WIFI_ONLY, settings.wifiOnly)
            putBoolean(KEY_AUTO_RESUME_WIFI, settings.autoResumeOnWifi)
            putBoolean(KEY_RETRY_FAILED, settings.retryFailed)
            putInt(KEY_MAX_RETRIES, settings.maxRetries)
            putBoolean(KEY_ASK_BEFORE_DOWNLOAD, settings.askBeforeDownload)
            putBoolean(KEY_KEEP_HISTORY, settings.keepDownloadHistory)
            putBoolean(KEY_PREVENT_DUPLICATES, settings.preventDuplicateFilenames)
            putInt(KEY_SPEED_LIMIT_KBPS, settings.speedLimitKbps)

            // Video & Audio
            putString(KEY_PREFERRED_VIDEO_QUALITY, settings.preferredVideoQuality)
            putString(KEY_DEFAULT_VIDEO_FORMAT, settings.defaultVideoFormat)
            putBoolean(KEY_ASK_QUALITY_BEFORE_DOWNLOAD, settings.askQualityBeforeDownload)
            putBoolean(KEY_PREFER_ORIGINAL_FORMAT, settings.preferOriginalFormat)
            putBoolean(KEY_DOWNLOAD_SUBTITLES, settings.downloadSubtitles)
            putString(KEY_PREFERRED_AUDIO_QUALITY, settings.preferredAudioQuality)
            putString(KEY_DEFAULT_AUDIO_FORMAT, settings.defaultAudioFormat)

            // Notifications
            putBoolean(KEY_NOTIFY_STARTED, settings.notifyStarted)
            putBoolean(KEY_NOTIFY_PROGRESS, settings.notifyProgress)
            putBoolean(KEY_NOTIFY_COMPLETED, settings.notifyCompleted)
            putBoolean(KEY_NOTIFY_FAILED, settings.notifyFailed)
            putBoolean(KEY_NOTIFY_UPDATES, settings.notifyUpdates)

            // Browser
            putString(KEY_BROWSER_HOMEPAGE, settings.browserHomepage)
            putString(KEY_SEARCH_ENGINE, settings.searchEngine)
            putBoolean(KEY_DESKTOP_SITE, settings.desktopSite)
            putBoolean(KEY_ASK_BEFORE_DOWNLOAD_MEDIA, settings.askBeforeDownloadMedia)
            putBoolean(KEY_JAVASCRIPT_ENABLED, settings.javaScriptEnabled)

            // Media Player
            putBoolean(KEY_REMEMBER_POSITION, settings.rememberPlaybackPosition)
            putBoolean(KEY_AUTO_PLAY_NEXT, settings.autoPlayNext)
            putBoolean(KEY_BACKGROUND_AUDIO, settings.backgroundAudio)
            putFloat(KEY_DEFAULT_SPEED, settings.defaultPlaybackSpeed)
            putBoolean(KEY_KEEP_SCREEN_ON, settings.keepScreenOn)

            // Appearance
            if (settings.darkMode != null) {
                putBoolean(KEY_DARK_MODE, settings.darkMode)
            } else {
                remove(KEY_DARK_MODE)
            }
            putBoolean(KEY_DYNAMIC_COLOR, settings.dynamicColor)
            putString(KEY_LANGUAGE, settings.language)

            // Privacy
            putBoolean(KEY_CLEAR_HISTORY_ON_EXIT, settings.clearHistoryOnExit)
            putBoolean(KEY_DO_NOT_TRACK, settings.doNotTrack)

            // Updates
            putBoolean(KEY_AUTO_CHECK_UPDATES, settings.autoCheckUpdates)
            putBoolean(KEY_WIFI_ONLY_UPDATES, settings.wifiOnlyUpdates)

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

    override suspend fun getStorageMetrics(): StorageMetrics = withContext(Dispatchers.IO) {
        val fm = fileManager ?: FileManager(context)
        StorageMetrics(
            downloadsSizeBytes = fm.getDownloadsSizeBytes(),
            stagingSizeBytes = fm.getStagingSizeBytes(),
            cacheSizeBytes = fm.getCacheSizeBytes(),
            availableBytes = fm.getAvailableStorageBytes(),
            totalBytes = fm.getTotalStorageBytes()
        )
    }

    override suspend fun clearStagingFiles(): Long = withContext(Dispatchers.IO) {
        val fm = fileManager ?: FileManager(context)
        fm.clearStagingFiles()
    }

    override suspend fun clearCache(): Long = withContext(Dispatchers.IO) {
        val fm = fileManager ?: FileManager(context)
        fm.clearCache()
    }

    override suspend fun clearPlaybackHistory(): Unit = withContext(Dispatchers.IO) {
        playbackHistoryDao?.clearHistory()
        Unit
    }

    private fun readSettings(): AppSettings {
        val darkModeVal = if (prefs.contains(KEY_DARK_MODE)) prefs.getBoolean(KEY_DARK_MODE, false) else null
        return AppSettings(
            // Downloads
            concurrentDownloads = prefs.getInt(KEY_CONCURRENT_DOWNLOADS, 3),
            connectionsPerDownload = prefs.getInt(KEY_CONNECTIONS_PER_DOWNLOAD, 4),
            downloadLocation = prefs.getString(KEY_DOWNLOAD_LOCATION, "") ?: "",
            wifiOnly = prefs.getBoolean(KEY_WIFI_ONLY, false),
            autoResumeOnWifi = prefs.getBoolean(KEY_AUTO_RESUME_WIFI, true),
            retryFailed = prefs.getBoolean(KEY_RETRY_FAILED, true),
            maxRetries = prefs.getInt(KEY_MAX_RETRIES, 3),
            askBeforeDownload = prefs.getBoolean(KEY_ASK_BEFORE_DOWNLOAD, false),
            keepDownloadHistory = prefs.getBoolean(KEY_KEEP_HISTORY, true),
            preventDuplicateFilenames = prefs.getBoolean(KEY_PREVENT_DUPLICATES, true),
            speedLimitKbps = prefs.getInt(KEY_SPEED_LIMIT_KBPS, 0),

            // Video & Audio
            preferredVideoQuality = prefs.getString(KEY_PREFERRED_VIDEO_QUALITY, "Highest Available") ?: "Highest Available",
            defaultVideoFormat = prefs.getString(KEY_DEFAULT_VIDEO_FORMAT, "mp4") ?: "mp4",
            askQualityBeforeDownload = prefs.getBoolean(KEY_ASK_QUALITY_BEFORE_DOWNLOAD, true),
            preferOriginalFormat = prefs.getBoolean(KEY_PREFER_ORIGINAL_FORMAT, true),
            downloadSubtitles = prefs.getBoolean(KEY_DOWNLOAD_SUBTITLES, false),
            preferredAudioQuality = prefs.getString(KEY_PREFERRED_AUDIO_QUALITY, "320 kbps") ?: "320 kbps",
            defaultAudioFormat = prefs.getString(KEY_DEFAULT_AUDIO_FORMAT, "mp3") ?: "mp3",

            // Notifications
            notifyStarted = prefs.getBoolean(KEY_NOTIFY_STARTED, true),
            notifyProgress = prefs.getBoolean(KEY_NOTIFY_PROGRESS, true),
            notifyCompleted = prefs.getBoolean(KEY_NOTIFY_COMPLETED, true),
            notifyFailed = prefs.getBoolean(KEY_NOTIFY_FAILED, true),
            notifyUpdates = prefs.getBoolean(KEY_NOTIFY_UPDATES, true),

            // Browser
            browserHomepage = prefs.getString(KEY_BROWSER_HOMEPAGE, "https://www.google.com") ?: "https://www.google.com",
            searchEngine = prefs.getString(KEY_SEARCH_ENGINE, "Google") ?: "Google",
            desktopSite = prefs.getBoolean(KEY_DESKTOP_SITE, false),
            askBeforeDownloadMedia = prefs.getBoolean(KEY_ASK_BEFORE_DOWNLOAD_MEDIA, true),
            javaScriptEnabled = prefs.getBoolean(KEY_JAVASCRIPT_ENABLED, true),

            // Media Player
            rememberPlaybackPosition = prefs.getBoolean(KEY_REMEMBER_POSITION, true),
            autoPlayNext = prefs.getBoolean(KEY_AUTO_PLAY_NEXT, false),
            backgroundAudio = prefs.getBoolean(KEY_BACKGROUND_AUDIO, true),
            defaultPlaybackSpeed = prefs.getFloat(KEY_DEFAULT_SPEED, 1.0f),
            keepScreenOn = prefs.getBoolean(KEY_KEEP_SCREEN_ON, true),

            // Appearance
            darkMode = darkModeVal,
            dynamicColor = prefs.getBoolean(KEY_DYNAMIC_COLOR, true),
            language = prefs.getString(KEY_LANGUAGE, "System Default") ?: "System Default",

            // Privacy
            clearHistoryOnExit = prefs.getBoolean(KEY_CLEAR_HISTORY_ON_EXIT, false),
            doNotTrack = prefs.getBoolean(KEY_DO_NOT_TRACK, true),

            // Updates
            autoCheckUpdates = prefs.getBoolean(KEY_AUTO_CHECK_UPDATES, true),
            wifiOnlyUpdates = prefs.getBoolean(KEY_WIFI_ONLY_UPDATES, true)
        )
    }

    companion object {
        // Downloads
        private const val KEY_CONCURRENT_DOWNLOADS = "concurrent_downloads"
        private const val KEY_CONNECTIONS_PER_DOWNLOAD = "connections_per_download"
        private const val KEY_DOWNLOAD_LOCATION = "download_location"
        private const val KEY_WIFI_ONLY = "wifi_only"
        private const val KEY_AUTO_RESUME_WIFI = "auto_resume_wifi"
        private const val KEY_RETRY_FAILED = "retry_failed"
        private const val KEY_MAX_RETRIES = "max_retries"
        private const val KEY_ASK_BEFORE_DOWNLOAD = "ask_before_download"
        private const val KEY_KEEP_HISTORY = "keep_history"
        private const val KEY_PREVENT_DUPLICATES = "prevent_duplicates"
        private const val KEY_SPEED_LIMIT_KBPS = "speed_limit_kbps"

        // Video & Audio
        private const val KEY_PREFERRED_VIDEO_QUALITY = "preferred_video_quality"
        private const val KEY_DEFAULT_VIDEO_FORMAT = "default_video_format"
        private const val KEY_ASK_QUALITY_BEFORE_DOWNLOAD = "ask_quality_before_download"
        private const val KEY_PREFER_ORIGINAL_FORMAT = "prefer_original_format"
        private const val KEY_DOWNLOAD_SUBTITLES = "download_subtitles"
        private const val KEY_PREFERRED_AUDIO_QUALITY = "preferred_audio_quality"
        private const val KEY_DEFAULT_AUDIO_FORMAT = "default_audio_format"

        // Notifications
        private const val KEY_NOTIFY_STARTED = "notify_started"
        private const val KEY_NOTIFY_PROGRESS = "notify_progress"
        private const val KEY_NOTIFY_COMPLETED = "notify_completed"
        private const val KEY_NOTIFY_FAILED = "notify_failed"
        private const val KEY_NOTIFY_UPDATES = "notify_updates"

        // Browser
        private const val KEY_BROWSER_HOMEPAGE = "browser_homepage"
        private const val KEY_SEARCH_ENGINE = "search_engine"
        private const val KEY_DESKTOP_SITE = "desktop_site"
        private const val KEY_ASK_BEFORE_DOWNLOAD_MEDIA = "ask_before_download_media"
        private const val KEY_JAVASCRIPT_ENABLED = "javascript_enabled"

        // Media Player
        private const val KEY_REMEMBER_POSITION = "remember_position"
        private const val KEY_AUTO_PLAY_NEXT = "auto_play_next"
        private const val KEY_BACKGROUND_AUDIO = "background_audio"
        private const val KEY_DEFAULT_SPEED = "default_speed"
        private const val KEY_KEEP_SCREEN_ON = "keep_screen_on"

        // Appearance
        private const val KEY_DARK_MODE = "dark_mode"
        private const val KEY_DYNAMIC_COLOR = "dynamic_color"
        private const val KEY_LANGUAGE = "language"

        // Privacy
        private const val KEY_CLEAR_HISTORY_ON_EXIT = "clear_history_on_exit"
        private const val KEY_DO_NOT_TRACK = "do_not_track"

        // Updates
        private const val KEY_AUTO_CHECK_UPDATES = "auto_check_updates"
        private const val KEY_WIFI_ONLY_UPDATES = "wifi_only_updates"
    }
}
