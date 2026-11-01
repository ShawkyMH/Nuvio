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
    private val fileManager: FileManager
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
            }.onFailure { error ->
                _events.emit(HomeUiEvent.ShowToast("Inspection failed: ${error.localizedMessage ?: "Unknown error"}"))
            }
        }
    }

    fun dismissFormatSheet() {
        _uiState.value = _uiState.value.copy(showFormatSheet = false, selectedMedia = null)
    }

    fun startDownloadWithFormat(format: FormatOption) {
        val media = _uiState.value.selectedMedia ?: return
        dismissFormatSheet()

        viewModelScope.launch {
            val fileName = "${media.title}.${format.container}"
            val targetFile = fileManager.createTargetFile(fileName)
            val task = DownloadTask(
                id = UUID.randomUUID().toString(),
                url = media.url,
                title = media.title,
                filePath = targetFile.absolutePath,
                mimeType = if (format.formatType == com.nuvio.domain.model.FormatType.AUDIO) "audio/*" else "video/*",
                isMediaExtraction = true,
                formatId = format.id,
                connectionCount = 4,
                status = TaskStatus.QUEUED
            )
            downloadRepository.insertTask(task)
            _events.emit(HomeUiEvent.ShowToast("Download started: ${media.title}"))
        }
    }
}
