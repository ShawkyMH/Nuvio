package com.nuvio.core.database

import android.content.ContentValues
import android.database.Cursor
import com.nuvio.domain.model.DownloadTask
import com.nuvio.domain.model.TaskStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

class DownloadTaskDao(private val dbHelper: NuvioDatabaseHelper) {

    private val tableUpdates = MutableSharedFlow<Unit>(extraBufferCapacity = 64)

    fun observeAllTasks(): Flow<List<DownloadTask>> = flow {
        emit(queryTasks(null, null))
        tableUpdates.collect {
            emit(queryTasks(null, null))
        }
    }.flowOn(Dispatchers.IO)

    fun observeActiveTasks(): Flow<List<DownloadTask>> = flow {
        val selection = "status IN ('QUEUED', 'INSPECTING', 'DOWNLOADING', 'PROCESSING')"
        emit(queryTasks(selection, null))
        tableUpdates.collect {
            emit(queryTasks(selection, null))
        }
    }.flowOn(Dispatchers.IO)

    fun observeCompletedTasks(): Flow<List<DownloadTask>> = flow {
        val selection = "status = 'COMPLETED'"
        emit(queryTasks(selection, null))
        tableUpdates.collect {
            emit(queryTasks(selection, null))
        }
    }.flowOn(Dispatchers.IO)

    fun observeTaskById(id: String): Flow<DownloadTask?> = flow {
        emit(getTaskById(id))
        tableUpdates.collect {
            emit(getTaskById(id))
        }
    }.flowOn(Dispatchers.IO)

    suspend fun getTaskById(id: String): DownloadTask? = withContext(Dispatchers.IO) {
        val list = queryTasks("id = ?", arrayOf(id))
        list.firstOrNull()
    }

    suspend fun insertTask(task: DownloadTask) = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("id", task.id)
            put("url", task.url)
            put("title", task.title)
            put("filePath", task.filePath)
            put("mimeType", task.mimeType)
            put("totalBytes", task.totalBytes)
            put("downloadedBytes", task.downloadedBytes)
            put("status", task.status.name)
            put("supportsRanges", if (task.supportsRanges) 1 else 0)
            put("etag", task.etag)
            put("lastModified", task.lastModified)
            put("connectionCount", task.connectionCount)
            put("downloadSpeed", task.downloadSpeed)
            put("isMediaExtraction", if (task.isMediaExtraction) 1 else 0)
            put("formatId", task.formatId)
            put("thumbnailUri", task.thumbnailUri)
            put("errorMessage", task.errorMessage)
            put("createdAt", task.createdAt)
            put("completedAt", task.completedAt)
        }
        db.insertWithOnConflict(
            "download_tasks",
            null,
            values,
            android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE
        )
        tableUpdates.tryEmit(Unit)
    }

    suspend fun updateTask(task: DownloadTask) = withContext(Dispatchers.IO) {
        insertTask(task)
    }

    suspend fun updateTaskProgress(
        taskId: String,
        downloadedBytes: Long,
        speed: Long,
        status: TaskStatus
    ) = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("downloadedBytes", downloadedBytes)
            put("downloadSpeed", speed)
            put("status", status.name)
            if (status == TaskStatus.COMPLETED) {
                put("completedAt", System.currentTimeMillis())
            }
        }
        db.update("download_tasks", values, "id = ?", arrayOf(taskId))
        tableUpdates.tryEmit(Unit)
    }

    suspend fun updateTaskStatus(taskId: String, status: TaskStatus, errorMessage: String? = null) =
        withContext(Dispatchers.IO) {
            val db = dbHelper.writableDatabase
            val values = ContentValues().apply {
                put("status", status.name)
                put("downloadSpeed", 0L)
                if (errorMessage != null) {
                    put("errorMessage", errorMessage)
                }
                if (status == TaskStatus.COMPLETED) {
                    put("completedAt", System.currentTimeMillis())
                }
            }
            db.update("download_tasks", values, "id = ?", arrayOf(taskId))
            tableUpdates.tryEmit(Unit)
        }

    suspend fun deleteTask(taskId: String) = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        db.delete("download_tasks", "id = ?", arrayOf(taskId))
        tableUpdates.tryEmit(Unit)
    }

    suspend fun getAllTasksDirect(): List<DownloadTask> = withContext(Dispatchers.IO) {
        queryTasks(null, null)
    }

    private fun queryTasks(selection: String?, selectionArgs: Array<String>?): List<DownloadTask> {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            "download_tasks",
            null,
            selection,
            selectionArgs,
            null,
            null,
            "createdAt DESC"
        )
        val tasks = mutableListOf<DownloadTask>()
        cursor.use {
            while (it.moveToNext()) {
                tasks.add(it.toDownloadTask())
            }
        }
        return tasks
    }

    private fun Cursor.toDownloadTask(): DownloadTask {
        return DownloadTask(
            id = getString(getColumnIndexOrThrow("id")),
            url = getString(getColumnIndexOrThrow("url")),
            title = getString(getColumnIndexOrThrow("title")),
            filePath = getString(getColumnIndexOrThrow("filePath")),
            mimeType = getString(getColumnIndexOrThrow("mimeType")),
            totalBytes = getLong(getColumnIndexOrThrow("totalBytes")),
            downloadedBytes = getLong(getColumnIndexOrThrow("downloadedBytes")),
            status = runCatching {
                TaskStatus.valueOf(getString(getColumnIndexOrThrow("status")))
            }.getOrDefault(TaskStatus.FAILED),
            supportsRanges = getInt(getColumnIndexOrThrow("supportsRanges")) == 1,
            etag = getString(getColumnIndexOrThrow("etag")),
            lastModified = getString(getColumnIndexOrThrow("lastModified")),
            connectionCount = getInt(getColumnIndexOrThrow("connectionCount")),
            downloadSpeed = getLong(getColumnIndexOrThrow("downloadSpeed")),
            isMediaExtraction = getInt(getColumnIndexOrThrow("isMediaExtraction")) == 1,
            formatId = getString(getColumnIndexOrThrow("formatId")),
            thumbnailUri = getString(getColumnIndexOrThrow("thumbnailUri")),
            errorMessage = getString(getColumnIndexOrThrow("errorMessage")),
            createdAt = getLong(getColumnIndexOrThrow("createdAt")),
            completedAt = if (isNull(getColumnIndexOrThrow("completedAt"))) null else getLong(getColumnIndexOrThrow("completedAt"))
        )
    }
}
