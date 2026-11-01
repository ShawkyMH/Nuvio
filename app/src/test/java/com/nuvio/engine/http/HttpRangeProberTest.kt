package com.nuvio.engine.http

import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.util.concurrent.atomic.AtomicBoolean

class HttpRangeProberTest {

    private lateinit var serverSocket: ServerSocket
    private lateinit var baseUrl: String
    private val isRunning = AtomicBoolean(true)
    private val client = OkHttpClient.Builder().build()
    private val prober = HttpRangeProber(client)

    @Volatile
    private var responseHandler: (request: String, out: OutputStream) -> Unit = { _, _ -> }

    @Before
    fun setUp() {
        isRunning.set(true)
        serverSocket = ServerSocket(0, 50, InetAddress.getByName("127.0.0.1"))
        baseUrl = "http://127.0.0.1:${serverSocket.localPort}"

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
    }

    @Test
    fun probe_whenServerSupportsRange_returnsProbeResultWithRangesAndResolvedUrl() = runBlocking {
        responseHandler = { _, out ->
            val response = "HTTP/1.1 206 Partial Content\r\n" +
                    "Content-Range: bytes 0-0/4096\r\n" +
                    "Accept-Ranges: bytes\r\n" +
                    "Content-Type: application/octet-stream\r\n" +
                    "Content-Length: 1\r\n" +
                    "\r\n" +
                    "X"
            out.write(response.toByteArray(Charsets.UTF_8))
            out.flush()
        }

        val result = prober.probe("$baseUrl/range-test").getOrThrow()

        assertTrue("Should support ranges", result.supportsRanges)
        assertEquals(4096L, result.totalBytes)
        assertEquals("$baseUrl/range-test", result.resolvedUrl)
    }

    @Test
    fun probe_whenServerReturns200WithAcceptRanges_returnsTrue() = runBlocking {
        responseHandler = { _, out ->
            val response = "HTTP/1.1 200 OK\r\n" +
                    "Accept-Ranges: bytes\r\n" +
                    "Content-Length: 1024\r\n" +
                    "Content-Type: text/plain\r\n" +
                    "\r\n" +
                    "A".repeat(1024)
            out.write(response.toByteArray(Charsets.UTF_8))
            out.flush()
        }

        val result = prober.probe("$baseUrl/direct-test").getOrThrow()

        assertTrue("Should support ranges via Accept-Ranges", result.supportsRanges)
        assertEquals(1024L, result.totalBytes)
        assertEquals("$baseUrl/direct-test", result.resolvedUrl)
    }

    @Test
    fun probe_whenServerReturns404_failsWithException() = runBlocking {
        responseHandler = { _, out ->
            val response = "HTTP/1.1 404 Not Found\r\n" +
                    "Content-Length: 0\r\n" +
                    "\r\n"
            out.write(response.toByteArray(Charsets.UTF_8))
            out.flush()
        }

        val result = prober.probe("$baseUrl/not-found")
        assertTrue("Probe should fail on 404", result.isFailure)
    }
}
