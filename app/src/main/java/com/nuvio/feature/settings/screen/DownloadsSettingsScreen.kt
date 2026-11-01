package com.nuvio.feature.settings.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.NetworkCheck
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nuvio.feature.settings.SettingsViewModel
import com.nuvio.feature.settings.component.SettingsCard
import com.nuvio.feature.settings.component.SettingsSectionTitle
import com.nuvio.feature.settings.component.SettingsSwitchRow
import com.nuvio.feature.settings.component.SettingsValueRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsSettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsState()
    var showConcurrentDialog by remember { mutableStateOf(false) }
    var showConnectionsDialog by remember { mutableStateOf(false) }
    var showRetriesDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Download Settings", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
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
                SettingsSectionTitle("Concurrency & Connections")
                SettingsCard {
                    SettingsValueRow(
                        title = "Max Simultaneous Downloads",
                        subtitle = "Number of active downloads running in parallel",
                        value = "${settings.concurrentDownloads}",
                        icon = Icons.Outlined.CloudDownload,
                        onClick = { showConcurrentDialog = true }
                    )
                    SettingsValueRow(
                        title = "Connections per Download",
                        subtitle = "Multi-threaded segmented range connections",
                        value = "${settings.connectionsPerDownload} threads",
                        icon = Icons.Outlined.NetworkCheck,
                        onClick = { showConnectionsDialog = true }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(12.dp))
                SettingsSectionTitle("Network & Connectivity")
                SettingsCard {
                    SettingsSwitchRow(
                        title = "Download over Wi-Fi Only",
                        subtitle = "Prevent downloading over cellular/metered data",
                        checked = settings.wifiOnly,
                        icon = Icons.Outlined.Wifi,
                        onCheckedChange = viewModel::updateWifiOnly
                    )
                    SettingsSwitchRow(
                        title = "Auto-Resume on Wi-Fi",
                        subtitle = "Automatically resume interrupted downloads when connected to Wi-Fi",
                        checked = settings.autoResumeOnWifi,
                        onCheckedChange = viewModel::updateAutoResume
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(12.dp))
                SettingsSectionTitle("Error Recovery & Retry")
                SettingsCard {
                    SettingsSwitchRow(
                        title = "Retry Failed Downloads",
                        subtitle = "Automatically retry on temporary connection drops",
                        checked = settings.retryFailed,
                        icon = Icons.Outlined.Refresh,
                        onCheckedChange = viewModel::updateRetryFailed
                    )
                    if (settings.retryFailed) {
                        SettingsValueRow(
                            title = "Maximum Retry Count",
                            subtitle = "Consecutive retry attempts before marking failed",
                            value = "${settings.maxRetries} times",
                            onClick = { showRetriesDialog = true }
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(12.dp))
                SettingsSectionTitle("Queue & File Management")
                SettingsCard {
                    SettingsSwitchRow(
                        title = "Confirm Before Starting",
                        subtitle = "Always prompt for format and details before queueing",
                        checked = settings.askBeforeDownload,
                        onCheckedChange = viewModel::updateAskBeforeDownload
                    )
                    SettingsSwitchRow(
                        title = "Keep Download History",
                        subtitle = "Retain records of completed downloads in library",
                        checked = settings.keepDownloadHistory,
                        icon = Icons.Outlined.History,
                        onCheckedChange = viewModel::updateKeepHistory
                    )
                    SettingsSwitchRow(
                        title = "Prevent Duplicate Filenames",
                        subtitle = "Append index counter if a file with same name exists",
                        checked = settings.preventDuplicateFilenames,
                        onCheckedChange = viewModel::updatePreventDuplicates
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showConcurrentDialog) {
        NumberPickerDialog(
            title = "Max Simultaneous Downloads",
            options = listOf(1, 2, 3, 4, 5, 6),
            selected = settings.concurrentDownloads,
            onSelect = {
                viewModel.updateConcurrentDownloads(it)
                showConcurrentDialog = false
            },
            onDismiss = { showConcurrentDialog = false }
        )
    }

    if (showConnectionsDialog) {
        NumberPickerDialog(
            title = "Connections per Download",
            options = listOf(1, 2, 4, 6, 8),
            selected = settings.connectionsPerDownload,
            onSelect = {
                viewModel.updateConnectionsPerDownload(it)
                showConnectionsDialog = false
            },
            onDismiss = { showConnectionsDialog = false }
        )
    }

    if (showRetriesDialog) {
        NumberPickerDialog(
            title = "Maximum Retry Count",
            options = listOf(1, 2, 3, 5, 10),
            selected = settings.maxRetries,
            onSelect = {
                viewModel.updateMaxRetries(it)
                showRetriesDialog = false
            },
            onDismiss = { showRetriesDialog = false }
        )
    }
}

@Composable
fun NumberPickerDialog(
    title: String,
    options: List<Int>,
    selected: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                options.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(option) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = option == selected,
                            onClick = { onSelect(option) }
                        )
                        Text(
                            text = "$option",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
