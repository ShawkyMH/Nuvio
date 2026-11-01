package com.nuvio.domain.repository

import com.nuvio.domain.model.DownloadSegment
import com.nuvio.domain.model.DownloadTask
import com.nuvio.domain.model.TaskStatus
import kotlinx.coroutines.flow.Flow

interface DownloadRepository {
    fun getAllTasks(): Flow<List<DownloadTask>>
    fun getActiveTasks(): Flow<List<DownloadTask>>
    fun getCompletedTasks(): Flow<List<DownloadTask>>
    fun getTaskById(id: String): Flow<DownloadTask?>
    fun getSegmentsForTask(taskId: String): Flow<List<DownloadSegment>>

    suspend fun insertTask(task: DownloadTask)
    suspend fun updateTask(task: DownloadTask)
    suspend fun updateTaskProgress(taskId: String, downloadedBytes: Long, speed: Long, status: TaskStatus)
    suspend fun deleteTask(taskId: String, deleteFile: Boolean)
    suspend fun pauseTask(taskId: String)
    suspend fun resumeTask(taskId: String)
    suspend fun retryTask(taskId: String)
    suspend fun cancelTask(taskId: String)
    suspend fun pauseAll()
    suspend fun resumeAll()
}
