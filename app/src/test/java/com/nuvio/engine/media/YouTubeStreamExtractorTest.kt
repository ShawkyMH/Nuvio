package com.nuvio.engine.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class YouTubeStreamExtractorTest {

    @Test
    fun testIsYouTubeUrl() {
        assertTrue(YouTubeStreamExtractor.isYouTubeUrl("https://www.youtube.com/watch?v=dQw4w9WgXcQ"))
        assertTrue(YouTubeStreamExtractor.isYouTubeUrl("https://youtu.be/dQw4w9WgXcQ"))
        assertTrue(YouTubeStreamExtractor.isYouTubeUrl("https://m.youtube.com/watch?v=dQw4w9WgXcQ"))
        assertTrue(YouTubeStreamExtractor.isYouTubeUrl("https://music.youtube.com/watch?v=dQw4w9WgXcQ"))
        assertTrue(YouTubeStreamExtractor.isYouTubeUrl("https://www.youtube.com/shorts/dQw4w9WgXcQ"))
        assertTrue(YouTubeStreamExtractor.isYouTubeUrl("https://www.youtube.com/embed/dQw4w9WgXcQ"))

        assertFalse(YouTubeStreamExtractor.isYouTubeUrl("https://vimeo.com/123456"))
        assertFalse(YouTubeStreamExtractor.isYouTubeUrl("https://example.com/video.mp4"))
        assertFalse(YouTubeStreamExtractor.isYouTubeUrl("https://google.com"))
    }

    @Test
    fun testExtractVideoId() {
        val expected = "dQw4w9WgXcQ"

        assertEquals(expected, YouTubeStreamExtractor.extractVideoId("https://www.youtube.com/watch?v=dQw4w9WgXcQ"))
        assertEquals(expected, YouTubeStreamExtractor.extractVideoId("https://youtu.be/dQw4w9WgXcQ"))
        assertEquals(expected, YouTubeStreamExtractor.extractVideoId("https://m.youtube.com/watch?v=dQw4w9WgXcQ&feature=shared"))
        assertEquals(expected, YouTubeStreamExtractor.extractVideoId("https://www.youtube.com/shorts/dQw4w9WgXcQ"))
        assertEquals(expected, YouTubeStreamExtractor.extractVideoId("https://www.youtube.com/embed/dQw4w9WgXcQ"))
        assertEquals(expected, YouTubeStreamExtractor.extractVideoId("https://www.youtube.com/v/dQw4w9WgXcQ"))
        assertEquals(expected, YouTubeStreamExtractor.extractVideoId("https://music.youtube.com/watch?v=dQw4w9WgXcQ"))
        assertEquals(expected, YouTubeStreamExtractor.extractVideoId("dQw4w9WgXcQ"))

        assertNull(YouTubeStreamExtractor.extractVideoId("https://example.com/file.mp4"))
        assertNull(YouTubeStreamExtractor.extractVideoId("https://youtube.com/feed/subscriptions"))
    }
}
