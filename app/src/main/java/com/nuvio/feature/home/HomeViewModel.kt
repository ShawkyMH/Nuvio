package com.nuvio.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nuvio.core.network.UrlValidator
import com.nuvio.core.storage.FileManager
import com.nuvio.domain.model.DownloadTask
import com.nuvio.domain.model.FormatOption
import com.nuvio.domain.model.MediaMetadata
import com.nuvio.domain.model.TaskStatus
import com.nuvio.domain.repository.DownloadRepository
import com.nuvio.domain.repository.MediaRepository
import com.nuvio.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

sealed interface HomeUiEvent {
    data class ShowToast(val message: String) : HomeUiEvent
    data class NavigateToBrowser(val url: String) : HomeUiEvent
    data class PlayMedia(val uri: String, val title: String) : HomeUiEvent
}

data class HomeUiState(
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val selectedMedia: MediaMetadata? = null,
    val showFormatSheet: Boolean = false
)

class HomeViewModel(
    private val downloadRepository: DownloadRepository,
    private val mediaRepository: MediaRepository,
    private val fileManager: FileManager,
    private val settingsRepository: SettingsRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<HomeUiEvent>()
    val events: SharedFlow<HomeUiEvent> = _events.asSharedFlow()

    val activeDownloads: StateFlow<List<DownloadTask>> = downloadRepository.getActiveTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentDownloads: StateFlow<List<DownloadTask>> = downloadRepository.getCompletedTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun submitQuery(rawInput: String) {
        val query = rawInput.trim()
        if (query.isBlank()) return

        if (!UrlValidator.isValidUrl(query)) {
            // Not a direct URL, route as web search in browser
            viewModelScope.launch {
                val searchUrl = "https://www.google.com/search?q=${java.net.URLEncoder.encode(query, "UTF-8")}"
                _events.emit(HomeUiEvent.NavigateToBrowser(searchUrl))
            }
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = mediaRepository.inspectMedia(query)
            _uiState.value = _uiState.value.copy(isLoading = false)

            result.onSuccess { metadata ->
                _uiState.value = _uiState.value.copy(
                    selectedMedia = metadata,
                    showFormatSheet = true
                )
            }.onFailure {
                // If media extraction fails or URL is a direct file, start direct HTTP download
                startDirectDownload(query)
            }
        }
    }

    fun startDirectDownload(url: String) {
        viewModelScope.launch {
            try {
                val fileName = UrlValidator.extractFileNameFromUrl(url, fallback = "download_${System.currentTimeMillis()}")
                val targetFile = fileManager.createTargetFile(fileName)
                val task = DownloadTask(
                    id = UUID.randomUUID().toString(),
                    url = url,
                    title = targetFile.name,
                    filePath = targetFile.absolutePath,
                    mimeType = "*/*",
                    isMediaExtraction = false,
                    connectionCount = settingsRepository?.settingsFlow?.value?.connectionsPerDownload ?: 4,
                    status = TaskStatus.QUEUED
                )
                downloadRepository.insertTask(task)
                _events.emit(HomeUiEvent.ShowToast("Download started: ${targetFile.name}"))
            } catch (e: Exception) {
                _events.emit(HomeUiEvent.ShowToast("Failed to start download: ${e.message}"))
            }
        }
    }

    fun pauseTask(taskId: String) = viewModelScope.launch {
        downloadRepository.pauseTask(taskId)
    }

    fun resumeTask(taskId: String) = viewModelScope.launch {
        downloadRepository.resumeTask(taskId)
    }

    fun cancelTask(taskId: String) = viewModelScope.launch {
        downloadRepository.cancelTask(taskId)
    }

    fun dismissFormatSheet() {
        _uiState.value = _uiState.value.copy(showFormatSheet = false, selectedMedia = null)
    }

    fun startDownloadWithFormat(format: FormatOption) {
        val media = _uiState.value.selectedMedia ?: return
        dismissFormatSheet()

        viewModelScope.launch {
            val cleanTitle = media.title.replace(Regex("""[\\/:*?"<>|]"""), "_").trim()
            val fileName = "$cleanTitle.${format.container}"
            val targetFile = fileManager.createTargetFile(fileName)
            val downloadUrl = format.streamUrl?.takeIf { it.isNotBlank() } ?: media.url
            val task = DownloadTask(
                id = UUID.randomUUID().toString(),
                url = downloadUrl,
                title = cleanTitle,
                filePath = targetFile.absolutePath,
                mimeType = if (format.formatType == com.nuvio.domain.model.FormatType.AUDIO) "audio/*" else "video/*",
                isMediaExtraction = false,
                formatId = format.id,
                totalBytes = if (format.estimatedBytes > 0) format.estimatedBytes else 0L,
                connectionCount = settingsRepository?.settingsFlow?.value?.connectionsPerDownload ?: 4,
                status = TaskStatus.QUEUED
            )
            downloadRepository.insertTask(task)
            _events.emit(HomeUiEvent.ShowToast("Download started: $cleanTitle"))
        }
    }
}
