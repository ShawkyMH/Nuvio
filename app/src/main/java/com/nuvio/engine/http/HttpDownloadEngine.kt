package com.nuvio.engine.http

import com.nuvio.core.database.DownloadSegmentDao
import com.nuvio.core.database.DownloadTaskDao
import com.nuvio.core.storage.FileManager
import com.nuvio.domain.model.DownloadSegment
import com.nuvio.domain.model.DownloadTask
import com.nuvio.domain.model.SegmentStatus
import com.nuvio.domain.model.TaskStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.coroutineContext

class HttpDownloadEngine(
    private val client: OkHttpClient,
    private val prober: HttpRangeProber,
    private val taskDao: DownloadTaskDao,
    private val segmentDao: DownloadSegmentDao,
    private val fileManager: FileManager
) {

    private val activeJobs = ConcurrentHashMap<String, Job>()
    private val engineScope = CoroutineScope(Dispatchers.IO + Job())

    fun isTaskActive(taskId: String): Boolean = activeJobs[taskId]?.isActive == true

    fun startDownload(task: DownloadTask, maxConnections: Int = 4) {
        if (isTaskActive(task.id)) return

        val job = engineScope.launch {
            try {
                executeTask(task, maxConnections)
            } catch (e: CancellationException) {
                // Task was paused or cancelled
                taskDao.updateTaskStatus(task.id, TaskStatus.PAUSED)
            } catch (e: Exception) {
                taskDao.updateTaskStatus(task.id, TaskStatus.FAILED, e.localizedMessage ?: "Network error")
            } finally {
                activeJobs.remove(task.id)
            }
        }
        activeJobs[task.id] = job
    }

    fun pauseDownload(taskId: String) {
        activeJobs[taskId]?.cancel()
        activeJobs.remove(taskId)
        engineScope.launch {
            taskDao.updateTaskStatus(taskId, TaskStatus.PAUSED)
        }
    }

    fun cancelDownload(taskId: String) {
        activeJobs[taskId]?.cancel()
        activeJobs.remove(taskId)
        engineScope.launch {
            taskDao.updateTaskStatus(taskId, TaskStatus.CANCELLED)
            fileManager.cleanTempFiles(taskId)
        }
    }

    private suspend fun executeTask(task: DownloadTask, maxConnections: Int) = withContext(Dispatchers.IO) {
        taskDao.updateTaskStatus(task.id, TaskStatus.INSPECTING)

        // 1. Probe remote server
        val probe = prober.probe(task.url).getOrNull()
        val totalBytes = probe?.totalBytes ?: task.totalBytes
        val supportsRanges = probe?.supportsRanges ?: false
        val etag = probe?.etag ?: task.etag
        val lastModified = probe?.lastModified ?: task.lastModified

        // Check if server resource changed on resume
        if (task.etag != null && etag != null && task.etag != etag) {
            // ETag mutated, restart from 0
            fileManager.cleanTempFiles(task.id)
            segmentDao.deleteSegmentsForTask(task.id)
        }

        val tempFile = fileManager.getTempFileForTask(task.id)
        val targetFile = File(task.filePath)

        if (supportsRanges && totalBytes > 0) {
            downloadWithSegments(
                task = task.copy(
                    totalBytes = totalBytes,
                    supportsRanges = true,
                    etag = etag,
                    lastModified = lastModified
                ),
                tempFile = tempFile,
                targetFile = targetFile,
                maxConnections = maxConnections.coerceIn(1, 8)
            )
        } else {
            downloadSingleStream(
                task = task.copy(totalBytes = totalBytes, supportsRanges = false),
                tempFile = tempFile,
                targetFile = targetFile
            )
        }
    }

    private suspend fun downloadWithSegments(
        task: DownloadTask,
        tempFile: File,
        targetFile: File,
        maxConnections: Int
    ) = coroutineScope {
        taskDao.updateTaskStatus(task.id, TaskStatus.DOWNLOADING)

        // Pre-allocate temp file
        if (!tempFile.exists() || tempFile.length() < task.totalBytes) {
            RandomAccessFile(tempFile, "rw").use { it.setLength(task.totalBytes) }
        }

        // Retrieve existing segments or initialize new ones
        var segments = segmentDao.getSegmentsForTask(task.id)
        if (segments.isEmpty()) {
            val count = maxConnections
            val segmentSize = task.totalBytes / count
            val newSegments = mutableListOf<DownloadSegment>()
            for (i in 0 until count) {
                val start = i * segmentSize
                val end = if (i == count - 1) task.totalBytes - 1 else (start + segmentSize - 1)
                newSegments.add(
                    DownloadSegment(
                        id = "${task.id}_$i",
                        taskId = task.id,
                        segmentIndex = i,
                        startByte = start,
                        endByte = end,
                        downloadedBytes = 0L,
                        status = SegmentStatus.PENDING
                    )
                )
            }
            segmentDao.insertSegments(newSegments)
            segments = newSegments
        }

        val totalDownloadedAtomic = AtomicLong(segments.sumOf { it.downloadedBytes })
        val speedTracker = SpeedTracker()
        speedTracker.reset(totalDownloadedAtomic.get())

        // Launch worker coroutine for each segment
        val segmentJobs = segments.map { segment ->
            async(Dispatchers.IO) {
                val worker = SegmentWorker(
                    client = client,
                    segment = segment,
                    url = task.url,
                    destinationFile = tempFile,
                    onProgress = { bytesWritten, status ->
                        val diff = bytesWritten - segment.downloadedBytes
                        val currentTotal = totalDownloadedAtomic.addAndGet(diff)
                        val speed = speedTracker.update(currentTotal)

                        segmentDao.updateSegmentProgress(segment.id, bytesWritten, status)
                        taskDao.updateTaskProgress(task.id, currentTotal, speed, TaskStatus.DOWNLOADING)
                    }
                )
                worker.execute()
            }
        }

        val results = segmentJobs.awaitAll()
        if (results.all { it }) {
            // All segments completed! Atomic finalize
            val success = fileManager.finalizeDownload(tempFile, targetFile)
            if (success) {
                taskDao.updateTaskProgress(task.id, task.totalBytes, 0L, TaskStatus.COMPLETED)
            } else {
                taskDao.updateTaskStatus(task.id, TaskStatus.FAILED, "Failed to move completed file")
            }
        } else {
            taskDao.updateTaskStatus(task.id, TaskStatus.PAUSED)
        }
    }

    private suspend fun downloadSingleStream(
        task: DownloadTask,
        tempFile: File,
        targetFile: File
    ) = withContext(Dispatchers.IO) {
        taskDao.updateTaskStatus(task.id, TaskStatus.DOWNLOADING)

        val request = Request.Builder().url(task.url).build()
        val speedTracker = SpeedTracker()
        speedTracker.reset(0L)

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                taskDao.updateTaskStatus(task.id, TaskStatus.FAILED, "HTTP ${response.code}")
                return@withContext
            }

            val body = response.body ?: run {
                taskDao.updateTaskStatus(task.id, TaskStatus.FAILED, "Empty response")
                return@withContext
            }

            val inputStream = body.byteStream()
            val outputStream = FileOutputStream(tempFile)
            val buffer = ByteArray(16384)
            var downloaded = 0L
            var read = 0

            outputStream.use { out ->
                while (coroutineContext.isActive && inputStream.read(buffer).also { read = it } != -1) {
                    out.write(buffer, 0, read)
                    downloaded += read
                    val speed = speedTracker.update(downloaded)
                    taskDao.updateTaskProgress(task.id, downloaded, speed, TaskStatus.DOWNLOADING)
                }
            }

            if (!coroutineContext.isActive) {
                taskDao.updateTaskStatus(task.id, TaskStatus.PAUSED)
                return@withContext
            }

            val success = fileManager.finalizeDownload(tempFile, targetFile)
            if (success) {
                taskDao.updateTaskProgress(task.id, downloaded, 0L, TaskStatus.COMPLETED)
            } else {
                taskDao.updateTaskStatus(task.id, TaskStatus.FAILED, "Failed to move completed file")
            }
        }
    }
}
