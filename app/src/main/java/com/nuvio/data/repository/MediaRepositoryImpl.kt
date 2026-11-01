package com.nuvio.data.repository

import com.nuvio.domain.model.MediaMetadata
import com.nuvio.domain.repository.MediaRepository
import com.nuvio.engine.ytdlp.YtDlpEngine

class MediaRepositoryImpl(
    private val ytDlpEngine: YtDlpEngine
) : MediaRepository {

    override suspend fun inspectMedia(url: String): Result<MediaMetadata> {
        return ytDlpEngine.inspectUrl(url)
    }
}
