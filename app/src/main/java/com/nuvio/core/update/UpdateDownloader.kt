package com.nuvio.core.update

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.coroutineContext

class UpdateDownloader(
    private val client: OkHttpClient,
    private val context: Context
) {

    val updatesDir: File
        get() {
            val dir = File(context.filesDir, "updates")
            if (!dir.exists()) {
                dir.mkdirs()
            }
            return dir
        }

    suspend fun download(
        updateInfo: UpdateInfo,
        onProgress: (downloadedBytes: Long, totalBytes: Long, percent: Int) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val destinationFile = File(updatesDir, updateInfo.apkFileName)
            val tempFile = File(updatesDir, "${updateInfo.apkFileName}.part")

            if (tempFile.exists()) {
                tempFile.delete()
            }

            val request = Request.Builder()
                .url(updateInfo.apkDownloadUrl)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw RuntimeException("HTTP ${response.code} downloading update APK")
                }

                val body = response.body ?: throw RuntimeException("Empty response body")
                val totalBytes = if (response.body?.contentLength() ?: -1L > 0) {
                    response.body!!.contentLength()
                } else {
                    updateInfo.apkSizeBytes
                }

                val inputStream = body.byteStream()
                val outputStream = FileOutputStream(tempFile)
                val buffer = ByteArray(16384)
                var downloadedBytes = 0L
                var read = 0

                outputStream.use { out ->
                    while (coroutineContext.isActive && inputStream.read(buffer).also { read = it } != -1) {
                        out.write(buffer, 0, read)
                        downloadedBytes += read
                        val percent = if (totalBytes > 0) {
                            ((downloadedBytes.toDouble() / totalBytes.toDouble()) * 100).toInt().coerceIn(0, 100)
                        } else 0
                        onProgress(downloadedBytes, totalBytes, percent)
                    }
                }

                if (!coroutineContext.isActive) {
                    tempFile.delete()
                    throw RuntimeException("Download cancelled")
                }

                if (destinationFile.exists()) {
                    destinationFile.delete()
                }

                val renamed = tempFile.renameTo(destinationFile)
                if (!renamed) {
                    throw RuntimeException("Failed to rename temporary APK to destination")
                }

                destinationFile
            }
        }
    }

    fun cleanUpdates() {
        val files = updatesDir.listFiles() ?: return
        for (f in files) {
            runCatching { f.delete() }
        }
    }
}
