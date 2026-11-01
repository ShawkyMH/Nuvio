package com.nuvio.domain.repository

import com.nuvio.domain.model.PlaybackHistoryItem
import kotlinx.coroutines.flow.Flow

interface PlaybackRepository {
    fun getPlaybackHistory(): Flow<List<PlaybackHistoryItem>>
    suspend fun savePlaybackPosition(
        mediaUri: String,
        title: String,
        artist: String?,
        positionMs: Long,
        durationMs: Long,
        isVideo: Boolean
    )
    suspend fun getLastPlaybackPosition(mediaUri: String): Long
    suspend fun clearHistory()
}
