package com.nuvio.core.database

import android.content.ContentValues
import android.database.Cursor
import com.nuvio.domain.model.PlaybackHistoryItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

class PlaybackHistoryDao(private val dbHelper: NuvioDatabaseHelper) {

    private val tableUpdates = MutableSharedFlow<Unit>(extraBufferCapacity = 64)

    fun observePlaybackHistory(): Flow<List<PlaybackHistoryItem>> = flow {
        emit(getHistory())
        tableUpdates.collect {
            emit(getHistory())
        }
    }.flowOn(Dispatchers.IO)

    suspend fun getHistory(): List<PlaybackHistoryItem> = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            "playback_history",
            null,
            null,
            null,
            null,
            null,
            "lastPlayedAt DESC"
        )
        val list = mutableListOf<PlaybackHistoryItem>()
        cursor.use {
            while (it.moveToNext()) {
                list.add(it.toPlaybackHistoryItem())
            }
        }
        list
    }

    suspend fun savePlaybackPosition(
        id: String,
        mediaUri: String,
        title: String,
        artist: String?,
        positionMs: Long,
        durationMs: Long,
        isVideo: Boolean
    ) = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("id", id)
            put("mediaUri", mediaUri)
            put("title", title)
            put("artist", artist)
            put("playbackPositionMs", positionMs)
            put("durationMs", durationMs)
            put("isVideo", if (isVideo) 1 else 0)
            put("lastPlayedAt", System.currentTimeMillis())
        }
        db.insertWithOnConflict(
            "playback_history",
            null,
            values,
            android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE
        )
        tableUpdates.tryEmit(Unit)
    }

    suspend fun getLastPosition(mediaUri: String): Long = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            "playback_history",
            arrayOf("playbackPositionMs"),
            "mediaUri = ?",
            arrayOf(mediaUri),
            null,
            null,
            null
        )
        cursor.use {
            if (it.moveToFirst()) {
                it.getLong(it.getColumnIndexOrThrow("playbackPositionMs"))
            } else {
                0L
            }
        }
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        db.delete("playback_history", null, null)
        tableUpdates.tryEmit(Unit)
    }

    private fun Cursor.toPlaybackHistoryItem(): PlaybackHistoryItem {
        return PlaybackHistoryItem(
            id = getString(getColumnIndexOrThrow("id")),
            mediaUri = getString(getColumnIndexOrThrow("mediaUri")),
            title = getString(getColumnIndexOrThrow("title")),
            artist = getString(getColumnIndexOrThrow("artist")),
            durationMs = getLong(getColumnIndexOrThrow("durationMs")),
            playbackPositionMs = getLong(getColumnIndexOrThrow("playbackPositionMs")),
            isVideo = getInt(getColumnIndexOrThrow("isVideo")) == 1,
            lastPlayedAt = getLong(getColumnIndexOrThrow("lastPlayedAt"))
        )
    }
}
