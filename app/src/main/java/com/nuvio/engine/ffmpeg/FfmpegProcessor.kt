package com.nuvio.engine.ffmpeg

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class FfmpegProcessor {

    /**
     * Muxes separate video and audio streams using stream copy without transcoding.
     */
    suspend fun muxStreams(
        videoFile: File,
        audioFile: File,
        outputFile: File
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val command = listOf(
                "ffmpeg",
                "-y",
                "-i", videoFile.absolutePath,
                "-i", audioFile.absolutePath,
                "-c", "copy",
                outputFile.absolutePath
            )
            val process = ProcessBuilder(command)
                .redirectErrorStream(true)
                .start()
            val code = process.waitFor()
            if (code != 0) {
                throw RuntimeException("FFmpeg muxing failed with code $code")
            }
        }
    }

    /**
     * Extracts audio stream to MP3 or AAC.
     */
    suspend fun extractAudio(
        videoFile: File,
        outputAudioFile: File,
        codec: String = "libmp3lame",
        bitrateKbps: Int = 320
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val command = listOf(
                "ffmpeg",
                "-y",
                "-i", videoFile.absolutePath,
                "-vn",
                "-c:a", codec,
                "-b:a", "${bitrateKbps}k",
                outputAudioFile.absolutePath
            )
            val process = ProcessBuilder(command)
                .redirectErrorStream(true)
                .start()
            val code = process.waitFor()
            if (code != 0) {
                throw RuntimeException("FFmpeg audio extraction failed with code $code")
            }
        }
    }
}
