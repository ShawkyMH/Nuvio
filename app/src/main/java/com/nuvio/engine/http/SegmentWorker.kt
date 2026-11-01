package com.nuvio.engine.http

import com.nuvio.domain.model.DownloadSegment
import com.nuvio.domain.model.SegmentStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.RandomAccessFile
import kotlin.coroutines.coroutineContext

class SegmentWorker(
    private val client: OkHttpClient,
    private val segment: DownloadSegment,
    private val url: String,
    private val destinationFile: File,
    private val onProgress: suspend (bytesWritten: Long, status: SegmentStatus, deltaBytes: Long) -> Unit
) {

    suspend fun execute(): Boolean = withContext(Dispatchers.IO) {
        val currentOffset = segment.startByte + segment.downloadedBytes
        if (currentOffset > segment.endByte) {
            onProgress(segment.downloadedBytes, SegmentStatus.COMPLETED, 0L)
            return@withContext true
        }

        val reqBuilder = Request.Builder()
            .url(url)
            .header("Range", "bytes=$currentOffset-${segment.endByte}")
            .header("Accept-Encoding", "identity")

        if (url.contains("googlevideo.com")) {
            reqBuilder.header("User-Agent", "com.google.android.youtube/19.09.37 (Linux; U; Android 14; en_US) gzip")
        }
        val request = reqBuilder.build()

        var raf: RandomAccessFile? = null
        try {
            onProgress(segment.downloadedBytes, SegmentStatus.DOWNLOADING, 0L)
            client.newCall(request).execute().use { response ->
                if (response.code != 206) {
                    onProgress(segment.downloadedBytes, SegmentStatus.FAILED, 0L)
                    return@withContext false
                }

                val body = response.body ?: run {
                    onProgress(segment.downloadedBytes, SegmentStatus.FAILED, 0L)
                    return@withContext false
                }

                raf = RandomAccessFile(destinationFile, "rw")
                raf!!.seek(currentOffset)

                val inputStream = body.byteStream()
                val buffer = ByteArray(16384) // 16 KB chunks
                var totalSegmentDownloaded = segment.downloadedBytes
                var read = 0

                while (coroutineContext.isActive && inputStream.read(buffer).also { read = it } != -1) {
                    raf!!.write(buffer, 0, read)
                    totalSegmentDownloaded += read
                    onProgress(totalSegmentDownloaded, SegmentStatus.DOWNLOADING, read.toLong())
                }

                if (!coroutineContext.isActive) {
                    // Task was cancelled or paused
                    return@withContext false
                }

                runCatching { raf?.channel?.force(true) }

                val isComplete = totalSegmentDownloaded >= segment.totalBytes
                val finalStatus = if (isComplete) SegmentStatus.COMPLETED else SegmentStatus.FAILED
                onProgress(totalSegmentDownloaded, finalStatus, 0L)
                return@withContext isComplete
            }
        } catch (e: Exception) {
            if (coroutineContext.isActive) {
                onProgress(segment.downloadedBytes, SegmentStatus.FAILED, 0L)
            }
            return@withContext false
        } finally {
            runCatching { raf?.close() }
        }
    }
}
