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
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.PictureInPicture
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.StayCurrentPortrait
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
fun PlayerSettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsState()
    var showSpeedDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Media Player Settings", fontWeight = FontWeight.SemiBold) },
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
                SettingsSectionTitle("Playback Preferences")
                SettingsCard {
                    SettingsSwitchRow(
                        title = "Remember Playback Position",
                        subtitle = "Resume video and audio from where you left off",
                        checked = settings.rememberPlaybackPosition,
                        icon = Icons.Outlined.History,
                        onCheckedChange = viewModel::updateRememberPosition
                    )
                    SettingsSwitchRow(
                        title = "Auto-Play Next Media",
                        subtitle = "Play the next library item when current media finishes",
                        checked = settings.autoPlayNext,
                        icon = Icons.Outlined.PlayCircle,
                        onCheckedChange = viewModel::updateAutoPlayNext
                    )
                    SettingsSwitchRow(
                        title = "Background Audio Playback",
                        subtitle = "Continue playing audio when app is minimized or screen off",
                        checked = settings.backgroundAudio,
                        icon = Icons.Outlined.GraphicEq,
                        onCheckedChange = viewModel::updateBackgroundAudio
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(12.dp))
                SettingsSectionTitle("Video Display & Controls")
                SettingsCard {
                    SettingsValueRow(
                        title = "Default Playback Speed",
                        subtitle = "Initial playback rate for videos and songs",
                        value = "${settings.defaultPlaybackSpeed}x",
                        icon = Icons.Outlined.Speed,
                        onClick = { showSpeedDialog = true }
                    )
                    SettingsSwitchRow(
                        title = "Keep Screen On",
                        subtitle = "Prevent screen timeout while watching video",
                        checked = settings.keepScreenOn,
                        icon = Icons.Outlined.StayCurrentPortrait,
                        onCheckedChange = viewModel::updateKeepScreenOn
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showSpeedDialog) {
        val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
        AlertDialog(
            onDismissRequest = { showSpeedDialog = false },
            title = { Text("Default Playback Speed") },
            text = {
                Column {
                    speeds.forEach { speed ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.updatePlaybackSpeed(speed)
                                    showSpeedDialog = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = settings.defaultPlaybackSpeed == speed,
                                onClick = {
                                    viewModel.updatePlaybackSpeed(speed)
                                    showSpeedDialog = false
                                }
                            )
                            Text(
                                text = "${speed}x",
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(start = 12.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSpeedDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
