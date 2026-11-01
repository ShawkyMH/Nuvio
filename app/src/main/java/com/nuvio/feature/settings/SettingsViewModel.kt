package com.nuvio.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nuvio.domain.model.AppSettings
import com.nuvio.domain.repository.PlaybackRepository
import com.nuvio.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val playbackRepository: PlaybackRepository
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.getSettings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    fun updateConcurrentDownloads(count: Int) = viewModelScope.launch {
        settingsRepository.updateConcurrentDownloads(count)
    }

    fun updateConnectionsPerDownload(count: Int) = viewModelScope.launch {
        settingsRepository.updateConnectionsPerDownload(count)
    }

    fun updateWifiOnly(wifiOnly: Boolean) = viewModelScope.launch {
        settingsRepository.updateWifiOnly(wifiOnly)
    }

    fun updateDarkMode(darkMode: Boolean?) = viewModelScope.launch {
        settingsRepository.updateDarkMode(darkMode)
    }

    fun updateDynamicColor(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.updateDynamicColor(enabled)
    }

    fun clearHistory() = viewModelScope.launch {
        playbackRepository.clearHistory()
    }
}
