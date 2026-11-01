package com.nuvio.core.update

import android.content.Context
import android.os.Build
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class UpdateRepositoryImpl(
    private val context: Context,
    private val releaseDataSource: GitHubReleaseDataSource,
    private val downloader: UpdateDownloader,
    private val installer: ApkInstaller
) : UpdateRepository {

    private val repositoryScope = CoroutineScope(Dispatchers.IO + Job())
    private var downloadJob: Job? = null

    private val currentVersionName: String
    private val currentVersionCode: Long

    init {
        val (name, code) = retrieveCurrentVersion(context)
        currentVersionName = name
        currentVersionCode = code
    }

    private val _updateState = MutableStateFlow(
        UpdateState(
            currentVersionName = currentVersionName,
            currentVersionCode = currentVersionCode
        )
    )
    override val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    override suspend fun checkForUpdates(manual: Boolean, includePrerelease: Boolean): Unit = withContext(Dispatchers.IO) {
        _updateState.value = _updateState.value.copy(
            status = UpdateStatus.CHECKING,
            errorMessage = null
        )

        val result = releaseDataSource.getLatestRelease(includePrerelease)

        result.onSuccess { updateInfo ->
            val now = System.currentTimeMillis()
            if (updateInfo == null) {
                _updateState.value = _updateState.value.copy(
                    status = UpdateStatus.UP_TO_DATE,
                    updateInfo = null,
                    lastCheckedTimestamp = now
                )
                return@onSuccess
            }

            // Compare versions
            val isNewer = updateInfo.versionCode > currentVersionCode

            _updateState.value = _updateState.value.copy(
                status = if (isNewer) UpdateStatus.UPDATE_AVAILABLE else UpdateStatus.UP_TO_DATE,
                updateInfo = updateInfo,
                showUpdateDialog = isNewer && manual,
                lastCheckedTimestamp = now
            )
        }.onFailure { error ->
            _updateState.value = _updateState.value.copy(
                status = UpdateStatus.FAILED,
                errorMessage = error.localizedMessage ?: "Failed to check for updates",
                lastCheckedTimestamp = System.currentTimeMillis()
            )
        }
        Unit
    }

    override suspend fun downloadUpdate(updateInfo: UpdateInfo) {
        downloadJob?.cancel()
        downloadJob = repositoryScope.launch {
            _updateState.value = _updateState.value.copy(
                status = UpdateStatus.DOWNLOADING,
                downloadProgressPercent = 0,
                downloadedBytes = 0L,
                errorMessage = null
            )

            val downloadResult = downloader.download(updateInfo) { downloaded, total, percent ->
                _updateState.value = _updateState.value.copy(
                    downloadedBytes = downloaded,
                    totalBytes = total,
                    downloadProgressPercent = percent
                )
            }

            downloadResult.onSuccess { apkFile ->
                _updateState.value = _updateState.value.copy(status = UpdateStatus.VERIFYING)

                // Verify SHA-256 integrity
                val isValid = ApkIntegrityVerifier.verify(apkFile, updateInfo.expectedSha256)
                if (isValid) {
                    _updateState.value = _updateState.value.copy(
                        status = UpdateStatus.READY_TO_INSTALL,
                        downloadedApkFile = apkFile
                    )
                } else {
                    apkFile.delete()
                    _updateState.value = _updateState.value.copy(
                        status = UpdateStatus.FAILED,
                        errorMessage = "APK integrity check failed (SHA-256 mismatch). File removed for safety."
                    )
                }
            }.onFailure { error ->
                if (error !is CancellationException) {
                    _updateState.value = _updateState.value.copy(
                        status = UpdateStatus.FAILED,
                        errorMessage = error.localizedMessage ?: "Download failed"
                    )
                }
            }
        }
    }

    override fun installDownloadedUpdate(): InstallationResult {
        val apkFile = _updateState.value.downloadedApkFile
            ?: return InstallationResult.Error("No downloaded APK file available to install")

        _updateState.value = _updateState.value.copy(status = UpdateStatus.INSTALLING)
        val result = installer.installApk(apkFile)

        if (result is InstallationResult.Error) {
            _updateState.value = _updateState.value.copy(
                status = UpdateStatus.FAILED,
                errorMessage = result.message
            )
        }
        return result
    }

    override fun dismissUpdateDialog() {
        _updateState.value = _updateState.value.copy(showUpdateDialog = false)
    }

    override fun cancelDownload() {
        downloadJob?.cancel()
        downloadJob = null
        downloader.cleanUpdates()
        _updateState.value = _updateState.value.copy(
            status = UpdateStatus.IDLE,
            downloadProgressPercent = 0,
            downloadedBytes = 0L
        )
    }

    companion object {
        fun retrieveCurrentVersion(context: Context): Pair<String, Long> {
            return runCatching {
                val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    context.packageManager.getPackageInfo(
                        context.packageName,
                        android.content.pm.PackageManager.PackageInfoFlags.of(0)
                    )
                } else {
                    @Suppress("DEPRECATION")
                    context.packageManager.getPackageInfo(context.packageName, 0)
                }
                val code = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    packageInfo.longVersionCode
                } else {
                    @Suppress("DEPRECATION")
                    packageInfo.versionCode.toLong()
                }
                Pair(packageInfo.versionName ?: "1.0.0", code)
            }.getOrDefault(Pair("1.0.0", 1L))
        }
    }
}
