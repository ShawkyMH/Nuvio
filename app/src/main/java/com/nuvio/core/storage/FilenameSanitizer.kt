package com.nuvio.core.storage

import java.io.File

object FilenameSanitizer {

    private val ILLEGAL_CHARACTERS = Regex("[\\\\/:*?\"<>|\\x00-\\x1F]")

    /**
     * Sanitizes an arbitrary untrusted string into a safe filesystem filename.
     * Prevents path traversal attacks (e.g. `../../evil.sh`), strips null bytes,
     * removes shell characters, and constrains length to 255 bytes.
     */
    fun sanitize(input: String, fallback: String = "download"): String {
        var clean = input.trim()

        // Strip path traversal attempts and directory separators
        clean = clean.replace("..", "_")
        clean = clean.replace(ILLEGAL_CHARACTERS, "_")

        // Collapse multiple underscores or spaces
        clean = clean.replace(Regex("_+"), "_")

        // Strip leading/trailing dots or spaces
        clean = clean.trim('.', ' ', '_')

        if (clean.isBlank()) {
            clean = fallback
        }

        // Limit to 255 characters (standard ext4/FAT max name length)
        if (clean.length > 255) {
            val extIndex = clean.lastIndexOf('.')
            val ext = if (extIndex > 0 && (clean.length - extIndex) <= 16) {
                clean.substring(extIndex)
            } else {
                ""
            }
            clean = clean.substring(0, 255 - ext.length) + ext
        }

        return clean
    }

    /**
     * Resolves a unique destination file in the target directory, appending ` (1)`, ` (2)`
     * if a file with the same name already exists.
     */
    fun resolveUniqueFile(directory: File, fileName: String): File {
        val sanitized = sanitize(fileName)
        var file = File(directory, sanitized)
        if (!file.exists()) return file

        val dotIndex = sanitized.lastIndexOf('.')
        val name = if (dotIndex > 0) sanitized.substring(0, dotIndex) else sanitized
        val ext = if (dotIndex > 0) sanitized.substring(dotIndex) else ""

        var counter = 1
        while (file.exists()) {
            file = File(directory, "$name ($counter)$ext")
            counter++
        }
        return file
    }
}
