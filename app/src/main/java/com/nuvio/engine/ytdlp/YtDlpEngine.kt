package com.nuvio.engine.ytdlp

import com.nuvio.core.network.UrlValidator
import com.nuvio.domain.model.FormatOption
import com.nuvio.domain.model.FormatType
import com.nuvio.domain.model.MediaMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class YtDlpEngine {

    suspend fun inspectUrl(url: String): Result<MediaMetadata> = withContext(Dispatchers.IO) {
        runCatching {
            if (!UrlValidator.isValidUrl(url)) {
                throw IllegalArgumentException("Invalid URL provided")
            }

            val fileName = UrlValidator.extractFileNameFromUrl(url)
            val lower = url.lowercase()

            val formats = mutableListOf<FormatOption>()
            val sourceName = when {
                lower.contains("youtube.com") || lower.contains("youtu.be") -> "YouTube"
                lower.contains("vimeo.com") -> "Vimeo"
                lower.contains("tiktok.com") -> "TikTok"
                lower.contains("instagram.com") -> "Instagram"
                lower.contains("twitter.com") || lower.contains("x.com") -> "X / Twitter"
                else -> "Direct Media"
            }

            // Standard format representations
            formats.add(
                FormatOption(
                    id = "best_video",
                    label = "Best Video (1080p Full HD)",
                    formatType = FormatType.VIDEO,
                    resolution = "1920x1080",
                    bitrateKbps = 4500,
                    container = "mp4",
                    estimatedBytes = -1L
                )
            )
            formats.add(
                FormatOption(
                    id = "720p_video",
                    label = "High Definition (720p HD)",
                    formatType = FormatType.VIDEO,
                    resolution = "1280x720",
                    bitrateKbps = 2500,
                    container = "mp4",
                    estimatedBytes = -1L
                )
            )
            formats.add(
                FormatOption(
                    id = "480p_video",
                    label = "Standard Definition (480p)",
                    formatType = FormatType.VIDEO,
                    resolution = "854x480",
                    bitrateKbps = 1200,
                    container = "mp4",
                    estimatedBytes = -1L
                )
            )
            formats.add(
                FormatOption(
                    id = "audio_mp3_320",
                    label = "Audio Only — MP3 (320 kbps High Quality)",
                    formatType = FormatType.AUDIO,
                    resolution = null,
                    bitrateKbps = 320,
                    container = "mp3",
                    estimatedBytes = -1L
                )
            )
            formats.add(
                FormatOption(
                    id = "audio_m4a_128",
                    label = "Audio Only — M4A (128 kbps Standard)",
                    formatType = FormatType.AUDIO,
                    resolution = null,
                    bitrateKbps = 128,
                    container = "m4a",
                    estimatedBytes = -1L
                )
            )

            val displayTitle = if (fileName != "download") fileName else "Media Stream ($sourceName)"

            MediaMetadata(
                url = url,
                title = displayTitle,
                thumbnailUrl = null,
                durationSeconds = 0L,
                sourceName = sourceName,
                formats = formats
            )
        }
    }

    /**
     * Executes yt-dlp or equivalent extractor safely via ProcessBuilder.
     * Prevents shell injection by passing discrete tokenized arguments.
     */
    suspend fun executeExtraction(
        binaryPath: String,
        arguments: List<String>,
        workingDir: File
    ): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val command = mutableListOf(binaryPath)
            command.addAll(arguments)

            val process = ProcessBuilder(command)
                .directory(workingDir)
                .redirectErrorStream(true)
                .start()

            val exitCode = process.waitFor()
            if (exitCode != 0) {
                val errorOutput = process.inputStream.bufferedReader().readText()
                throw RuntimeException("Extraction failed with code $exitCode: $errorOutput")
            }
            exitCode
        }
    }
}
