package com.nuvio.core.download

import android.os.Build
import java.io.File
import java.io.FileInputStream
import java.util.Locale

sealed interface ValidationResult {
    object Valid : ValidationResult
    data class Invalid(val reason: String) : ValidationResult {
        val isCorrupted: Boolean get() = true
    }
}

object DownloadedFileValidator {

    private val MEDIA_EXTENSIONS = setOf(
        "mp4", "m4a", "mp3", "mkv", "webm", "avi", "mov", "flv", "3gp",
        "wav", "ogg", "opus", "flac"
    )

    private val DOCUMENT_ARCHIVE_IMAGE_EXTENSIONS = setOf(
        "pdf", "zip", "apk", "jar", "png", "jpg", "jpeg", "gif", "webp", "tar", "gz"
    )

    /**
     * Validates downloaded file integrity, completeness, non-emptiness, and file signature.
     */
    fun validate(
        file: File,
        expectedTotalBytes: Long = -1L,
        mimeType: String? = null
    ): ValidationResult {
        if (!file.exists()) {
            return ValidationResult.Invalid("Downloaded file does not exist at destination: ${file.name}")
        }

        if (!file.isFile) {
            return ValidationResult.Invalid("Destination path is not a file: ${file.name}")
        }

        val actualLength = file.length()
        if (actualLength == 0L) {
            return ValidationResult.Invalid("Downloaded file is empty (0 bytes)")
        }

        if (!file.canRead()) {
            return ValidationResult.Invalid("Downloaded file cannot be read: permission denied")
        }

        // Verify file length matches expected Content-Length when known
        if (expectedTotalBytes > 0L && actualLength != expectedTotalBytes) {
            return ValidationResult.Invalid(
                "File size mismatch: expected $expectedTotalBytes bytes, but received $actualLength bytes"
            )
        }

        val extension = file.extension.lowercase(Locale.ROOT)
        val headerBytes = readHeaderBytes(file, 1024)

        // 1. Detect HTML / XML error pages saved under media or archive extensions
        if (isHtmlOrXmlResponse(headerBytes)) {
            if (MEDIA_EXTENSIONS.contains(extension) || DOCUMENT_ARCHIVE_IMAGE_EXTENSIONS.contains(extension)) {
                return ValidationResult.Invalid(
                    "Downloaded content is an HTML error/access-denied page saved as .${extension.uppercase()}"
                )
            }
        }

        // 2. Validate Magic Bytes / Format Signatures
        val signatureResult = validateMagicBytes(extension, headerBytes, actualLength)
        if (signatureResult is ValidationResult.Invalid) {
            return signatureResult
        }

        // 3. Android MediaExtractor verification for media files on Android runtime
        if (isAndroidRuntime() && MEDIA_EXTENSIONS.contains(extension)) {
            val mediaCheck = validateWithAndroidMediaExtractor(file)
            if (mediaCheck is ValidationResult.Invalid) {
                return mediaCheck
            }
        }

        return ValidationResult.Valid
    }

    private fun readHeaderBytes(file: File, maxBytes: Int): ByteArray {
        return runCatching {
            FileInputStream(file).use { input ->
                val buffer = ByteArray(maxBytes)
                val read = input.read(buffer)
                if (read > 0) buffer.copyOf(read) else ByteArray(0)
            }
        }.getOrDefault(ByteArray(0))
    }

    private fun isHtmlOrXmlResponse(headerBytes: ByteArray): Boolean {
        if (headerBytes.isEmpty()) return false
        val sample = String(headerBytes, Charsets.UTF_8).trim().lowercase(Locale.ROOT)
        return sample.startsWith("<!doctype html") ||
                sample.startsWith("<html") ||
                sample.startsWith("<?xml") ||
                sample.contains("<html") ||
                sample.contains("<head>") ||
                sample.contains("<body>") ||
                sample.contains("<title>403") ||
                sample.contains("<title>404") ||
                sample.contains("<title>error") ||
                sample.contains("cloudflare")
    }

