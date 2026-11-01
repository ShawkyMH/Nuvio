package com.nuvio.core.database

import android.content.ContentValues
import android.database.Cursor
import com.nuvio.domain.model.DownloadSegment
import com.nuvio.domain.model.SegmentStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

class DownloadSegmentDao(private val dbHelper: NuvioDatabaseHelper) {

    private val tableUpdates = MutableSharedFlow<Unit>(extraBufferCapacity = 64)

    fun observeSegmentsForTask(taskId: String): Flow<List<DownloadSegment>> = flow {
        emit(getSegmentsForTask(taskId))
        tableUpdates.collect {
            emit(getSegmentsForTask(taskId))
        }
    }.flowOn(Dispatchers.IO)

    suspend fun getSegmentsForTask(taskId: String): List<DownloadSegment> = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            "download_segments",
            null,
            "taskId = ?",
            arrayOf(taskId),
            null,
            null,
            "segmentIndex ASC"
        )
        val segments = mutableListOf<DownloadSegment>()
        cursor.use {
            while (it.moveToNext()) {
                segments.add(it.toDownloadSegment())
            }
        }
        segments
    }

    suspend fun insertSegments(segments: List<DownloadSegment>) = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            for (segment in segments) {
                val values = ContentValues().apply {
                    put("id", segment.id)
                    put("taskId", segment.taskId)
                    put("segmentIndex", segment.segmentIndex)
                    put("startByte", segment.startByte)
                    put("endByte", segment.endByte)
                    put("downloadedBytes", segment.downloadedBytes)
                    put("status", segment.status.name)
                }
                db.insertWithOnConflict(
                    "download_segments",
                    null,
                    values,
                    android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE
                )
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        tableUpdates.tryEmit(Unit)
    }

    suspend fun updateSegmentProgress(segmentId: String, downloadedBytes: Long, status: SegmentStatus) =
        withContext(Dispatchers.IO) {
            val db = dbHelper.writableDatabase
            val values = ContentValues().apply {
                put("downloadedBytes", downloadedBytes)
                put("status", status.name)
            }
            db.update("download_segments", values, "id = ?", arrayOf(segmentId))
            tableUpdates.tryEmit(Unit)
        }

    suspend fun deleteSegmentsForTask(taskId: String) = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        db.delete("download_segments", "taskId = ?", arrayOf(taskId))
        tableUpdates.tryEmit(Unit)
    }

    private fun Cursor.toDownloadSegment(): DownloadSegment {
        return DownloadSegment(
            id = getString(getColumnIndexOrThrow("id")),
            taskId = getString(getColumnIndexOrThrow("taskId")),
            segmentIndex = getInt(getColumnIndexOrThrow("segmentIndex")),
            startByte = getLong(getColumnIndexOrThrow("startByte")),
            endByte = getLong(getColumnIndexOrThrow("endByte")),
            downloadedBytes = getLong(getColumnIndexOrThrow("downloadedBytes")),
            status = runCatching {
                SegmentStatus.valueOf(getString(getColumnIndexOrThrow("status")))
            }.getOrDefault(SegmentStatus.FAILED)
        )
    }
}
