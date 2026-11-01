package com.nuvio.core.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

class GitHubReleaseDataSource(
    private val client: OkHttpClient
) {

    suspend fun getLatestRelease(includePrerelease: Boolean = false): Result<UpdateInfo?> = withContext(Dispatchers.IO) {
        runCatching {
            val url = if (includePrerelease) {
                UpdateConfig.ALL_RELEASES_URL
            } else {
                UpdateConfig.LATEST_RELEASE_URL
            }

            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "Nuvio-Android-App")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.code == 404) {
                    // No releases found
                    return@runCatching null
                }

                if (response.code == 403) {
                    throw RuntimeException("GitHub API rate limit exceeded. Please try again later.")
                }

                if (!response.isSuccessful) {
                    throw RuntimeException("HTTP ${response.code} when checking updates from GitHub")
                }

                val bodyStr = response.body?.string() ?: throw RuntimeException("Empty response from GitHub")

                val parsed = SimpleJsonParser(bodyStr).parse()
                @Suppress("UNCHECKED_CAST")
                val releaseMap = if (includePrerelease && parsed is List<*>) {
                    parsed.firstOrNull() as? Map<String, Any?>
                } else {
                    parsed as? Map<String, Any?>
                } ?: return@runCatching null

                parseReleaseMap(releaseMap)
            }
        }
    }

    private fun parseReleaseMap(map: Map<String, Any?>): UpdateInfo? {
        val tagName = map["tag_name"]?.toString() ?: ""
        if (tagName.isBlank()) return null

        val isPrerelease = map["prerelease"] as? Boolean ?: false
        val isDraft = map["draft"] as? Boolean ?: false
        if (isDraft) return null

        val releaseTitle = map["name"]?.toString() ?: tagName
        val releaseNotes = map["body"]?.toString() ?: "Bug fixes and performance improvements."
        val publishedAt = map["published_at"]?.toString() ?: ""

        val assetsList = map["assets"] as? List<*> ?: emptyList<Any>()
        var apkDownloadUrl: String? = null
        var apkFileName = ""
        var apkSizeBytes = 0L
        var checksumDownloadUrl: String? = null

        for (item in assetsList) {
            val asset = item as? Map<*, *> ?: continue
            val name = asset["name"]?.toString() ?: ""
            val downloadUrl = asset["browser_download_url"]?.toString() ?: ""

            if (name.endsWith(".apk", ignoreCase = true)) {
                apkDownloadUrl = downloadUrl
                apkFileName = name
                apkSizeBytes = (asset["size"] as? Number)?.toLong() ?: 0L
            } else if (name.endsWith(".sha256", ignoreCase = true) || name.endsWith(".sha256.txt", ignoreCase = true)) {
                checksumDownloadUrl = downloadUrl
            }
        }

        if (apkDownloadUrl.isNullOrBlank()) {
            // Release has no APK asset attached
            return null
        }

        val versionName = tagName.removePrefix("v").removePrefix("V").trim()
        val versionCode = parseVersionCode(versionName, releaseNotes)

        var expectedSha256: String? = null
        if (!checksumDownloadUrl.isNullOrBlank()) {
            expectedSha256 = fetchChecksumText(checksumDownloadUrl)
        }

        return UpdateInfo(
            tagName = tagName,
            versionName = versionName,
            versionCode = versionCode,
            releaseTitle = releaseTitle,
            releaseNotes = releaseNotes,
            publishedAt = publishedAt,
            isPrerelease = isPrerelease,
            apkDownloadUrl = apkDownloadUrl,
            apkFileName = if (apkFileName.isNotBlank()) apkFileName else "nuvio-$tagName.apk",
            apkSizeBytes = apkSizeBytes,
            checksumDownloadUrl = checksumDownloadUrl,
            expectedSha256 = expectedSha256
        )
    }

    private fun fetchChecksumText(url: String): String? {
        return runCatching {
            val req = Request.Builder().url(url).build()
            client.newCall(req).execute().use { res ->
                if (res.isSuccessful) res.body?.string()?.trim() else null
            }
        }.getOrNull()
    }

    companion object {
        /**
         * Parses version code from semantic version string (e.g. 1.2.3 -> 10203),
         * or parses an explicit "versionCode: 123" declaration in release notes.
         */
        fun parseVersionCode(versionName: String, releaseNotes: String = ""): Long {
            // Check for explicit versionCode in release notes
            val explicitMatch = Regex("versionCode\\s*[:=]\\s*(\\d+)", RegexOption.IGNORE_CASE)
                .find(releaseNotes)
            if (explicitMatch != null) {
                return explicitMatch.groupValues[1].toLongOrNull() ?: 1L
            }

            // Semantic versioning fallback: major * 10000 + minor * 100 + patch
            val parts = versionName.split(".").mapNotNull { it.filter { char -> char.isDigit() }.toLongOrNull() }
            return when (parts.size) {
                1 -> parts[0] * 10000L
                2 -> parts[0] * 10000L + parts[1] * 100L
                3 -> parts[0] * 10000L + parts[1] * 100L + parts[2]
                else -> 1L
            }
        }
    }
}
