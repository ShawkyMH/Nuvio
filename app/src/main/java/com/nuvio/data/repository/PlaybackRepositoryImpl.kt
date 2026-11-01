package com.nuvio.data.repository

import com.nuvio.core.database.PlaybackHistoryDao
import com.nuvio.domain.model.PlaybackHistoryItem
import com.nuvio.domain.repository.PlaybackRepository
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class PlaybackRepositoryImpl(
    private val dao: PlaybackHistoryDao
) : PlaybackRepository {

    override fun getPlaybackHistory(): Flow<List<PlaybackHistoryItem>> = dao.observePlaybackHistory()

    override suspend fun savePlaybackPosition(
        mediaUri: String,
        title: String,
        artist: String?,
        positionMs: Long,
        durationMs: Long,
        isVideo: Boolean
    ) {
        val id = UUID.nameUUIDFromBytes(mediaUri.toByteArray()).toString()
        dao.savePlaybackPosition(
            id = id,
            mediaUri = mediaUri,
            title = title,
            artist = artist,
            positionMs = positionMs,
            durationMs = durationMs,
            isVideo = isVideo
        )
    }

    override suspend fun getLastPlaybackPosition(mediaUri: String): Long {
        return dao.getLastPosition(mediaUri)
    }

    override suspend fun clearHistory() {
        dao.clearHistory()
    }
}
