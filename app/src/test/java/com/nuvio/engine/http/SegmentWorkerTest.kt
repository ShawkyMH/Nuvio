package com.nuvio.engine.http

import com.nuvio.domain.model.DownloadSegment
import com.nuvio.domain.model.SegmentStatus
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.RandomAccessFile
import java.net.InetAddress
import java.net.ServerSocket
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

class SegmentWorkerTest {

    private lateinit var serverSocket: ServerSocket
    private lateinit var baseUrl: String
    private lateinit var tempFile: File
    private val isRunning = AtomicBoolean(true)
    private val client = OkHttpClient.Builder().build()
    private val sampleData = ByteArray(1024) { i -> (i % 256).toByte() }

    @Volatile
    private var responseHandler: (request: String, out: OutputStream) -> Unit = { _, _ -> }

    @Before
    fun setUp() {
        isRunning.set(true)
        serverSocket = ServerSocket(0, 50, InetAddress.getByName("127.0.0.1"))
        baseUrl = "http://127.0.0.1:${serverSocket.localPort}"

        tempFile = File.createTempFile("nuvio_seg_test", ".tmp")
        RandomAccessFile(tempFile, "rw").use { raf ->
            raf.setLength(1024)
        }

        Thread {
            while (isRunning.get() && !serverSocket.isClosed) {
                try {
                    val socket = serverSocket.accept()
                    Thread {
                        socket.use { s ->
                            val reader = BufferedReader(InputStreamReader(s.getInputStream()))
                            val headerBuilder = StringBuilder()
                            var line: String?
                            while (reader.readLine().also { line = it } != null) {
                                if (line.isNullOrBlank()) break
                                headerBuilder.append(line).append("\r\n")
                            }
                            responseHandler(headerBuilder.toString(), s.getOutputStream())
                        }
                    }.start()
                } catch (_: Exception) {
                    break
                }
            }
        }.start()
    }

    @After
    fun tearDown() {
        isRunning.set(false)
        runCatching { serverSocket.close() }
        if (tempFile.exists()) {
            tempFile.delete()
        }
    }

    @Test
    fun downloadSegment_whenServerReturns206_writesSegmentAtCorrectOffsetAndEmitsDelta() = runBlocking {
        responseHandler = { requestHeader, out ->
            val rangeLine = requestHeader.lines().find { it.startsWith("Range:", ignoreCase = true) }
            if (rangeLine != null && rangeLine.contains("bytes=256-511")) {
                val slice = sampleData.copyOfRange(256, 512)
                val response = "HTTP/1.1 206 Partial Content\r\n" +
                        "Content-Range: bytes 256-511/1024\r\n" +
                        "Content-Length: ${slice.size}\r\n" +
                        "Content-Type: application/octet-stream\r\n" +
                        "\r\n"
                out.write(response.toByteArray(Charsets.UTF_8))
                out.write(slice)
                out.flush()
            } else {
                val err = "HTTP/1.1 400 Bad Request\r\n\r\n"
                out.write(err.toByteArray(Charsets.UTF_8))
                out.flush()
            }
        }

        val segment = DownloadSegment(
            id = "seg-1",
            taskId = "task-1",
            segmentIndex = 1,
            startByte = 256L,
            endByte = 511L,
            downloadedBytes = 0L,
            status = SegmentStatus.PENDING
        )

        val totalDeltaAccumulator = AtomicLong(0)
        var lastStatus = SegmentStatus.PENDING

        val worker = SegmentWorker(
            client = client,
            segment = segment,
            url = "$baseUrl/media-range",
            destinationFile = tempFile,
            onProgress = { _, status, deltaBytes ->
                lastStatus = status
                totalDeltaAccumulator.addAndGet(deltaBytes)
            }
        )

        val success = worker.execute()
        assertTrue("Worker should succeed", success)
        assertEquals(SegmentStatus.COMPLETED, lastStatus)
        assertEquals(256L, totalDeltaAccumulator.get())

        // Verify bytes in file between 256 and 511 match sampleData
        val fileBytes = tempFile.readBytes()
        val writtenSlice = fileBytes.copyOfRange(256, 512)
        val expectedSlice = sampleData.copyOfRange(256, 512)
        assertArrayEquals(expectedSlice, writtenSlice)
    }

    @Test
    fun downloadSegment_whenServerReturns200InsteadOf206_returnsFalseAndFails() = runBlocking {
        responseHandler = { _, out ->
            // Send 200 OK instead of requested 206
            val response = "HTTP/1.1 200 OK\r\n" +
                    "Content-Length: ${sampleData.size}\r\n" +
                    "Content-Type: application/octet-stream\r\n" +
                    "\r\n"
            out.write(response.toByteArray(Charsets.UTF_8))
            out.write(sampleData)
            out.flush()
        }

        val segment = DownloadSegment(
            id = "seg-0",
            taskId = "task-2",
            segmentIndex = 0,
            startByte = 0L,
            endByte = 255L,
            downloadedBytes = 0L,
            status = SegmentStatus.PENDING
        )

        var lastStatus = SegmentStatus.PENDING
        val worker = SegmentWorker(
            client = client,
            segment = segment,
            url = "$baseUrl/bad-range",
            destinationFile = tempFile,
            onProgress = { _, status, _ ->
                lastStatus = status
            }
        )

        val success = worker.execute()
        assertFalse("Worker must reject 200 OK for range request", success)
        assertEquals(SegmentStatus.FAILED, lastStatus)
    }
}
