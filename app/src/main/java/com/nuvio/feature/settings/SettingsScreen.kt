package com.nuvio.feature.settings

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.SdCard
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nuvio.core.designsystem.component.formatBytes
import com.nuvio.core.update.UpdateDialog
import com.nuvio.core.update.UpdateUiEvent
import com.nuvio.core.update.UpdateViewModel
import com.nuvio.feature.settings.component.SettingsCard
import com.nuvio.feature.settings.component.SettingsNavRow
import com.nuvio.feature.settings.component.SettingsSectionTitle
import com.nuvio.feature.settings.screen.AboutSettingsScreen
import com.nuvio.feature.settings.screen.AppearanceSettingsScreen
import com.nuvio.feature.settings.screen.BrowserSettingsScreen
import com.nuvio.feature.settings.screen.DownloadsSettingsScreen
import com.nuvio.feature.settings.screen.MediaSettingsScreen
import com.nuvio.feature.settings.screen.NotificationSettingsScreen
import com.nuvio.feature.settings.screen.PlayerSettingsScreen
import com.nuvio.feature.settings.screen.PrivacySettingsScreen
import com.nuvio.feature.settings.screen.StorageSettingsScreen
import com.nuvio.feature.settings.screen.UpdatesSettingsScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    updateViewModel: UpdateViewModel,
    onNavigateToLibrary: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val currentSubScreen by viewModel.currentSubScreen.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val storage by viewModel.storageMetrics.collectAsState()
    val updateState by updateViewModel.updateState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        updateViewModel.events.collect { event ->
            when (event) {
                is UpdateUiEvent.ShowToast -> Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                is UpdateUiEvent.RequirePermission -> context.startActivity(event.intent)
            }
        }
    }

    // Intercept back navigation when on a sub-screen
    BackHandler(enabled = currentSubScreen != null) {
        viewModel.navigateToSubScreen(null)
    }

    when (currentSubScreen) {
        SettingsSubScreen.DOWNLOADS -> {
            DownloadsSettingsScreen(
                viewModel = viewModel,
                onBack = { viewModel.navigateToSubScreen(null) }
            )
        }
        SettingsSubScreen.MEDIA -> {
            MediaSettingsScreen(
                viewModel = viewModel,
                onBack = { viewModel.navigateToSubScreen(null) }
            )
        }
        SettingsSubScreen.STORAGE -> {
            StorageSettingsScreen(
                viewModel = viewModel,
                onBack = { viewModel.navigateToSubScreen(null) },
                onNavigateToLibrary = {
                    viewModel.navigateToSubScreen(null)
                    onNavigateToLibrary?.invoke()
                }
            )
        }
        SettingsSubScreen.NOTIFICATIONS -> {
            NotificationSettingsScreen(
                viewModel = viewModel,
                onBack = { viewModel.navigateToSubScreen(null) }
            )
        }
        SettingsSubScreen.BROWSER -> {
            BrowserSettingsScreen(
                viewModel = viewModel,
                onBack = { viewModel.navigateToSubScreen(null) }
            )
        }
        SettingsSubScreen.PLAYER -> {
            PlayerSettingsScreen(
                viewModel = viewModel,
                onBack = { viewModel.navigateToSubScreen(null) }
            )
        }
        SettingsSubScreen.APPEARANCE -> {
            AppearanceSettingsScreen(
                viewModel = viewModel,
                onBack = { viewModel.navigateToSubScreen(null) }
            )
        }
        SettingsSubScreen.PRIVACY -> {
            PrivacySettingsScreen(
                viewModel = viewModel,
                onBack = { viewModel.navigateToSubScreen(null) }
            )
        }
        SettingsSubScreen.UPDATES -> {
            UpdatesSettingsScreen(
                viewModel = viewModel,
                updateViewModel = updateViewModel,
                onBack = { viewModel.navigateToSubScreen(null) }
            )
        }
        SettingsSubScreen.ABOUT -> {
            AboutSettingsScreen(
                updateViewModel = updateViewModel,
                onBack = { viewModel.navigateToSubScreen(null) }
            )
        }
        null -> {
            // Main Settings Hub
            Scaffold(
                modifier = modifier.fillMaxSize(),
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                text = "Settings",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background
                        )
                    )
                },
                containerColor = MaterialTheme.colorScheme.background
            ) { innerPadding ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    item {
                        SettingsSectionTitle("Engine & Operations")
                        SettingsCard {
                            SettingsNavRow(
                                title = "Downloads",
                                subtitle = "Concurrent tasks, connections, Wi-Fi only, retries",
                                icon = Icons.Outlined.Download,
                                statusText = "${settings.concurrentDownloads} active • ${settings.connectionsPerDownload} threads",
                                onClick = { viewModel.navigateToSubScreen(SettingsSubScreen.DOWNLOADS) }
                            )
                            SettingsNavRow(
                                title = "Video & Audio",
                                subtitle = "Preferred resolutions, formats, subtitles, bitrates",
                                icon = Icons.Outlined.VideoLibrary,
                                statusText = "${settings.preferredVideoQuality} • ${settings.defaultVideoFormat.uppercase()}",
                                onClick = { viewModel.navigateToSubScreen(SettingsSubScreen.MEDIA) }
                            )
                            SettingsNavRow(
                                title = "Storage",
                                subtitle = "Download folders, cache size, cleanup staging files",
                                icon = Icons.Outlined.SdCard,
                                statusText = "${formatBytes(storage.downloadsSizeBytes)} used",
                                onClick = { viewModel.navigateToSubScreen(SettingsSubScreen.STORAGE) }
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(12.dp))
                        SettingsSectionTitle("Experience & Content")
                        SettingsCard {
                            SettingsNavRow(
                                title = "Browser",
                                subtitle = "Search engine, homepage, desktop mode, media detector",
                                icon = Icons.Outlined.Language,
                                statusText = settings.searchEngine,
                                onClick = { viewModel.navigateToSubScreen(SettingsSubScreen.BROWSER) }
                            )
                            SettingsNavRow(
                                title = "Media Player",
                                subtitle = "Remember position, playback speed, background audio",
                                icon = Icons.Outlined.PlayCircle,
                                statusText = "${settings.defaultPlaybackSpeed}x",
                                onClick = { viewModel.navigateToSubScreen(SettingsSubScreen.PLAYER) }
                            )
                            SettingsNavRow(
                                title = "Notifications",
                                subtitle = "Transfer alerts, progress indicators, update prompts",
                                icon = Icons.Outlined.Notifications,
                                onClick = { viewModel.navigateToSubScreen(SettingsSubScreen.NOTIFICATIONS) }
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(12.dp))
                        SettingsSectionTitle("System & Device")
                        SettingsCard {
                            val themeName = when (settings.darkMode) {
                                null -> "System Default"
                                true -> "Dark"
                                false -> "Light"
                            }
                            SettingsNavRow(
                                title = "Appearance",
                                subtitle = "Theme, dynamic colors, typography, language",
                                icon = Icons.Outlined.ColorLens,
                                statusText = themeName,
                                onClick = { viewModel.navigateToSubScreen(SettingsSubScreen.APPEARANCE) }
                            )
                            SettingsNavRow(
                                title = "Privacy & Security",
                                subtitle = "Local-only storage, history management, zero telemetry",
                                icon = Icons.Outlined.Lock,
                                onClick = { viewModel.navigateToSubScreen(SettingsSubScreen.PRIVACY) }
                            )
                            SettingsNavRow(
                                title = "App Updates",
                                subtitle = "Check for updates, release notes, SHA-256 verification",
                                icon = Icons.Outlined.SystemUpdate,
                                statusText = "v${updateState.currentVersionName}",
                                onClick = { viewModel.navigateToSubScreen(SettingsSubScreen.UPDATES) }
                            )
                            SettingsNavRow(
                                title = "About Nuvio",
                                subtitle = "Version details, open source licenses, team charter",
                                icon = Icons.Outlined.Info,
                                onClick = { viewModel.navigateToSubScreen(SettingsSubScreen.ABOUT) }
                            )
                        }
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }
    }

    if (updateState.showUpdateDialog && updateState.updateInfo != null) {
        UpdateDialog(
            state = updateState,
            onDownloadClick = updateViewModel::downloadUpdate,
            onInstallClick = updateViewModel::installUpdate,
            onCancelClick = updateViewModel::cancelDownload,
            onDismiss = updateViewModel::dismissDialog
        )
    }
}
