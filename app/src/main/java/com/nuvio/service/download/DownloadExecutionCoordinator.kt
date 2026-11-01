package com.nuvio.service.download

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import androidx.core.content.ContextCompat

class DownloadExecutionCoordinator(private val context: Context) {

    fun ensureTransferExecution(wifiOnly: Boolean = false) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // Android 14+ User-Initiated Data Transfer Job
            val scheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as? JobScheduler ?: return

            val networkBuilder = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)

            if (wifiOnly) {
                networkBuilder.addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
            }

            val jobInfo = JobInfo.Builder(
                DownloadJobService.JOB_ID,
                ComponentName(context, DownloadJobService::class.java)
            )
                .setUserInitiated(true)
                .setRequiredNetwork(networkBuilder.build())
                .build()

            scheduler.schedule(jobInfo)
        } else {
            // Android 8-13 Foreground Service
            val intent = Intent(context, DownloadForegroundService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }
    }

    fun stopTransferExecution() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val scheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as? JobScheduler
            scheduler?.cancel(DownloadJobService.JOB_ID)
        } else {
            val intent = Intent(context, DownloadForegroundService::class.java)
            context.stopService(intent)
        }
    }
}
