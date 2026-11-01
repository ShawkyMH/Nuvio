package com.nuvio.core.update

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import java.io.File
import java.security.MessageDigest

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
     * Validates package archive compatibility (package name and certificate match)
     * before delegating to the Android package installer.
     */
    fun validateApkCompatibility(apkFile: File): Result<Unit> {
        val pm = context.packageManager
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            @Suppress("DEPRECATION")
            PackageManager.GET_SIGNATURES
        }

        val archiveInfo = pm.getPackageArchiveInfo(apkFile.absolutePath, flags)
            ?: return Result.failure(IllegalStateException("Invalid or corrupted APK package file"))

        if (archiveInfo.packageName != context.packageName) {
            return Result.failure(
                IllegalStateException("Package mismatch: update is ${archiveInfo.packageName}, expected ${context.packageName}")
            )
        }

        // Compare signing certificates if available
        val apkCertDigest = getArchiveCertificateSha256(archiveInfo)
        val installedCertDigest = getInstalledAppCertificateSha256()

        if (apkCertDigest != null && installedCertDigest != null && !apkCertDigest.equals(installedCertDigest, ignoreCase = true)) {
            return Result.failure(
                IllegalStateException(
                    "Signing certificate conflict: installed app ($installedCertDigest) vs update ($apkCertDigest). In-place update requires the same signing key."
                )
            )
        }

        return Result.success(Unit)
    }

    private fun getArchiveCertificateSha256(archiveInfo: android.content.pm.PackageInfo): String? {
        return runCatching {
            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                archiveInfo.signingInfo?.apkContentsSigners
            } else {
                @Suppress("DEPRECATION")
                archiveInfo.signatures
            }
            val firstSig = signatures?.firstOrNull() ?: return null
            sha256Hex(firstSig.toByteArray())
        }.getOrNull()
    }

    private fun getInstalledAppCertificateSha256(): String? {
        return runCatching {
            val pm = context.packageManager
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                PackageManager.GET_SIGNING_CERTIFICATES
            } else {
                @Suppress("DEPRECATION")
                PackageManager.GET_SIGNATURES
            }
            val info = pm.getPackageInfo(context.packageName, flags)
            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                info.signingInfo?.apkContentsSigners
            } else {
                @Suppress("DEPRECATION")
                info.signatures
            }
            val firstSig = signatures?.firstOrNull() ?: return null
            sha256Hex(firstSig.toByteArray())
        }.getOrNull()
    }

    private fun sha256Hex(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }

    /**
     * Initiates the standard Android package installation UI for the given APK.
     */
    fun installApk(apkFile: File): InstallationResult {
        if (!apkFile.exists()) {
            return InstallationResult.Error("APK file not found: ${apkFile.absolutePath}")
        }

        val compatibility = validateApkCompatibility(apkFile)
        if (compatibility.isFailure) {
            return InstallationResult.Error(compatibility.exceptionOrNull()?.message ?: "Incompatible APK package")
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
