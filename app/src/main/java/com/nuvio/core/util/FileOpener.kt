package com.nuvio.core.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.core.content.FileProvider
import com.nuvio.core.update.ApkInstaller
import java.io.File
import java.util.Locale

sealed interface FileOpenResult {
    object HandledInternally : FileOpenResult
    object OpenedWithExternalApp : FileOpenResult
    data class NoAppFound(val fileName: String, val mimeType: String) : FileOpenResult
    data class Error(val message: String) : FileOpenResult
}

object FileOpener {

    private val MEDIA_EXTENSIONS = setOf(
        "mp4", "m4a", "mp3", "mkv", "webm", "avi", "mov", "flv", "3gp",
        "wav", "ogg", "opus", "flac"
    )

    fun isMediaFile(file: File): Boolean {
        val ext = file.extension.lowercase(Locale.ROOT)
        return MEDIA_EXTENSIONS.contains(ext)
    }

    fun isMediaFile(filePath: String): Boolean {
        return isMediaFile(File(filePath))
    }

    fun openFile(
        context: Context,
        filePath: String,
        onPlayMedia: ((filePath: String, title: String) -> Unit)? = null
    ): FileOpenResult {
        val file = File(filePath)
        if (!file.exists()) {
            Toast.makeText(context, "File does not exist: ${file.name}", Toast.LENGTH_SHORT).show()
            return FileOpenResult.Error("File does not exist")
        }

        val extension = file.extension.lowercase(Locale.ROOT)

        // 1. Audio and Video: Play inside Nuvio's internal Media Player if handler provided
        if (isMediaFile(file) && onPlayMedia != null) {
            onPlayMedia(file.absolutePath, file.name)
            return FileOpenResult.HandledInternally
        }

        // 2. APK: Use ApkInstaller
        if (extension == "apk") {
            val installer = ApkInstaller(context)
            installer.installApk(file)
            return FileOpenResult.OpenedWithExternalApp
        }

        // 3. Other files (PDF, ZIP, images, docs, etc.): Open via FileProvider & Intent.ACTION_VIEW
        return try {
            val authority = "${context.packageName}.fileprovider"
            val contentUri = FileProvider.getUriForFile(context, authority, file)

            val mimeType = getMimeType(file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(intent)
            FileOpenResult.OpenedWithExternalApp
        } catch (e: ActivityNotFoundException) {
            val mimeType = getMimeType(file)
            Toast.makeText(
                context,
                "No compatible app installed to open ${file.name}",
                Toast.LENGTH_LONG
            ).show()
            FileOpenResult.NoAppFound(file.name, mimeType)
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to open file: ${e.message}", Toast.LENGTH_SHORT).show()
            FileOpenResult.Error(e.localizedMessage ?: "Unknown error")
        }
    }

    fun getMimeType(file: File): String {
        val ext = file.extension.lowercase(Locale.ROOT)
        val fromMap = runCatching { MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) }.getOrNull()
        if (!fromMap.isNullOrBlank()) return fromMap

        return when (ext) {
            "pdf" -> "application/pdf"
            "zip" -> "application/zip"
            "apk" -> "application/vnd.android.package-archive"
            "mp4" -> "video/mp4"
            "mp3" -> "audio/mpeg"
            "m4a" -> "audio/mp4"
            "mkv" -> "video/x-matroska"
            "webm" -> "video/webm"
            "png" -> "image/png"
            "jpg", "jpeg" -> "image/jpeg"
            "txt" -> "text/plain"
            else -> "*/*"
        }
    }
}
