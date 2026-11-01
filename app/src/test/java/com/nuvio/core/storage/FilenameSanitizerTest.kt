package com.nuvio.core.storage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class FilenameSanitizerTest {

    @Test
    fun testSanitize_stripsPathTraversal() {
        val input = "../../etc/passwd"
        val result = FilenameSanitizer.sanitize(input)
        assertFalse("Result should not contain path separators", result.contains("/"))
        assertFalse("Result should not contain path separators", result.contains("\\"))
        assertFalse("Result should not contain ..", result.contains(".."))
    }

    @Test
    fun testSanitize_removesIllegalCharacters() {
        val input = "my:illegal*file?name<is>here|.mp4"
        val result = FilenameSanitizer.sanitize(input)
        assertFalse(result.contains(":"))
        assertFalse(result.contains("*"))
        assertFalse(result.contains("?"))
        assertFalse(result.contains("<"))
        assertFalse(result.contains(">"))
        assertFalse(result.contains("|"))
        assertTrue(result.endsWith(".mp4"))
    }

    @Test
    fun testSanitize_blankInputReturnsFallback() {
        val result = FilenameSanitizer.sanitize("   ", fallback = "fallback_file")
        assertEquals("fallback_file", result)
    }

    @Test
    fun testSanitize_truncatesTo255Characters() {
        val longName = "a".repeat(300) + ".mp4"
        val result = FilenameSanitizer.sanitize(longName)
        assertTrue(result.length <= 255)
        assertTrue(result.endsWith(".mp4"))
    }
}
