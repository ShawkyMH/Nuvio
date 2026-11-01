package com.nuvio.core.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ApkIntegrityVerifierTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testCalculateSha256_knownContent() {
        val testFile = tempFolder.newFile("sample.apk")
        testFile.writeText("Nuvio Test APK Content")

        val hash = ApkIntegrityVerifier.calculateSha256(testFile)
        assertEquals(64, hash.length)

        // Verifying with calculated hash returns true
        assertTrue(ApkIntegrityVerifier.verify(testFile, hash))
    }

    @Test
    fun testVerify_invalidHashFails() {
        val testFile = tempFolder.newFile("sample2.apk")
        testFile.writeText("Valid Content")

        val wrongHash = "0000000000000000000000000000000000000000000000000000000000000000"
        assertFalse(ApkIntegrityVerifier.verify(testFile, wrongHash))
    }

    @Test
    fun testExtractHashFromChecksumText_sha256sumFormat() {
        val standardOutput = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855  nuvio-v1.0.0.apk"
        val extracted = ApkIntegrityVerifier.extractHashFromChecksumText(standardOutput)
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", extracted)
    }

    @Test
    fun testExtractHashFromChecksumText_invalidText() {
        val invalid = "Not a hash"
        assertNull(ApkIntegrityVerifier.extractHashFromChecksumText(invalid))
    }

    @Test
    fun testVerify_nonExistentFileFails() {
        val missing = File(tempFolder.root, "does_not_exist.apk")
        assertFalse(ApkIntegrityVerifier.verify(missing, "somehash"))
    }
}
