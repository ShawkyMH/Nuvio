package com.nuvio.feature.settings.screen

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nuvio.feature.settings.SettingsViewModel
import com.nuvio.feature.settings.component.SettingsCard
import com.nuvio.feature.settings.component.SettingsSectionTitle
import com.nuvio.feature.settings.component.SettingsSwitchRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Notification Settings", fontWeight = FontWeight.SemiBold) },
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
                SettingsSectionTitle("Download Status Alerts")
                SettingsCard {
                    SettingsSwitchRow(
                        title = "Download Started",
                        subtitle = "Alert when a transfer begins in foreground/background",
                        checked = settings.notifyStarted,
                        icon = Icons.Outlined.FileDownload,
                        onCheckedChange = viewModel::updateNotificationStarted
                    )
                    SettingsSwitchRow(
                        title = "Transfer Progress Updates",
                        subtitle = "Continuous progress bar in system notification shade",
                        checked = settings.notifyProgress,
                        icon = Icons.Outlined.Sync,
                        onCheckedChange = viewModel::updateNotificationProgress
                    )
                    SettingsSwitchRow(
                        title = "Download Completed",
                        subtitle = "Notify when downloads pass file integrity validation",
                        checked = settings.notifyCompleted,
                        icon = Icons.Outlined.CheckCircle,
                        onCheckedChange = viewModel::updateNotificationCompleted
                    )
                    SettingsSwitchRow(
                        title = "Download Failed",
                        subtitle = "Notify with cause when an unrecoverable failure occurs",
                        checked = settings.notifyFailed,
                        icon = Icons.Outlined.ErrorOutline,
                        onCheckedChange = viewModel::updateNotificationFailed
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(12.dp))
                SettingsSectionTitle("App Updates")
                SettingsCard {
                    SettingsSwitchRow(
                        title = "New Version Available",
                        subtitle = "Notify when a new verified GitHub release is available",
                        checked = settings.notifyUpdates,
                        icon = Icons.Outlined.Notifications,
                        onCheckedChange = viewModel::updateNotificationUpdates
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
