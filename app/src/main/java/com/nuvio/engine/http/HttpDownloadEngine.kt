package com.nuvio.engine.http

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.nuvio.core.database.DownloadSegmentDao
import com.nuvio.core.database.DownloadTaskDao
import com.nuvio.core.download.DownloadedFileValidator
import com.nuvio.core.download.ValidationResult
import com.nuvio.core.storage.FileManager
import com.nuvio.domain.model.DownloadSegment
import com.nuvio.domain.model.DownloadTask
import com.nuvio.domain.model.SegmentStatus
import com.nuvio.domain.model.TaskStatus
import com.nuvio.domain.repository.SettingsRepository
import com.nuvio.service.download.DownloadExecutionCoordinator
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
    private val fileManager: FileManager,
    private val executionCoordinator: DownloadExecutionCoordinator? = null,
    private val settingsRepository: SettingsRepository? = null,
    private val context: Context? = null
) {

    private val activeJobs = ConcurrentHashMap<String, Job>()
    private val engineScope = CoroutineScope(Dispatchers.IO + Job())

    fun isTaskActive(taskId: String): Boolean = activeJobs[taskId]?.isActive == true

    fun startDownload(task: DownloadTask, maxConnections: Int = 4) {
        if (isTaskActive(task.id)) return

        val wifiOnly = settingsRepository?.settingsFlow?.value?.wifiOnly ?: false
        if (wifiOnly && context != null && !isWifiConnected(context)) {
            engineScope.launch {
                taskDao.updateTaskStatus(task.id, TaskStatus.PAUSED, "Paused: Wi-Fi connection required")
            }
            return
        }

        val effectiveConnections = settingsRepository?.settingsFlow?.value?.connectionsPerDownload ?: maxConnections

        executionCoordinator?.ensureTransferExecution()

        val job = engineScope.launch {
            try {
                executeTask(task, effectiveConnections)
            } catch (e: CancellationException) {
                // Task was paused or cancelled
                taskDao.updateTaskStatus(task.id, TaskStatus.PAUSED)
            } catch (e: Exception) {
                taskDao.updateTaskStatus(task.id, TaskStatus.FAILED, e.localizedMessage ?: "Network error")
            } finally {
                activeJobs.remove(task.id)
                if (activeJobs.isEmpty()) {
                    executionCoordinator?.stopTransferExecution()
                }
            }
        }
        activeJobs[task.id] = job
    }

    private fun isWifiConnected(ctx: Context): Boolean {
        val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return true
        val activeNet = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(activeNet) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    fun pauseDownload(taskId: String) {
        activeJobs[taskId]?.cancel()
        activeJobs.remove(taskId)
        if (activeJobs.isEmpty()) {
            executionCoordinator?.stopTransferExecution()
        }
        engineScope.launch {
            taskDao.updateTaskStatus(taskId, TaskStatus.PAUSED)
        }
    }

    fun cancelDownload(taskId: String) {
        activeJobs[taskId]?.cancel()
        activeJobs.remove(taskId)
        if (activeJobs.isEmpty()) {
            executionCoordinator?.stopTransferExecution()
        }
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
        val resolvedUrl = probe?.resolvedUrl?.takeIf { it.isNotBlank() } ?: task.url

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
                resolvedUrl = resolvedUrl,
                tempFile = tempFile,
                targetFile = targetFile,
                maxConnections = maxConnections.coerceIn(1, 8)
            )
        } else {
            downloadSingleStream(
                task = task.copy(totalBytes = totalBytes, supportsRanges = false),
                resolvedUrl = resolvedUrl,
                tempFile = tempFile,
                targetFile = targetFile
            )
        }
    }

    private suspend fun downloadWithSegments(
        task: DownloadTask,
        resolvedUrl: String,
        tempFile: File,
        targetFile: File,
        maxConnections: Int
    ): Unit = coroutineScope {
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
                    url = resolvedUrl,
                    destinationFile = tempFile,
                    onProgress = { currentSegmentDownloaded, status, delta ->
                        val currentTotal = totalDownloadedAtomic.addAndGet(delta)
                        val speed = speedTracker.update(currentTotal)

                        segmentDao.updateSegmentProgress(segment.id, currentSegmentDownloaded, status)
                        taskDao.updateTaskProgress(task.id, currentTotal, speed, TaskStatus.DOWNLOADING)
                    }
                )
                worker.execute()
            }
        }

        val results = segmentJobs.awaitAll()
        if (results.all { it }) {
            // All segments completed! Proceed to finalize and validate
            finalizeAndValidate(task, tempFile, targetFile, task.totalBytes)
        } else {
            if (coroutineContext.isActive) {
                // Segment download encountered an unrecoverable failure (e.g. range rejection);
                // clean segments and fall back automatically to single-stream download
                segmentDao.deleteSegmentsForTask(task.id)
                if (tempFile.exists()) tempFile.delete()
                downloadSingleStream(task.copy(supportsRanges = false), resolvedUrl, tempFile, targetFile)
            } else {
                taskDao.updateTaskStatus(task.id, TaskStatus.PAUSED)
            }
        }
    }

    private suspend fun downloadSingleStream(
        task: DownloadTask,
        resolvedUrl: String,
        tempFile: File,
        targetFile: File
    ) = withContext(Dispatchers.IO) {
        taskDao.updateTaskStatus(task.id, TaskStatus.DOWNLOADING)

        val request = Request.Builder()
            .url(resolvedUrl)
            .header("Accept-Encoding", "identity")
            .build()
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

            val resolvedTotal = if (task.totalBytes > 0) task.totalBytes else body.contentLength()
            if (resolvedTotal > 0 && resolvedTotal != task.totalBytes) {
                taskDao.updateTask(task.copy(totalBytes = resolvedTotal))
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
                out.flush()
            }

            if (!coroutineContext.isActive) {
                taskDao.updateTaskStatus(task.id, TaskStatus.PAUSED)
                return@withContext
            }

            // Check for premature truncation
            if (resolvedTotal > 0 && downloaded < resolvedTotal) {
                tempFile.delete()
                taskDao.updateTaskStatus(
                    task.id,
                    TaskStatus.FAILED,
                    "Transfer prematurely terminated: received $downloaded of $resolvedTotal bytes"
                )
                return@withContext
            }

            finalizeAndValidate(task, tempFile, targetFile, resolvedTotal)
        }
    }

    private suspend fun finalizeAndValidate(
        task: DownloadTask,
        tempFile: File,
        targetFile: File,
        expectedBytes: Long
    ) {
        // 1. FINALIZING: Move staging file to target destination
        taskDao.updateTaskStatus(task.id, TaskStatus.FINALIZING)
        val success = fileManager.finalizeDownload(tempFile, targetFile)
        if (!success) {
            taskDao.updateTaskStatus(task.id, TaskStatus.FAILED, "Failed to move completed file to destination")
            return
        }

        // 2. VALIDATING: Check completeness, non-emptiness, magic bytes, and absence of HTML errors
        taskDao.updateTaskStatus(task.id, TaskStatus.VALIDATING)
        val validation = DownloadedFileValidator.validate(
            file = targetFile,
            expectedTotalBytes = expectedBytes,
            mimeType = task.mimeType
        )

        when (validation) {
            is ValidationResult.Valid -> {
                val finalTotal = if (expectedBytes > 0) expectedBytes else targetFile.length()
                taskDao.updateTaskProgress(task.id, finalTotal, 0L, TaskStatus.COMPLETED)
            }
            is ValidationResult.Invalid -> {
                // Delete invalid or corrupted file to protect user storage
                runCatching { targetFile.delete() }
                taskDao.updateTaskStatus(task.id, TaskStatus.FAILED, validation.reason)
            }
        }
    }
}
