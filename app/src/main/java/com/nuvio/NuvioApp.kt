package com.nuvio

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.nuvio.di.AppContainer

class NuvioApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        container = AppContainer(this)
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)

            // Download progress channel
            val downloadChannel = NotificationChannel(
                CHANNEL_DOWNLOADS,
                "Downloads",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows active download progress and completion notifications"
                setShowBadge(false)
            }

            // Media playback channel
            val playbackChannel = NotificationChannel(
                CHANNEL_PLAYBACK,
                "Media Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows ongoing media playback controls"
                setShowBadge(false)
            }

            notificationManager.createNotificationChannels(listOf(downloadChannel, playbackChannel))
        }
    }

    companion object {
        const val CHANNEL_DOWNLOADS = "nuvio_downloads"
        const val CHANNEL_PLAYBACK = "nuvio_playback"

        lateinit var instance: NuvioApp
            private set
    }
}
