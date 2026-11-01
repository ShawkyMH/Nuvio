package com.nuvio.core.update

import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

object ApkIntegrityVerifier {

    /**
     * Computes SHA-256 hexadecimal checksum of a file.
     */
    fun calculateSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(16384)
        FileInputStream(file).use { fis ->
            var bytesRead: Int
            while (fis.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        val hashBytes = digest.digest()
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Extracts raw 64-character hex hash from a checksum string, which may be
     * formatted as a standalone hash or standard `sha256sum` output (`<hash>  <filename>`).
     */
    fun extractHashFromChecksumText(text: String): String? {
        val clean = text.trim()
        val firstToken = clean.split(Regex("\\s+")).firstOrNull() ?: return null
        return if (firstToken.length == 64 && firstToken.all { it.isDigit() || it in 'a'..'f' || it in 'A'..'F' }) {
            firstToken.lowercase()
        } else {
            null
        }
    }

    /**
     * Verifies that the file exists, has content, and matches the expected SHA-256 checksum.
     */
    fun verify(file: File, expectedChecksumText: String?): Boolean {
        if (!file.exists() || file.length() <= 0) return false

        if (expectedChecksumText.isNullOrBlank()) {
            // No checksum provided in release assets; file is structurally non-empty
            return true
        }

        val expectedHash = extractHashFromChecksumText(expectedChecksumText) ?: return false
        val actualHash = calculateSha256(file)

        return actualHash.equals(expectedHash, ignoreCase = true)
    }
}
