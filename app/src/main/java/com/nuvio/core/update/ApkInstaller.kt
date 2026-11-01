package com.nuvio.core.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import java.io.File

sealed interface InstallationResult {
    object Launched : InstallationResult
    data class PermissionNeeded(val settingsIntent: Intent) : InstallationResult
    data class Error(val message: String) : InstallationResult
}

class ApkInstaller(private val context: Context) {

    /**
     * Checks if the app is authorized to request package installation.
     */
    fun canInstallPackages(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    /**
     * Initiates the standard Android package installation UI for the given APK.
     */
    fun installApk(apkFile: File): InstallationResult {
        if (!apkFile.exists()) {
            return InstallationResult.Error("APK file not found: ${apkFile.absolutePath}")
        }

        // Check unknown sources installation permission on Android 8.0+ (API 26+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !canInstallPackages()) {
            val settingsIntent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            return InstallationResult.PermissionNeeded(settingsIntent)
        }

        return try {
            val authority = "${context.packageName}.fileprovider"
            val contentUri = FileProvider.getUriForFile(context, authority, apkFile)

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(installIntent)
            InstallationResult.Launched
        } catch (e: Exception) {
            InstallationResult.Error("Failed to launch package installer: ${e.localizedMessage ?: "Unknown error"}")
        }
    }
}
