package com.nuvio.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppSettingsTest {

    @Test
    fun testDefaultSettings() {
        val settings = AppSettings()

        // 1. Downloads
        assertEquals(3, settings.concurrentDownloads)
        assertEquals(4, settings.connectionsPerDownload)
        assertEquals("", settings.downloadLocation)
        assertFalse(settings.wifiOnly)
        assertTrue(settings.autoResumeOnWifi)
        assertTrue(settings.retryFailed)
        assertEquals(3, settings.maxRetries)
        assertFalse(settings.askBeforeDownload)
        assertTrue(settings.keepDownloadHistory)
        assertTrue(settings.preventDuplicateFilenames)
        assertEquals(0, settings.speedLimitKbps)

        // 2. Video & Audio
        assertEquals("Highest Available", settings.preferredVideoQuality)
        assertEquals("mp4", settings.defaultVideoFormat)
        assertTrue(settings.askQualityBeforeDownload)
        assertTrue(settings.preferOriginalFormat)
        assertFalse(settings.downloadSubtitles)
        assertEquals("320 kbps", settings.preferredAudioQuality)
        assertEquals("mp3", settings.defaultAudioFormat)

        // 3. Notifications
        assertTrue(settings.notifyStarted)
        assertTrue(settings.notifyProgress)
        assertTrue(settings.notifyCompleted)
        assertTrue(settings.notifyFailed)
        assertTrue(settings.notifyUpdates)

        // 4. Browser
        assertEquals("Google", settings.searchEngine)
        assertEquals("https://www.google.com", settings.browserHomepage)
        assertFalse(settings.desktopSite)
        assertTrue(settings.askBeforeDownloadMedia)
        assertTrue(settings.javaScriptEnabled)

        // 5. Media Player
        assertTrue(settings.rememberPlaybackPosition)
        assertFalse(settings.autoPlayNext)
        assertTrue(settings.backgroundAudio)
        assertEquals(1.0f, settings.defaultPlaybackSpeed, 0.001f)
        assertTrue(settings.keepScreenOn)

        // 6. Appearance
        assertNull(settings.darkMode)
        assertTrue(settings.dynamicColor)
        assertEquals("System Default", settings.language)

        // 7. Privacy
        assertFalse(settings.clearHistoryOnExit)
        assertTrue(settings.doNotTrack)

        // 8. Updates
        assertTrue(settings.autoCheckUpdates)
        assertTrue(settings.wifiOnlyUpdates)
    }

    @Test
    fun testCopySettings() {
        val original = AppSettings()
        val modified = original.copy(
            concurrentDownloads = 6,
            connectionsPerDownload = 8,
            wifiOnly = true,
            preferredVideoQuality = "1080p",
            defaultAudioFormat = "m4a",
            searchEngine = "DuckDuckGo",
            browserHomepage = "https://duckduckgo.com",
            desktopSite = true,
            darkMode = true,
            defaultPlaybackSpeed = 1.5f,
            clearHistoryOnExit = true
        )

        assertEquals(6, modified.concurrentDownloads)
        assertEquals(8, modified.connectionsPerDownload)
        assertTrue(modified.wifiOnly)
        assertEquals("1080p", modified.preferredVideoQuality)
        assertEquals("m4a", modified.defaultAudioFormat)
        assertEquals("DuckDuckGo", modified.searchEngine)
        assertEquals("https://duckduckgo.com", modified.browserHomepage)
        assertTrue(modified.desktopSite)
        assertEquals(true, modified.darkMode)
        assertEquals(1.5f, modified.defaultPlaybackSpeed, 0.001f)
        assertTrue(modified.clearHistoryOnExit)

        // Ensure untouched values remain default
        assertTrue(modified.javaScriptEnabled)
        assertTrue(modified.rememberPlaybackPosition)
        assertTrue(modified.autoCheckUpdates)
    }

    @Test
    fun testStorageMetricsDataClass() {
        val metrics = StorageMetrics(
            downloadsSizeBytes = 1024L * 1024L * 500L, // 500 MB
            stagingSizeBytes = 1024L * 1024L * 50L,    // 50 MB
            cacheSizeBytes = 1024L * 1024L * 10L,      // 10 MB
            availableBytes = 1024L * 1024L * 1024L * 20L, // 20 GB
            totalBytes = 1024L * 1024L * 1024L * 64L      // 64 GB
        )

        assertEquals(524288000L, metrics.downloadsSizeBytes)
        assertEquals(52428800L, metrics.stagingSizeBytes)
        assertEquals(10485760L, metrics.cacheSizeBytes)
        assertTrue(metrics.availableBytes > 0)
        assertTrue(metrics.totalBytes > metrics.availableBytes)
    }
}
