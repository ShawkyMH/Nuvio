package com.nuvio.service.playback

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.nuvio.NuvioApp

class MediaPlaybackService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val title = intent?.getStringExtra("EXTRA_TITLE") ?: "Nuvio Media Player"
        val artist = intent?.getStringExtra("EXTRA_ARTIST") ?: "Playing audio"

        val notification = createNotification(title, artist)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        return START_NOT_STICKY
    }

    private fun createNotification(title: String, artist: String): Notification {
        return NotificationCompat.Builder(this, NuvioApp.CHANNEL_PLAYBACK)
            .setContentTitle(title)
            .setContentText(artist)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOngoing(true)
            .build()
    }

    companion object {
        const val NOTIFICATION_ID = 3001
    }
}
