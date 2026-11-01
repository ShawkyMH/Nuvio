package com.nuvio.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nuvio.domain.model.AppSettings
import com.nuvio.domain.model.StorageMetrics
import com.nuvio.domain.repository.PlaybackRepository
import com.nuvio.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class SettingsSubScreen {
    DOWNLOADS,
    MEDIA,
    STORAGE,
    NOTIFICATIONS,
    BROWSER,
    PLAYER,
    APPEARANCE,
    PRIVACY,
    UPDATES,
    ABOUT
}

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val playbackRepository: PlaybackRepository
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.getSettings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    private val _currentSubScreen = MutableStateFlow<SettingsSubScreen?>(null)
    val currentSubScreen: StateFlow<SettingsSubScreen?> = _currentSubScreen.asStateFlow()

    private val _storageMetrics = MutableStateFlow(StorageMetrics())
    val storageMetrics: StateFlow<StorageMetrics> = _storageMetrics.asStateFlow()

    init {
        loadStorageMetrics()
    }

    fun navigateToSubScreen(subScreen: SettingsSubScreen?) {
        _currentSubScreen.value = subScreen
        if (subScreen == SettingsSubScreen.STORAGE) {
            loadStorageMetrics()
        }
    }

    fun loadStorageMetrics() = viewModelScope.launch {
        _storageMetrics.value = settingsRepository.getStorageMetrics()
    }

    fun updateSettings(newSettings: AppSettings) = viewModelScope.launch {
        settingsRepository.updateSettings(newSettings)
    }

    // Downloads
    fun updateConcurrentDownloads(count: Int) = viewModelScope.launch {
        val updated = settings.value.copy(concurrentDownloads = count.coerceIn(1, 8))
        settingsRepository.updateSettings(updated)
    }

    fun updateConnectionsPerDownload(count: Int) = viewModelScope.launch {
        val updated = settings.value.copy(connectionsPerDownload = count.coerceIn(1, 8))
        settingsRepository.updateSettings(updated)
    }

    fun updateWifiOnly(wifiOnly: Boolean) = viewModelScope.launch {
        val updated = settings.value.copy(wifiOnly = wifiOnly)
        settingsRepository.updateSettings(updated)
    }

    fun updateAutoResume(enabled: Boolean) = viewModelScope.launch {
        val updated = settings.value.copy(autoResumeOnWifi = enabled)
        settingsRepository.updateSettings(updated)
    }

    fun updateRetryFailed(enabled: Boolean) = viewModelScope.launch {
        val updated = settings.value.copy(retryFailed = enabled)
        settingsRepository.updateSettings(updated)
    }

    fun updateMaxRetries(retries: Int) = viewModelScope.launch {
        val updated = settings.value.copy(maxRetries = retries.coerceIn(1, 10))
        settingsRepository.updateSettings(updated)
    }

    fun updateAskBeforeDownload(enabled: Boolean) = viewModelScope.launch {
        val updated = settings.value.copy(askBeforeDownload = enabled)
        settingsRepository.updateSettings(updated)
    }

    fun updateKeepHistory(enabled: Boolean) = viewModelScope.launch {
        val updated = settings.value.copy(keepDownloadHistory = enabled)
        settingsRepository.updateSettings(updated)
    }

    fun updatePreventDuplicates(enabled: Boolean) = viewModelScope.launch {
        val updated = settings.value.copy(preventDuplicateFilenames = enabled)
        settingsRepository.updateSettings(updated)
    }

    // Video & Audio
    fun updatePreferredVideoQuality(quality: String) = viewModelScope.launch {
        val updated = settings.value.copy(preferredVideoQuality = quality)
        settingsRepository.updateSettings(updated)
    }

    fun updateDefaultVideoFormat(format: String) = viewModelScope.launch {
        val updated = settings.value.copy(defaultVideoFormat = format)
        settingsRepository.updateSettings(updated)
    }

    fun updateAskQualityBeforeDownload(enabled: Boolean) = viewModelScope.launch {
        val updated = settings.value.copy(askQualityBeforeDownload = enabled)
        settingsRepository.updateSettings(updated)
    }

    fun updatePreferOriginalFormat(enabled: Boolean) = viewModelScope.launch {
        val updated = settings.value.copy(preferOriginalFormat = enabled)
        settingsRepository.updateSettings(updated)
    }

    fun updateDownloadSubtitles(enabled: Boolean) = viewModelScope.launch {
        val updated = settings.value.copy(downloadSubtitles = enabled)
        settingsRepository.updateSettings(updated)
    }

    fun updatePreferredAudioQuality(quality: String) = viewModelScope.launch {
        val updated = settings.value.copy(preferredAudioQuality = quality)
        settingsRepository.updateSettings(updated)
    }

    fun updateDefaultAudioFormat(format: String) = viewModelScope.launch {
        val updated = settings.value.copy(defaultAudioFormat = format)
        settingsRepository.updateSettings(updated)
    }

    // Notifications
    fun updateNotificationStarted(enabled: Boolean) = viewModelScope.launch {
        val updated = settings.value.copy(notifyStarted = enabled)
        settingsRepository.updateSettings(updated)
    }

    fun updateNotificationProgress(enabled: Boolean) = viewModelScope.launch {
        val updated = settings.value.copy(notifyProgress = enabled)
        settingsRepository.updateSettings(updated)
    }

    fun updateNotificationCompleted(enabled: Boolean) = viewModelScope.launch {
        val updated = settings.value.copy(notifyCompleted = enabled)
        settingsRepository.updateSettings(updated)
    }

    fun updateNotificationFailed(enabled: Boolean) = viewModelScope.launch {
        val updated = settings.value.copy(notifyFailed = enabled)
        settingsRepository.updateSettings(updated)
    }

    fun updateNotificationUpdates(enabled: Boolean) = viewModelScope.launch {
        val updated = settings.value.copy(notifyUpdates = enabled)
        settingsRepository.updateSettings(updated)
    }

    // Browser
    fun updateBrowserHomepage(homepage: String) = viewModelScope.launch {
        val updated = settings.value.copy(browserHomepage = homepage)
        settingsRepository.updateSettings(updated)
    }

    fun updateSearchEngine(engine: String) = viewModelScope.launch {
        val updated = settings.value.copy(searchEngine = engine)
        settingsRepository.updateSettings(updated)
    }

    fun updateDesktopSite(enabled: Boolean) = viewModelScope.launch {
        val updated = settings.value.copy(desktopSite = enabled)
        settingsRepository.updateSettings(updated)
    }

    fun updateAskBeforeDownloadMedia(enabled: Boolean) = viewModelScope.launch {
        val updated = settings.value.copy(askBeforeDownloadMedia = enabled)
        settingsRepository.updateSettings(updated)
    }

    // Media Player
    fun updateRememberPosition(enabled: Boolean) = viewModelScope.launch {
        val updated = settings.value.copy(rememberPlaybackPosition = enabled)
        settingsRepository.updateSettings(updated)
    }

    fun updateAutoPlayNext(enabled: Boolean) = viewModelScope.launch {
        val updated = settings.value.copy(autoPlayNext = enabled)
        settingsRepository.updateSettings(updated)
    }

    fun updateBackgroundAudio(enabled: Boolean) = viewModelScope.launch {
        val updated = settings.value.copy(backgroundAudio = enabled)
        settingsRepository.updateSettings(updated)
    }

    fun updatePlaybackSpeed(speed: Float) = viewModelScope.launch {
        val updated = settings.value.copy(defaultPlaybackSpeed = speed)
        settingsRepository.updateSettings(updated)
    }

    fun updateKeepScreenOn(enabled: Boolean) = viewModelScope.launch {
        val updated = settings.value.copy(keepScreenOn = enabled)
        settingsRepository.updateSettings(updated)
    }

    // Appearance
    fun updateDarkMode(darkMode: Boolean?) = viewModelScope.launch {
        val updated = settings.value.copy(darkMode = darkMode)
        settingsRepository.updateSettings(updated)
    }

    fun updateDynamicColor(enabled: Boolean) = viewModelScope.launch {
        val updated = settings.value.copy(dynamicColor = enabled)
        settingsRepository.updateSettings(updated)
    }

    fun updateLanguage(lang: String) = viewModelScope.launch {
        val updated = settings.value.copy(language = lang)
        settingsRepository.updateSettings(updated)
    }

    // Privacy
    fun updateClearHistoryOnExit(enabled: Boolean) = viewModelScope.launch {
        val updated = settings.value.copy(clearHistoryOnExit = enabled)
        settingsRepository.updateSettings(updated)
    }

    fun updateDoNotTrack(enabled: Boolean) = viewModelScope.launch {
        val updated = settings.value.copy(doNotTrack = enabled)
        settingsRepository.updateSettings(updated)
    }

    // Updates
    fun updateAutoCheckUpdates(enabled: Boolean) = viewModelScope.launch {
        val updated = settings.value.copy(autoCheckUpdates = enabled)
        settingsRepository.updateSettings(updated)
    }

    fun updateWifiOnlyUpdates(enabled: Boolean) = viewModelScope.launch {
        val updated = settings.value.copy(wifiOnlyUpdates = enabled)
        settingsRepository.updateSettings(updated)
    }

    // Actions
    fun clearStagingFiles() = viewModelScope.launch {
        settingsRepository.clearStagingFiles()
        loadStorageMetrics()
    }

    fun clearCache() = viewModelScope.launch {
        settingsRepository.clearCache()
        loadStorageMetrics()
    }

    fun clearHistory() = viewModelScope.launch {
        playbackRepository.clearHistory()
        settingsRepository.clearPlaybackHistory()
    }
}
