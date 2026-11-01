package com.nuvio.feature.settings.screen

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nuvio.core.update.UpdateState
import com.nuvio.core.update.UpdateStatus
import com.nuvio.core.update.UpdateViewModel
import com.nuvio.feature.settings.SettingsViewModel
import com.nuvio.feature.settings.component.SettingsCard
import com.nuvio.feature.settings.component.SettingsSectionTitle
import com.nuvio.feature.settings.component.SettingsSwitchRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdatesSettingsScreen(
    viewModel: SettingsViewModel,
    updateViewModel: UpdateViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsState()
    val updateState by updateViewModel.updateState.collectAsState()
    val context = LocalContext.current

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("App Updates", fontWeight = FontWeight.SemiBold) },
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
                SettingsSectionTitle("Current Version Status")
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                    )
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Nuvio v${updateState.currentVersionName}",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Build ${updateState.currentVersionCode} • Production Release",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            when (updateState.status) {
                                UpdateStatus.CHECKING -> CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                UpdateStatus.UP_TO_DATE -> Icon(
                                    imageVector = Icons.Outlined.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                else -> Icon(
                                    imageVector = Icons.Outlined.SystemUpdate,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Status message or Action Button
                        when (updateState.status) {
                            UpdateStatus.CHECKING -> {
                                Text(
                                    text = "Connecting to GitHub Releases API...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            UpdateStatus.UPDATE_AVAILABLE -> {
                                val info = updateState.updateInfo
                                Text(
                                    text = "Update Available: v${info?.versionName ?: ""}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = updateViewModel::downloadUpdate,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Outlined.CloudDownload, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Download Update")
                                }
                            }
                            UpdateStatus.DOWNLOADING -> {
                                Text(
                                    text = "Downloading APK: ${updateState.downloadProgressPercent}%",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                LinearProgressIndicator(
                                    progress = { updateState.downloadProgressPercent / 100f },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            UpdateStatus.READY_TO_INSTALL -> {
                                Button(
                                    onClick = updateViewModel::installUpdate,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Install Update Now")
                                }
                            }
                            UpdateStatus.FAILED -> {
                                Text(
                                    text = updateState.errorMessage ?: "Update check failed",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = updateViewModel::checkForUpdates,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Outlined.Refresh, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Retry Check")
                                }
                            }
                            else -> {
                                OutlinedButton(
                                    onClick = updateViewModel::checkForUpdates,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Outlined.Refresh, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Check for Updates")
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(12.dp))
                SettingsSectionTitle("Update Preferences")
                SettingsCard {
                    SettingsSwitchRow(
                        title = "Automatic Update Checks",
                        subtitle = "Check GitHub releases periodically in background",
                        checked = settings.autoCheckUpdates,
                        onCheckedChange = viewModel::updateAutoCheckUpdates
                    )
                    SettingsSwitchRow(
                        title = "Download Updates on Wi-Fi Only",
                        subtitle = "Prevent APK package transfer over metered mobile data",
                        checked = settings.wifiOnlyUpdates,
                        icon = Icons.Outlined.Wifi,
                        onCheckedChange = viewModel::updateWifiOnlyUpdates
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
