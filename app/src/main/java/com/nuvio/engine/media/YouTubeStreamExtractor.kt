package com.nuvio.engine.media

import com.nuvio.domain.model.FormatOption
import com.nuvio.domain.model.FormatType
import com.nuvio.domain.model.MediaMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class YouTubeStreamExtractor(
    private val client: OkHttpClient
) {

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    companion object {
        private val YT_VIDEO_ID_REGEX = Regex(
            """(?:v=|vi=|\/v\/|\/vi\/|\/embed\/|\/shorts\/|\/live\/|youtu\.be\/|\/watch\?v=|\/watch\?vi=)([a-zA-Z0-9_-]{11})"""
        )

        fun isYouTubeUrl(url: String): Boolean {
            val lower = url.lowercase()
            return lower.contains("youtube.com") || lower.contains("youtu.be")
        }

        fun extractVideoId(url: String): String? {
            val trimmed = url.trim()
            if (trimmed.length == 11 && trimmed.matches(Regex("""^[a-zA-Z0-9_-]{11}$"""))) {
                return trimmed
            }
            val match = YT_VIDEO_ID_REGEX.find(trimmed)
            return match?.groupValues?.get(1)
        }
    }

    suspend fun extract(url: String): Result<MediaMetadata> = withContext(Dispatchers.IO) {
        runCatching {
            val videoId = extractVideoId(url)
                ?: throw IllegalArgumentException("Could not extract a valid YouTube video ID from URL: $url")

            // 1. Query Android VR InnerTube client for direct stream URLs
            val requestBodyJson = JSONObject().apply {
                put("videoId", videoId)
                put("context", JSONObject().apply {
                    put("client", JSONObject().apply {
                        put("clientName", "ANDROID_VR")
                        put("clientVersion", "1.56.21")
                        put("deviceMake", "Oculus")
                        put("deviceModel", "Quest 3")
                        put("hl", "en")
                        put("gl", "US")
                    })
                })
            }

            val request = Request.Builder()
                .url("https://www.youtube.com/youtubei/v1/player?prettyPrint=false")
                .post(requestBodyJson.toString().toRequestBody(jsonMediaType))
                .header("User-Agent", "com.google.android.youtube/19.09.37 (Linux; U; Android 14; en_US) gzip")
                .header("Accept", "application/json")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                throw IllegalStateException("YouTube API returned HTTP ${response.code}")
            }

            val bodyString = response.body?.string()
                ?: throw IllegalStateException("Empty response from YouTube API")

            val json = JSONObject(bodyString)

            // Check playability status
            val playability = json.optJSONObject("playabilityStatus")
            val status = playability?.optString("status", "OK") ?: "OK"
            if (status != "OK") {
                val reason = playability?.optString("reason", "Video is unavailable or restricted")
                throw IllegalStateException("Video unavailable: $reason")
            }

            // Extract videoDetails
            val details = json.optJSONObject("videoDetails")
                ?: throw IllegalStateException("Missing videoDetails in YouTube response")
            val title = details.optString("title", "YouTube Video")
            val author = details.optString("author", "YouTube Channel")
            val durationSeconds = details.optLong("lengthSeconds", 0L)

            // Extract thumbnail
            var thumbnailUrl: String? = null
            val thumbnailObj = details.optJSONObject("thumbnail")
            val thumbnailsArray = thumbnailObj?.optJSONArray("thumbnails")
            if (thumbnailsArray != null && thumbnailsArray.length() > 0) {
                thumbnailUrl = thumbnailsArray.getJSONObject(thumbnailsArray.length() - 1).optString("url")
            }

            val formats = mutableListOf<FormatOption>()
            val streamingData = json.optJSONObject("streamingData")

            if (streamingData != null) {
                // 1. Muxed formats (Video + Audio combined)
                val muxedFormats = streamingData.optJSONArray("formats")
                if (muxedFormats != null) {
                    for (i in 0 until muxedFormats.length()) {
                        val f = muxedFormats.getJSONObject(i)
                        val streamUrl = f.optString("url", "")
                        if (streamUrl.isNotBlank()) {
                            val itag = f.optInt("itag", 0)
                            val qualityLabel = f.optString("qualityLabel", "Standard")
                            val mimeType = f.optString("mimeType", "video/mp4")
                            val contentLength = f.optLong("contentLength", -1L)
                            val bitrate = f.optInt("bitrate", 0)
                            val container = if (mimeType.contains("webm")) "webm" else "mp4"

                            formats.add(
                                FormatOption(
                                    id = "muxed_$itag",
                                    label = "Video & Audio — $qualityLabel (${container.uppercase()})",
                                    formatType = FormatType.VIDEO,
                                    resolution = qualityLabel,
                                    bitrateKbps = bitrate / 1000,
                                    container = container,
                                    estimatedBytes = contentLength,
                                    isDirectStream = true,
                                    streamUrl = streamUrl
                                )
                            )
                        }
                    }
                }

                // 2. Adaptive formats (Separate Audio and HD Video)
                val adaptiveFormats = streamingData.optJSONArray("adaptiveFormats")
                if (adaptiveFormats != null) {
                    // Extract Audio streams
                    for (i in 0 until adaptiveFormats.length()) {
                        val f = adaptiveFormats.getJSONObject(i)
                        val streamUrl = f.optString("url", "")
                        val mimeType = f.optString("mimeType", "")
                        if (streamUrl.isNotBlank() && mimeType.startsWith("audio/")) {
                            val itag = f.optInt("itag", 0)
                            val contentLength = f.optLong("contentLength", -1L)
                            val bitrate = f.optInt("bitrate", 0)

                            if (mimeType.contains("mp4a")) {
                                formats.add(
                                    FormatOption(
                                        id = "audio_$itag",
                                        label = "Audio Only — M4A / AAC (~${bitrate / 1000} kbps)",
                                        formatType = FormatType.AUDIO,
                                        resolution = null,
                                        bitrateKbps = bitrate / 1000,
                                        container = "m4a",
                                        estimatedBytes = contentLength,
                                        isDirectStream = true,
                                        streamUrl = streamUrl
                                    )
                                )
                            } else if (mimeType.contains("opus")) {
                                formats.add(
                                    FormatOption(
                                        id = "audio_$itag",
                                        label = "Audio Only — Opus / WebM (~${bitrate / 1000} kbps)",
                                        formatType = FormatType.AUDIO,
                                        resolution = null,
                                        bitrateKbps = bitrate / 1000,
                                        container = "webm",
                                        estimatedBytes = contentLength,
                                        isDirectStream = true,
                                        streamUrl = streamUrl
                                    )
                                )
                            }
                        }
                    }

                    // Extract High Definition Video streams (1080p, 720p, 480p)
                    for (i in 0 until adaptiveFormats.length()) {
                        val f = adaptiveFormats.getJSONObject(i)
                        val streamUrl = f.optString("url", "")
                        val mimeType = f.optString("mimeType", "")
                        if (streamUrl.isNotBlank() && mimeType.startsWith("video/")) {
                            val itag = f.optInt("itag", 0)
                            val qualityLabel = f.optString("qualityLabel", "")
                            val contentLength = f.optLong("contentLength", -1L)
                            val bitrate = f.optInt("bitrate", 0)
                            val container = if (mimeType.contains("webm")) "webm" else "mp4"

                            // Include standard MP4 video streams
                            if (container == "mp4" && qualityLabel.isNotBlank()) {
                                formats.add(
                                    FormatOption(
                                        id = "video_$itag",
                                        label = "Video Stream — $qualityLabel (MP4)",
                                        formatType = FormatType.VIDEO,
                                        resolution = qualityLabel,
                                        bitrateKbps = bitrate / 1000,
                                        container = container,
                                        estimatedBytes = contentLength,
                                        isDirectStream = true,
                                        streamUrl = streamUrl
                                    )
                                )
                            }
                        }
                    }
                }
            }

            if (formats.isEmpty()) {
                throw IllegalStateException("No direct playable stream formats could be extracted for this video")
            }

            MediaMetadata(
                url = url,
                title = title,
                thumbnailUrl = thumbnailUrl,
                durationSeconds = durationSeconds,
                sourceName = "YouTube ($author)",
                formats = formats
            )
        }
    }
}
