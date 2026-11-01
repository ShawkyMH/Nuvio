package com.nuvio.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nuvio.core.storage.FileManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

enum class LibraryCategory {
    ALL,
    VIDEOS,
    MUSIC,
    DOCUMENTS,
    ARCHIVES,
    OTHER
}

data class LocalMediaFile(
    val file: File,
    val name: String,
    val sizeBytes: Long,
    val lastModified: Long,
    val category: LibraryCategory
)

data class StorageStats(
    val usedBytes: Long = 0L,
    val freeBytes: Long = 0L,
    val totalBytes: Long = 0L
)

class LibraryViewModel(
    private val fileManager: FileManager
) : ViewModel() {

    private val _files = MutableStateFlow<List<LocalMediaFile>>(emptyList())
    val files: StateFlow<List<LocalMediaFile>> = _files.asStateFlow()

    private val _selectedCategory = MutableStateFlow(LibraryCategory.ALL)
    val selectedCategory: StateFlow<LibraryCategory> = _selectedCategory.asStateFlow()

    private val _storageStats = MutableStateFlow(StorageStats())
    val storageStats: StateFlow<StorageStats> = _storageStats.asStateFlow()

    init {
        loadFiles()
    }

    fun setCategory(category: LibraryCategory) {
        _selectedCategory.value = category
    }

    fun loadFiles() = viewModelScope.launch(Dispatchers.IO) {
        val dir = fileManager.downloadsDir
        val fileList = dir.listFiles()?.filter { it.isFile }?.map { file ->
            LocalMediaFile(
                file = file,
                name = file.name,
                sizeBytes = file.length(),
                lastModified = file.lastModified(),
                category = resolveCategory(file.name)
            )
        }?.sortedByDescending { it.lastModified } ?: emptyList()

        _files.value = fileList

        val total = fileManager.getTotalStorageBytes()
        val free = fileManager.getAvailableStorageBytes()
        val used = (total - free).coerceAtLeast(0L)
        _storageStats.value = StorageStats(usedBytes = used, freeBytes = free, totalBytes = total)
    }

    fun deleteFile(localFile: LocalMediaFile) = viewModelScope.launch(Dispatchers.IO) {
        if (localFile.file.exists()) {
            localFile.file.delete()
        }
        loadFiles()
    }

    private fun resolveCategory(name: String): LibraryCategory {
        val ext = name.substringAfterLast('.', "").lowercase()
        return when (ext) {
            "mp4", "mkv", "webm", "avi", "mov", "flv" -> LibraryCategory.VIDEOS
            "mp3", "m4a", "ogg", "wav", "flac", "aac" -> LibraryCategory.MUSIC
            "pdf", "doc", "docx", "txt", "epub" -> LibraryCategory.DOCUMENTS
            "zip", "rar", "7z", "tar", "gz" -> LibraryCategory.ARCHIVES
            else -> LibraryCategory.OTHER
        }
    }
}
