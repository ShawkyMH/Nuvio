package com.nuvio.service.download

import android.app.Notification
import android.app.job.JobParameters
import android.app.job.JobService
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.nuvio.NuvioApp

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
class DownloadJobService : JobService() {

    override fun onStartJob(params: JobParameters?): Boolean {
        if (params == null) return false

        val notification = createNotification()
        setNotification(
            params,
            NOTIFICATION_ID,
            notification,
            JOB_END_NOTIFICATION_POLICY_DETACH
        )

        // Downloads are executed via NuvioApp.instance.container.downloadEngine
        return true
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        return true // Reschedule if constraints interrupted
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, NuvioApp.CHANNEL_DOWNLOADS)
            .setContentTitle("Nuvio Downloader")
            .setContentText("Transferring files...")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    companion object {
        const val JOB_ID = 1001
        const val NOTIFICATION_ID = 2001
    }
}