    private fun validateMagicBytes(extension: String, bytes: ByteArray, fileLength: Long): ValidationResult {
        if (bytes.isEmpty()) {
            return ValidationResult.Invalid("Unable to read file header")
        }

        when (extension) {
            "mp4", "m4a", "mov" -> {
                // ISO Base Media File Format:
                // Bytes 4-7 must be "ftyp", "moov", "mdat", "free", "skip", or "wide"
                if (bytes.size >= 8) {
                    val boxType = String(bytes.copyOfRange(4, 8), Charsets.US_ASCII)
                    val validBoxes = setOf("ftyp", "moov", "mdat", "free", "skip", "wide")
                    if (!validBoxes.contains(boxType)) {
                        // Check if "ftyp" appears within the first 64 bytes
                        val sampleAscii = String(bytes.take(64).toByteArray(), Charsets.US_ASCII)
                        if (!sampleAscii.contains("ftyp") && !sampleAscii.contains("moov") && !sampleAscii.contains("mdat")) {
                            return ValidationResult.Invalid("Invalid MP4/M4A: missing ISO-BMFF box structure (no ftyp/moov box)")
                        }
                    }
                }
            }
            "webm", "mkv" -> {
                // EBML Header: 0x1A 0x45 0xDF 0xA3
                if (bytes.size >= 4) {
                    val isEbml = (bytes[0].toInt() and 0xFF == 0x1A) &&
                            (bytes[1].toInt() and 0xFF == 0x45) &&
                            (bytes[2].toInt() and 0xFF == 0xDF) &&
                            (bytes[3].toInt() and 0xFF == 0xA3)
                    if (!isEbml) {
                        return ValidationResult.Invalid("Invalid WebM/MKV: missing EBML header signature")
                    }
                }
            }
            "mp3" -> {
                // ID3 tag: starts with "ID3" (0x49 0x44 0x33)
                // OR MPEG audio frame sync: first byte 0xFF and top 3 bits of 2nd byte are 1 (0xE0)
                if (bytes.size >= 3) {
                    val isId3 = bytes[0] == 0x49.toByte() && bytes[1] == 0x44.toByte() && bytes[2] == 0x33.toByte()
                    val isMpegSync = (bytes[0].toInt() and 0xFF == 0xFF) && ((bytes[1].toInt() and 0xE0) == 0xE0)
                    if (!isId3 && !isMpegSync) {
                        return ValidationResult.Invalid("Invalid MP3: missing ID3 tag header or MPEG frame sync")
                    }
                }
            }
            "pdf" -> {
                // PDF header: starts with "%PDF" (0x25 0x50 0x44 0x46)
                if (bytes.size >= 4) {
                    val isPdf = bytes[0] == 0x25.toByte() && bytes[1] == 0x50.toByte() &&
                            bytes[2] == 0x44.toByte() && bytes[3] == 0x46.toByte()
                    if (!isPdf) {
                        return ValidationResult.Invalid("Invalid PDF: missing %PDF header signature")
                    }
                }
            }
            "zip", "apk", "jar" -> {
                // ZIP magic bytes: "PK\x03\x04" (0x50 0x4B 0x03 0x04) or "PK\x05\x06" or "PK\x07\x08"
                if (bytes.size >= 4) {
                    val isZip = bytes[0] == 0x50.toByte() && bytes[1] == 0x4B.toByte() &&
                            ((bytes[2] == 0x03.toByte() && bytes[3] == 0x04.toByte()) ||
                                    (bytes[2] == 0x05.toByte() && bytes[3] == 0x06.toByte()) ||
                                    (bytes[2] == 0x07.toByte() && bytes[3] == 0x08.toByte()))
                    if (!isZip) {
                        return ValidationResult.Invalid("Invalid ZIP/APK: missing PK archive signature")
                    }
                }
            }
            "png" -> {
                // PNG signature: 89 50 4E 47 0D 0A 1A 0A
                if (bytes.size >= 8) {
                    val isPng = bytes[0] == 0x89.toByte() && bytes[1] == 0x50.toByte() &&
                            bytes[2] == 0x4E.toByte() && bytes[3] == 0x47.toByte() &&
                            bytes[4] == 0x0D.toByte() && bytes[5] == 0x0A.toByte() &&
                            bytes[6] == 0x1A.toByte() && bytes[7] == 0x0A.toByte()
                    if (!isPng) {
                        return ValidationResult.Invalid("Invalid PNG: missing PNG signature")
                    }
                }
            }
            "jpg", "jpeg" -> {
                // JPEG SOI marker: FF D8 FF
                if (bytes.size >= 3) {
                    val isJpg = (bytes[0].toInt() and 0xFF == 0xFF) &&
                            (bytes[1].toInt() and 0xFF == 0xD8) &&
                            (bytes[2].toInt() and 0xFF == 0xFF)
                    if (!isJpg) {
                        return ValidationResult.Invalid("Invalid JPEG: missing SOI marker (FF D8 FF)")
                    }
                }
            }
        }

        return ValidationResult.Valid
    }

    private fun isAndroidRuntime(): Boolean {
        return runCatching {
            Build.VERSION.SDK_INT > 0
        }.getOrDefault(false)
    }

    private fun validateWithAndroidMediaExtractor(file: File): ValidationResult {
        return try {
            val extractorClass = Class.forName("android.media.MediaExtractor")
            val extractor = extractorClass.getDeclaredConstructor().newInstance()
            val setDataSourceMethod = extractorClass.getMethod("setDataSource", String::class.java)
            val getTrackCountMethod = extractorClass.getMethod("getTrackCount")
            val releaseMethod = extractorClass.getMethod("release")

            setDataSourceMethod.invoke(extractor, file.absolutePath)
            val trackCount = getTrackCountMethod.invoke(extractor) as Int
            releaseMethod.invoke(extractor)

            if (trackCount <= 0) {
                ValidationResult.Invalid("Media file contains no playable tracks (trackCount = 0)")
            } else {
                ValidationResult.Valid
            }
        } catch (e: Exception) {
            // If MediaExtractor failed to parse the container
            val causeMsg = e.cause?.message ?: e.message ?: "Failed to parse media container"
            ValidationResult.Invalid("Corrupted media file structure: $causeMsg")
        }
    }
}
