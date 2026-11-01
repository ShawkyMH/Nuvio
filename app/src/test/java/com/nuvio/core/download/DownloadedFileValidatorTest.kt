package com.nuvio.core.download

import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.io.FileOutputStream

class DownloadedFileValidatorTest {

    private lateinit var tempDir: File

    @Before
    fun setUp() {
        tempDir = File(System.getProperty("java.io.tmpdir"), "nuvio_val_test_${System.currentTimeMillis()}")
        tempDir.mkdirs()
    }

    @After
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    @Test
    fun validate_whenFileDoesNotExist_returnsInvalid() {
        val nonExistent = File(tempDir, "missing.mp4")
        val result = DownloadedFileValidator.validate(nonExistent)
        assertTrue(result is ValidationResult.Invalid)
        assertTrue((result as ValidationResult.Invalid).reason.contains("does not exist"))
    }

    @Test
    fun validate_whenFileIsEmpty_returnsInvalid() {
        val emptyFile = File(tempDir, "empty.mp4").apply { createNewFile() }
        val result = DownloadedFileValidator.validate(emptyFile)
        assertTrue(result is ValidationResult.Invalid)
        assertTrue((result as ValidationResult.Invalid).reason.contains("empty"))
    }

    @Test
    fun validate_whenSizeMismatchesExpected_returnsInvalid() {
        val file = File(tempDir, "test.bin").apply {
            writeBytes(ByteArray(500) { 0x01 })
        }
        val result = DownloadedFileValidator.validate(file, expectedTotalBytes = 1000L)
        assertTrue(result is ValidationResult.Invalid)
        assertTrue((result as ValidationResult.Invalid).reason.contains("size mismatch"))
    }

    @Test
    fun validate_whenHtmlErrorPageSavedAsMp4_returnsInvalid() {
        val htmlContent = "<!DOCTYPE html><html><head><title>403 Forbidden</title></head><body>Access Denied</body></html>"
        val file = File(tempDir, "video.mp4").apply {
            writeText(htmlContent)
        }
        val result = DownloadedFileValidator.validate(file)
        assertTrue(result is ValidationResult.Invalid)
        assertTrue((result as ValidationResult.Invalid).reason.contains("HTML error/access-denied page"))
    }

    @Test
    fun validate_whenHtmlErrorPageSavedAsPdf_returnsInvalid() {
        val htmlContent = "<html><body><h1>404 Not Found</h1></body></html>"
        val file = File(tempDir, "document.pdf").apply {
            writeText(htmlContent)
        }
        val result = DownloadedFileValidator.validate(file)
        assertTrue(result is ValidationResult.Invalid)
        assertTrue((result as ValidationResult.Invalid).reason.contains("HTML error/access-denied page"))
    }

    @Test
    fun validate_whenValidMp4WithFtyp_returnsValid() {
        val file = File(tempDir, "sample.mp4")
        FileOutputStream(file).use { out ->
            // Box size: 32 bytes (0x00000020)
            out.write(byteArrayOf(0x00, 0x00, 0x00, 0x20))
            // Box type: 'ftyp'
            out.write("ftyp".toByteArray(Charsets.US_ASCII))
            // Major brand: 'isom'
            out.write("isom".toByteArray(Charsets.US_ASCII))
            // Minor version
            out.write(byteArrayOf(0x00, 0x00, 0x02, 0x00))
            // Compatible brands: 'isom', 'mp41'
            out.write("isommp41".toByteArray(Charsets.US_ASCII))
            // Some padding payload
            out.write(ByteArray(256) { 0x00 })
        }

        val result = DownloadedFileValidator.validate(file)
        assertTrue(result is ValidationResult.Valid)
    }

