package com.nuvio.core.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UrlValidatorTest {

    @Test
    fun testIsValidUrl_validHttpAndHttps() {
        assertTrue(UrlValidator.isValidUrl("https://example.com/video.mp4"))
        assertTrue(UrlValidator.isValidUrl("http://cdn.site.org/file.zip?id=123"))
    }

    @Test
    fun testIsValidUrl_invalidSchemes() {
        assertFalse(UrlValidator.isValidUrl("ftp://files.example.com/file.iso"))
        assertFalse(UrlValidator.isValidUrl("file:///android_asset/file.txt"))
        assertFalse(UrlValidator.isValidUrl("javascript:alert(1)"))
        assertFalse(UrlValidator.isValidUrl("content://media/external/123"))
    }

    @Test
    fun testIsValidUrl_blocksLoopbackAndLocalNetworks() {
        assertFalse(UrlValidator.isValidUrl("http://localhost/admin"))
        assertFalse(UrlValidator.isValidUrl("http://127.0.0.1:8080/secret"))
        assertFalse(UrlValidator.isValidUrl("http://0.0.0.0/test"))
        assertFalse(UrlValidator.isValidUrl("http://169.254.169.254/latest/meta-data/"))
        assertFalse(UrlValidator.isValidUrl("http://192.168.1.1/router"))
        assertFalse(UrlValidator.isValidUrl("http://10.0.0.1/internal"))
    }

    @Test
    fun testExtractFileNameFromUrl() {
        assertEquals(
            "sample.mp4",
            UrlValidator.extractFileNameFromUrl("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample.mp4")
        )
        assertEquals(
            "archive.zip",
            UrlValidator.extractFileNameFromUrl("https://cdn.example.org/download/archive.zip?token=xyz#hash")
        )
        assertEquals(
            "default",
            UrlValidator.extractFileNameFromUrl("https://example.com/", fallback = "default")
        )
    }

    @Test
    fun testIsMediaSource() {
        assertTrue(UrlValidator.isMediaSource("https://www.youtube.com/watch?v=dQw4w9WgXcQ"))
        assertTrue(UrlValidator.isMediaSource("https://youtu.be/dQw4w9WgXcQ"))
        assertTrue(UrlValidator.isMediaSource("https://example.com/stream.m3u8"))
        assertTrue(UrlValidator.isMediaSource("https://example.com/media/audio.mp3"))
        assertFalse(UrlValidator.isMediaSource("https://example.com/document.pdf"))
    }
}
