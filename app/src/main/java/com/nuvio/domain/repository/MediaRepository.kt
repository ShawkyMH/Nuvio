package com.nuvio.domain.repository

import com.nuvio.domain.model.DownloadTask
import com.nuvio.domain.model.FormatOption
import com.nuvio.domain.model.MediaMetadata

interface MediaRepository {
    suspend fun inspectMedia(url: String): Result<MediaMetadata>
}
