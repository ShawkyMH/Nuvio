package com.nuvio.feature.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nuvio.core.network.UrlValidator
import com.nuvio.core.storage.FileManager
import com.nuvio.domain.model.DownloadTask
import com.nuvio.domain.model.TaskStatus
import com.nuvio.domain.repository.DownloadRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

enum class DownloadFilter {
    ALL,
    ACTIVE,
    COMPLETED
}

class DownloadsViewModel(
    private val repository: DownloadRepository,
    private val fileManager: FileManager
) : ViewModel() {

    private val _selectedFilter = MutableStateFlow(DownloadFilter.ALL)
    val selectedFilter: StateFlow<DownloadFilter> = _selectedFilter.asStateFlow()

    private val allTasksFlow = repository.getAllTasks()

    val filteredTasks: StateFlow<List<DownloadTask>> = combine(allTasksFlow, _selectedFilter) { tasks, filter ->
        when (filter) {
            DownloadFilter.ALL -> tasks
            DownloadFilter.ACTIVE -> tasks.filter { it.status.isActive || it.status.isPaused }
            DownloadFilter.COMPLETED -> tasks.filter { it.status == com.nuvio.domain.model.TaskStatus.COMPLETED }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalActiveSpeed: StateFlow<Long> = allTasksFlow.combine(_selectedFilter) { tasks, _ ->
        tasks.filter { it.status == com.nuvio.domain.model.TaskStatus.DOWNLOADING }.sumOf { it.downloadSpeed }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    fun setFilter(filter: DownloadFilter) {
        _selectedFilter.value = filter
    }

    fun pauseTask(taskId: String) = viewModelScope.launch {
        repository.pauseTask(taskId)
    }

    fun resumeTask(taskId: String) = viewModelScope.launch {
        repository.resumeTask(taskId)
    }

    fun cancelTask(taskId: String) = viewModelScope.launch {
        repository.cancelTask(taskId)
    }

    fun deleteTask(taskId: String, deleteFile: Boolean) = viewModelScope.launch {
        repository.deleteTask(taskId, deleteFile)
    }

    fun pauseAll() = viewModelScope.launch {
        repository.pauseAll()
    }

    fun resumeAll() = viewModelScope.launch {
        repository.resumeAll()
    }

    fun startDirectDownload(url: String) {
        val trimmed = url.trim()
        if (!UrlValidator.isValidUrl(trimmed)) return
        viewModelScope.launch {
            try {
                val fileName = UrlValidator.extractFileNameFromUrl(trimmed, fallback = "download_${System.currentTimeMillis()}")
                val targetFile = fileManager.createTargetFile(fileName)
                val task = DownloadTask(
                    id = UUID.randomUUID().toString(),
                    url = trimmed,
                    title = targetFile.name,
                    filePath = targetFile.absolutePath,
                    mimeType = "*/*",
                    isMediaExtraction = false,
                    connectionCount = 4,
                    status = TaskStatus.QUEUED
                )
                repository.insertTask(task)
            } catch (_: Exception) {
            }
        }
    }
}
