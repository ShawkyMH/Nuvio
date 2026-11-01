package com.nuvio.core.update

import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GitHubReleaseDataSourceTest {

    private fun createMockClient(responseCode: Int, responseBody: String): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor { chain ->
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(responseCode)
                    .message(if (responseCode == 200) "OK" else "Error")
                    .body(responseBody.toResponseBody("application/json".toMediaType()))
                    .build()
            }
            .build()
    }

    @Test
    fun testGetLatestRelease_successWithApkAndChecksum() = runBlocking {
        val json = """
            {
                "tag_name": "v1.1.0",
                "name": "Nuvio v1.1.0",
                "body": "Bug fixes and improvements",
                "prerelease": false,
                "draft": false,
                "published_at": "2026-11-01T12:00:00Z",
                "assets": [
                    {
                        "name": "nuvio-v1.1.0.apk",
                        "size": 19500000,
                        "browser_download_url": "https://github.com/ShawkyMH/Nuvio/releases/download/v1.1.0/nuvio-v1.1.0.apk"
                    },
                    {
                        "name": "nuvio-v1.1.0.apk.sha256",
                        "size": 64,
                        "browser_download_url": "https://github.com/ShawkyMH/Nuvio/releases/download/v1.1.0/nuvio-v1.1.0.apk.sha256"
                    }
                ]
            }
        """.trimIndent()

        val client = createMockClient(200, json)
        val dataSource = GitHubReleaseDataSource(client)

        val result = dataSource.getLatestRelease(includePrerelease = false)
        if (result.isFailure) {
            println("FAILURE CAUSE: " + result.exceptionOrNull())
            result.exceptionOrNull()?.printStackTrace()
        }
        assertTrue(result.isSuccess)

        val info = result.getOrNull()
        assertNotNull(info)
        assertEquals("v1.1.0", info?.tagName)
        assertEquals("1.1.0", info?.versionName)
        assertEquals(10100L, info?.versionCode)
        assertEquals("nuvio-v1.1.0.apk", info?.apkFileName)
        assertEquals("https://github.com/ShawkyMH/Nuvio/releases/download/v1.1.0/nuvio-v1.1.0.apk", info?.apkDownloadUrl)
    }

    @Test
    fun testGetLatestRelease_missingApkAssetReturnsNull() = runBlocking {
        val json = """
            {
                "tag_name": "v1.0.5",
                "name": "Source Only Release",
                "body": "No APK",
                "prerelease": false,
                "draft": false,
                "assets": []
            }
        """.trimIndent()

        val client = createMockClient(200, json)
        val dataSource = GitHubReleaseDataSource(client)

        val result = dataSource.getLatestRelease(includePrerelease = false)
        if (result.isFailure) {
            println("FAILURE CAUSE: " + result.exceptionOrNull())
            result.exceptionOrNull()?.printStackTrace()
        }
        assertTrue(result.isSuccess)
        assertNull(result.getOrNull())
    }

    @Test
    fun testGetLatestRelease_draftReleaseIgnored() = runBlocking {
        val json = """
            {
                "tag_name": "v1.2.0-draft",
                "name": "Draft Release",
                "prerelease": false,
                "draft": true,
                "assets": [
                    {
                        "name": "nuvio.apk",
                        "browser_download_url": "https://example.com/nuvio.apk"
                    }
                ]
            }
        """.trimIndent()

        val client = createMockClient(200, json)
        val dataSource = GitHubReleaseDataSource(client)

        val result = dataSource.getLatestRelease(includePrerelease = false)
        if (result.isFailure) {
            println("FAILURE CAUSE: " + result.exceptionOrNull())
            result.exceptionOrNull()?.printStackTrace()
        }
        assertTrue(result.isSuccess)
        assertNull(result.getOrNull())
    }

    @Test
    fun testGetLatestRelease_404ReturnsNull() = runBlocking {
        val client = createMockClient(404, "Not Found")
        val dataSource = GitHubReleaseDataSource(client)

        val result = dataSource.getLatestRelease(includePrerelease = false)
        if (result.isFailure) {
            println("FAILURE CAUSE: " + result.exceptionOrNull())
            result.exceptionOrNull()?.printStackTrace()
        }
        assertTrue(result.isSuccess)
        assertNull(result.getOrNull())
    }

    @Test
    fun testGetLatestRelease_rateLimitThrowsException() = runBlocking {
        val client = createMockClient(403, "API rate limit exceeded")
        val dataSource = GitHubReleaseDataSource(client)

        val result = dataSource.getLatestRelease(includePrerelease = false)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("rate limit") == true)
    }
}
