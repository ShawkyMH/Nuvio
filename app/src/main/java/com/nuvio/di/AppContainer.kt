package com.nuvio.di

import android.content.Context
import com.nuvio.core.database.DownloadSegmentDao
import com.nuvio.core.database.DownloadTaskDao
import com.nuvio.core.database.NuvioDatabaseHelper
import com.nuvio.core.database.PlaybackHistoryDao
import com.nuvio.core.network.NetworkClientProvider
import com.nuvio.core.storage.FileManager
import com.nuvio.data.repository.DownloadRepositoryImpl
import com.nuvio.data.repository.MediaRepositoryImpl
import com.nuvio.data.repository.PlaybackRepositoryImpl
import com.nuvio.data.repository.SettingsRepositoryImpl
import com.nuvio.domain.repository.DownloadRepository
import com.nuvio.domain.repository.MediaRepository
import com.nuvio.domain.repository.PlaybackRepository
import com.nuvio.domain.repository.SettingsRepository
import com.nuvio.engine.ffmpeg.FfmpegProcessor
import com.nuvio.engine.http.HttpDownloadEngine
import com.nuvio.engine.http.HttpRangeProber
import com.nuvio.engine.ytdlp.YtDlpEngine

class AppContainer(private val context: Context) {

    val dbHelper: NuvioDatabaseHelper by lazy {
        NuvioDatabaseHelper(context)
    }

    val taskDao: DownloadTaskDao by lazy {
        DownloadTaskDao(dbHelper)
    }

    val segmentDao: DownloadSegmentDao by lazy {
        DownloadSegmentDao(dbHelper)
    }

    val playbackHistoryDao: PlaybackHistoryDao by lazy {
        PlaybackHistoryDao(dbHelper)
    }

    val fileManager: FileManager by lazy {
        FileManager(context)
    }

    val okHttpClient by lazy {
        NetworkClientProvider.createClient()
    }

    val httpRangeProber: HttpRangeProber by lazy {
        HttpRangeProber(okHttpClient)
    }

    val downloadEngine: HttpDownloadEngine by lazy {
        HttpDownloadEngine(
            client = okHttpClient,
            prober = httpRangeProber,
            taskDao = taskDao,
            segmentDao = segmentDao,
            fileManager = fileManager
        )
    }

    val ytDlpEngine: YtDlpEngine by lazy {
        YtDlpEngine()
    }

    val ffmpegProcessor: FfmpegProcessor by lazy {
        FfmpegProcessor()
    }

    val downloadRepository: DownloadRepository by lazy {
        DownloadRepositoryImpl(
            taskDao = taskDao,
            segmentDao = segmentDao,
            downloadEngine = downloadEngine,
            fileManager = fileManager
        )
    }

    val playbackRepository: PlaybackRepository by lazy {
        PlaybackRepositoryImpl(playbackHistoryDao)
    }

    val settingsRepository: SettingsRepository by lazy {
        SettingsRepositoryImpl(context)
    }

    val mediaRepository: MediaRepository by lazy {
        MediaRepositoryImpl(ytDlpEngine)
    }
}
