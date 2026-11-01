package com.nuvio.domain.model

data class PlaybackHistoryItem(
    val id: String,
    val mediaUri: String,
    val title: String,
    val artist: String? = null,
    val durationMs: Long = 0L,
    val playbackPositionMs: Long = 0L,
    val isVideo: Boolean = true,
    val lastPlayedAt: Long = System.currentTimeMillis()
)
