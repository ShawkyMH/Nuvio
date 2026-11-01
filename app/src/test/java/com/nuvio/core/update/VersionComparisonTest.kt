package com.nuvio.core.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VersionComparisonTest {

    @Test
    fun testParseVersionCode_semanticVersioning() {
        assertEquals(10000L, GitHubReleaseDataSource.parseVersionCode("1.0.0"))
        assertEquals(10100L, GitHubReleaseDataSource.parseVersionCode("1.1.0"))
        assertEquals(10204L, GitHubReleaseDataSource.parseVersionCode("1.2.4"))
        assertEquals(20000L, GitHubReleaseDataSource.parseVersionCode("2.0.0"))
    }

    @Test
    fun testParseVersionCode_explicitInReleaseNotes() {
        val notes = "Release notes...\nversionCode: 42\nFixes bug"
        assertEquals(42L, GitHubReleaseDataSource.parseVersionCode("1.0.0", notes))

        val notesWithEquals = "Features added\nversionCode=150"
        assertEquals(150L, GitHubReleaseDataSource.parseVersionCode("1.0.0", notesWithEquals))
    }

    @Test
    fun testVersionComparison_newerVersionAvailable() {
        val currentCode = 10000L
        val remoteCode = 10100L
        assertTrue("Remote should be detected as newer", remoteCode > currentCode)
    }

    @Test
    fun testVersionComparison_sameVersionIsUpToDate() {
        val currentCode = 10000L
        val remoteCode = 10000L
        assertFalse("Same version should not trigger update", remoteCode > currentCode)
    }

    @Test
    fun testVersionComparison_olderRemoteIgnored() {
        val currentCode = 10500L
        val remoteCode = 10100L
        assertFalse("Older version should not trigger update", remoteCode > currentCode)
    }
}
