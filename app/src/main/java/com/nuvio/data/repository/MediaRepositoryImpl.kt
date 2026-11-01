package com.nuvio.data.repository

import com.nuvio.domain.model.MediaMetadata
import com.nuvio.domain.repository.MediaRepository
import com.nuvio.engine.media.YouTubeStreamExtractor
import com.nuvio.engine.ytdlp.YtDlpEngine

class MediaRepositoryImpl(
    private val ytDlpEngine: YtDlpEngine,
    private val youTubeExtractor: YouTubeStreamExtractor? = null
) : MediaRepository {

    override suspend fun inspectMedia(url: String): Result<MediaMetadata> {
        if (YouTubeStreamExtractor.isYouTubeUrl(url) && youTubeExtractor != null) {
            val ytResult = youTubeExtractor.extract(url)
            if (ytResult.isSuccess) {
                return ytResult
            }
        }
        return ytDlpEngine.inspectUrl(url)
    }
}
