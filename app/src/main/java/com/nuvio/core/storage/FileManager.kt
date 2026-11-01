package com.nuvio.core.storage

import android.content.Context
import android.os.Environment
import android.os.StatFs
import java.io.File

class FileManager(private val context: Context) {

    /**
     * Standard downloads directory for staging active and completed downloads.
     */
    val downloadsDir: File
        get() {
            val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                ?: File(context.filesDir, "downloads")
            if (!dir.exists()) {
                dir.mkdirs()
            }
            return dir
        }

    /**
     * Staging directory for temporary segment files.
     * Placed within downloadsDir when available so file moves are atomic and on the same mount.
     */
    val tempDir: File
        get() {
            val dir = File(downloadsDir, ".staging")
            if (!dir.exists()) {
                dir.mkdirs()
            }
            return dir
        }

    /**
     * Returns available free storage space in bytes.
     */
    fun getAvailableStorageBytes(): Long {
        return runCatching {
            val stat = StatFs(downloadsDir.path)
            stat.availableBytes
        }.getOrDefault(0L)
    }

    /**
     * Returns total storage space in bytes.
     */
    fun getTotalStorageBytes(): Long {
        return runCatching {
            val stat = StatFs(downloadsDir.path)
            stat.totalBytes
        }.getOrDefault(0L)
    }

    /**
     * Prepares target and temporary files for a new task.
     */
    fun createTargetFile(fileName: String): File {
        return FilenameSanitizer.resolveUniqueFile(downloadsDir, fileName)
    }

    fun getTempFileForTask(taskId: String): File {
        return File(tempDir, "$taskId.tmp")
    }

    /**
     * Safely finalizes the temp file to the final target file.
     * Tries atomic rename first; if cross-device/filesystem link occurs, falls back to stream copying.
     */
    fun finalizeDownload(tempFile: File, targetFile: File): Boolean {
        if (!tempFile.exists()) return false

        val parent = targetFile.parentFile
        if (parent != null && !parent.exists()) {
            parent.mkdirs()
        }

        if (targetFile.exists()) {
            targetFile.delete()
        }

        // 1. Try atomic filesystem rename
        if (tempFile.renameTo(targetFile)) {
            return true
        }

        // 2. Fallback for cross-device mount moves (e.g. internal cache to external SD)
        return try {
            tempFile.inputStream().buffered().use { input ->
                targetFile.outputStream().buffered().use { output ->
                    input.copyTo(output)
                }
            }
            tempFile.delete()
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Deletes temporary artifacts for a cancelled or failed task.
     */
    fun cleanTempFiles(taskId: String) {
        val temp = getTempFileForTask(taskId)
        if (temp.exists()) {
            temp.delete()
        }
    }
}
