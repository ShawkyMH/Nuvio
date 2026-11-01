package com.nuvio.engine.http

import com.nuvio.core.storage.FilenameSanitizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

class HttpRangeProber(private val client: OkHttpClient) {

    suspend fun probe(url: String): Result<RangeProbeResult> = withContext(Dispatchers.IO) {
        runCatching {
            // First attempt: GET with Range 0-0 to check range slicing and Content-Range
            val rangeRequest = Request.Builder()
                .url(url)
                .header("Range", "bytes=0-0")
                .build()

            client.newCall(rangeRequest).execute().use { response ->
                val code = response.code
                val acceptRanges = response.header("Accept-Ranges")
                val contentRange = response.header("Content-Range")
                val etag = response.header("ETag")
                val lastModified = response.header("Last-Modified")
                val mimeType = response.header("Content-Type") ?: "application/octet-stream"
                val contentDisposition = response.header("Content-Disposition")
                val suggestedFileName = parseContentDispositionFilename(contentDisposition)

                if (code == 206 && contentRange != null) {
                    // Example: "bytes 0-0/1048576"
                    val total = parseTotalFromContentRange(contentRange)
                    RangeProbeResult(
                        supportsRanges = true,
                        totalBytes = total,
                        etag = etag,
                        lastModified = lastModified,
                        mimeType = mimeType,
                        suggestedFileName = suggestedFileName
                    )
                } else {
                    // Fall back to Content-Length
                    val contentLength = response.body?.contentLength() ?: -1L
                    val supports = acceptRanges.equals("bytes", ignoreCase = true) && contentLength > 0
                    RangeProbeResult(
                        supportsRanges = supports,
                        totalBytes = contentLength,
                        etag = etag,
                        lastModified = lastModified,
                        mimeType = mimeType,
                        suggestedFileName = suggestedFileName
                    )
                }
            }
        }
    }

    private fun parseTotalFromContentRange(contentRange: String): Long {
        return runCatching {
            val slashIndex = contentRange.lastIndexOf('/')
            if (slashIndex != -1 && slashIndex + 1 < contentRange.length) {
                val totalStr = contentRange.substring(slashIndex + 1).trim()
                if (totalStr != "*") totalStr.toLong() else -1L
            } else {
                -1L
            }
        }.getOrDefault(-1L)
    }

    private fun parseContentDispositionFilename(disposition: String?): String? {
        if (disposition.isNullOrBlank()) return null
        return runCatching {
            val parts = disposition.split(";")
            for (part in parts) {
                val trimmed = part.trim()
                if (trimmed.startsWith("filename*=", ignoreCase = true)) {
                    val value = trimmed.substringAfter("filename*=").trim('"', '\'')
                    val name = if (value.contains("''")) value.substringAfter("''") else value
                    return@runCatching FilenameSanitizer.sanitize(name)
                } else if (trimmed.startsWith("filename=", ignoreCase = true)) {
                    val value = trimmed.substringAfter("filename=").trim('"', '\'')
                    return@runCatching FilenameSanitizer.sanitize(value)
                }
            }
            null
        }.getOrNull()
    }
}
