package com.nuvio.data.repository

import com.nuvio.core.database.DownloadSegmentDao
import com.nuvio.core.database.DownloadTaskDao
import com.nuvio.core.storage.FileManager
import com.nuvio.domain.model.DownloadSegment
import com.nuvio.domain.model.DownloadTask
import com.nuvio.domain.model.TaskStatus
import com.nuvio.domain.repository.DownloadRepository
import com.nuvio.engine.http.HttpDownloadEngine
import kotlinx.coroutines.flow.Flow
import java.io.File

class DownloadRepositoryImpl(
    private val taskDao: DownloadTaskDao,
    private val segmentDao: DownloadSegmentDao,
    private val downloadEngine: HttpDownloadEngine,
    private val fileManager: FileManager
) : DownloadRepository {

    override fun getAllTasks(): Flow<List<DownloadTask>> = taskDao.observeAllTasks()

    override fun getActiveTasks(): Flow<List<DownloadTask>> = taskDao.observeActiveTasks()

    override fun getCompletedTasks(): Flow<List<DownloadTask>> = taskDao.observeCompletedTasks()

    override fun getTaskById(id: String): Flow<DownloadTask?> = taskDao.observeTaskById(id)

    override fun getSegmentsForTask(taskId: String): Flow<List<DownloadSegment>> =
        segmentDao.observeSegmentsForTask(taskId)

    override suspend fun insertTask(task: DownloadTask) {
        taskDao.insertTask(task)
        downloadEngine.startDownload(task, maxConnections = task.connectionCount)
    }

    override suspend fun updateTask(task: DownloadTask) {
        taskDao.updateTask(task)
    }

    override suspend fun updateTaskProgress(
        taskId: String,
        downloadedBytes: Long,
        speed: Long,
        status: TaskStatus
    ) {
        taskDao.updateTaskProgress(taskId, downloadedBytes, speed, status)
    }

    override suspend fun deleteTask(taskId: String, deleteFile: Boolean) {
        downloadEngine.cancelDownload(taskId)
        val task = taskDao.getTaskById(taskId)
        if (deleteFile && task != null) {
            val file = File(task.filePath)
            if (file.exists()) {
                file.delete()
            }
        }
        taskDao.deleteTask(taskId)
        segmentDao.deleteSegmentsForTask(taskId)
    }

    override suspend fun pauseTask(taskId: String) {
        downloadEngine.pauseDownload(taskId)
    }

    override suspend fun resumeTask(taskId: String) {
        val task = taskDao.getTaskById(taskId) ?: return
        taskDao.updateTaskStatus(taskId, TaskStatus.QUEUED)
        downloadEngine.startDownload(task, maxConnections = task.connectionCount)
    }

    override suspend fun retryTask(taskId: String) {
        val task = taskDao.getTaskById(taskId) ?: return
        taskDao.updateTaskStatus(taskId, TaskStatus.QUEUED)
        downloadEngine.startDownload(task, maxConnections = task.connectionCount)
    }

    override suspend fun cancelTask(taskId: String) {
        downloadEngine.cancelDownload(taskId)
    }

    override suspend fun pauseAll() {
        val active = taskDao.getAllTasksDirect().filter { it.status.isActive }
        for (task in active) {
            pauseTask(task.id)
        }
    }

    override suspend fun resumeAll() {
        val paused = taskDao.getAllTasksDirect().filter { it.status.isPaused }
        for (task in paused) {
            resumeTask(task.id)
        }
    }
}