    @Test
    fun validate_whenValidWebM_returnsValid() {
        val file = File(tempDir, "sample.webm")
        FileOutputStream(file).use { out ->
            // EBML magic bytes: 0x1A 0x45 0xDF 0xA3
            out.write(byteArrayOf(0x1A.toByte(), 0x45.toByte(), 0xDF.toByte(), 0xA3.toByte()))
            out.write(ByteArray(100) { 0x00 })
        }

        val result = DownloadedFileValidator.validate(file)
        assertTrue(result is ValidationResult.Valid)
    }

    @Test
    fun validate_whenValidMp3WithId3Tag_returnsValid() {
        val file = File(tempDir, "audio.mp3")
        FileOutputStream(file).use { out ->
            // ID3 tag header: "ID3"
            out.write(byteArrayOf(0x49.toByte(), 0x44.toByte(), 0x33.toByte(), 0x03, 0x00, 0x00, 0x00, 0x00, 0x01, 0x00))
            out.write(ByteArray(500) { 0x20 })
        }

        val result = DownloadedFileValidator.validate(file)
        assertTrue(result is ValidationResult.Valid)
    }

    @Test
    fun validate_whenValidMp3WithMpegFrameSync_returnsValid() {
        val file = File(tempDir, "raw_audio.mp3")
        FileOutputStream(file).use { out ->
            // MPEG 1 Layer 3 frame sync: 0xFF 0xFB
            out.write(byteArrayOf(0xFF.toByte(), 0xFB.toByte(), 0x90.toByte(), 0x64.toByte()))
            out.write(ByteArray(300) { 0x10 })
        }

        val result = DownloadedFileValidator.validate(file)
        assertTrue(result is ValidationResult.Valid)
    }

    @Test
    fun validate_whenValidPdf_returnsValid() {
        val file = File(tempDir, "document.pdf")
        FileOutputStream(file).use { out ->
            out.write("%PDF-1.5\n%Header".toByteArray(Charsets.US_ASCII))
            out.write(ByteArray(200) { 0x00 })
        }

        val result = DownloadedFileValidator.validate(file)
        assertTrue(result is ValidationResult.Valid)
    }

    @Test
    fun validate_whenValidZip_returnsValid() {
        val file = File(tempDir, "archive.zip")
        FileOutputStream(file).use { out ->
            out.write(byteArrayOf(0x50.toByte(), 0x4B.toByte(), 0x03.toByte(), 0x04.toByte()))
            out.write(ByteArray(100) { 0x00 })
        }

        val result = DownloadedFileValidator.validate(file)
        assertTrue(result is ValidationResult.Valid)
    }

    @Test
    fun validate_whenValidPng_returnsValid() {
        val file = File(tempDir, "image.png")
        FileOutputStream(file).use { out ->
            out.write(byteArrayOf(0x89.toByte(), 0x50.toByte(), 0x4E.toByte(), 0x47.toByte(), 0x0D.toByte(), 0x0A.toByte(), 0x1A.toByte(), 0x0A.toByte()))
            out.write(ByteArray(50) { 0x00 })
        }

        val result = DownloadedFileValidator.validate(file)
        assertTrue(result is ValidationResult.Valid)
    }

    @Test
    fun validate_whenValidJpeg_returnsValid() {
        val file = File(tempDir, "image.jpg")
        FileOutputStream(file).use { out ->
            out.write(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte()))
            out.write(ByteArray(50) { 0x00 })
        }

        val result = DownloadedFileValidator.validate(file)
        assertTrue(result is ValidationResult.Valid)
    }

    @Test
    fun validate_whenCorruptMp4Header_returnsInvalid() {
        val file = File(tempDir, "bad.mp4")
        FileOutputStream(file).use { out ->
            // Random non-MP4 bytes
            out.write(byteArrayOf(0x12, 0x34, 0x56, 0x78, 0x90.toByte(), 0x12, 0x34, 0x56))
            out.write(ByteArray(100) { 0x7F })
        }

        val result = DownloadedFileValidator.validate(file)
        assertTrue(result is ValidationResult.Invalid)
        assertTrue((result as ValidationResult.Invalid).reason.contains("missing ISO-BMFF box structure"))
    }
}
