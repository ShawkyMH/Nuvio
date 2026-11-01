package com.nuvio.feature.browser

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nuvio.core.network.UrlValidator
import com.nuvio.core.storage.FileManager
import com.nuvio.domain.model.DownloadTask
import com.nuvio.domain.model.TaskStatus
import com.nuvio.domain.repository.DownloadRepository
import com.nuvio.domain.repository.SettingsRepository
import com.nuvio.engine.browser.MediaCandidate
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.util.UUID

sealed interface BrowserEvent {
    data class ShowToast(val message: String) : BrowserEvent
}

data class BrowserUiState(
    val currentUrl: String = "https://www.google.com",
    val pageTitle: String = "Google",
    val isLoading: Boolean = false,
    val progress: Int = 0,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val isPrivateMode: Boolean = false,
    val detectedMedia: MediaCandidate? = null
)

class BrowserViewModel(
    private val downloadRepository: DownloadRepository,
    private val fileManager: FileManager,
    private val settingsRepository: SettingsRepository? = null
) : ViewModel() {

    private val defaultHomepage = settingsRepository?.settingsFlow?.value?.browserHomepage?.takeIf { it.isNotBlank() } ?: "https://www.google.com"

    val settingsFlow: StateFlow<com.nuvio.domain.model.AppSettings>? = settingsRepository?.settingsFlow

    private val _uiState = MutableStateFlow(
        BrowserUiState(
            currentUrl = defaultHomepage,
            pageTitle = "Home"
        )
    )
    val uiState: StateFlow<BrowserUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<BrowserEvent>()
    val events: SharedFlow<BrowserEvent> = _events.asSharedFlow()

    fun loadUrl(url: String) {
        val target = if (UrlValidator.isValidUrl(url)) {
            url.trim()
        } else {
            val query = URLEncoder.encode(url.trim(), "UTF-8")
            val engine = settingsRepository?.settingsFlow?.value?.searchEngine ?: "Google"
            when (engine) {
                "DuckDuckGo" -> "https://duckduckgo.com/?q=$query"
                "Bing" -> "https://www.bing.com/search?q=$query"
                "Yahoo" -> "https://search.yahoo.com/search?p=$query"
                "Ecosia" -> "https://www.ecosia.org/search?q=$query"
                else -> "https://www.google.com/search?q=$query"
            }
        }
        _uiState.value = _uiState.value.copy(currentUrl = target)
    }

    fun onPageStarted(url: String) {
        _uiState.value = _uiState.value.copy(currentUrl = url, isLoading = true, detectedMedia = null)
    }

    fun onPageFinished(url: String, title: String?, canBack: Boolean, canForward: Boolean) {
        _uiState.value = _uiState.value.copy(
            currentUrl = url,
            pageTitle = title ?: url,
            isLoading = false,
            canGoBack = canBack,
            canGoForward = canForward
        )
    }

    fun onProgressChanged(progress: Int) {
        _uiState.value = _uiState.value.copy(progress = progress, isLoading = progress < 100)
    }

    fun onMediaDetected(candidate: MediaCandidate) {
        if (_uiState.value.detectedMedia?.url != candidate.url) {
            _uiState.value = _uiState.value.copy(detectedMedia = candidate)
        }
    }

    fun dismissDetectedMedia() {
        _uiState.value = _uiState.value.copy(detectedMedia = null)
    }

    fun downloadDetectedMedia() {
        val candidate = _uiState.value.detectedMedia ?: return
        dismissDetectedMedia()

        viewModelScope.launch {
            val fileName = UrlValidator.extractFileNameFromUrl(candidate.url, fallback = "media_${System.currentTimeMillis()}.mp4")
            val targetFile = fileManager.createTargetFile(fileName)
            val task = DownloadTask(
                id = UUID.randomUUID().toString(),
                url = candidate.url,
                title = candidate.pageTitle ?: fileName,
                filePath = targetFile.absolutePath,
                mimeType = candidate.mimeType ?: "video/*",
                isMediaExtraction = false,
                connectionCount = settingsRepository?.settingsFlow?.value?.connectionsPerDownload ?: 4,
                status = TaskStatus.QUEUED
            )
            downloadRepository.insertTask(task)
            _events.emit(BrowserEvent.ShowToast("Download added to queue"))
        }
    }

    fun togglePrivateMode() {
        _uiState.value = _uiState.value.copy(isPrivateMode = !_uiState.value.isPrivateMode)
    }
}
