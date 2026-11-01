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
import androidx.compose.material.icons.outlined.Audiotrack
import androidx.compose.material.icons.outlined.HighQuality
import androidx.compose.material.icons.outlined.Subtitles
import androidx.compose.material.icons.outlined.VideoLibrary
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
fun MediaSettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsState()
    var showVideoQualityDialog by remember { mutableStateOf(false) }
    var showVideoFormatDialog by remember { mutableStateOf(false) }
    var showAudioQualityDialog by remember { mutableStateOf(false) }
    var showAudioFormatDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Video & Audio Settings", fontWeight = FontWeight.SemiBold) },
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
                SettingsSectionTitle("Video Preferences")
                SettingsCard {
                    SettingsValueRow(
                        title = "Preferred Video Quality",
                        subtitle = "Target stream resolution when inspecting media",
                        value = settings.preferredVideoQuality,
                        icon = Icons.Outlined.HighQuality,
                        onClick = { showVideoQualityDialog = true }
                    )
                    SettingsValueRow(
                        title = "Default Video Container",
                        subtitle = "Target container format for video downloads",
                        value = settings.defaultVideoFormat.uppercase(),
                        icon = Icons.Outlined.VideoLibrary,
                        onClick = { showVideoFormatDialog = true }
                    )
                    SettingsSwitchRow(
                        title = "Prompt for Quality Selection",
                        subtitle = "Display format selection sheet before downloading videos",
                        checked = settings.askQualityBeforeDownload,
                        onCheckedChange = viewModel::updateAskQualityBeforeDownload
                    )
                    SettingsSwitchRow(
                        title = "Prefer Original Format",
                        subtitle = "Avoid stream transcode/remuxing when container matches",
                        checked = settings.preferOriginalFormat,
                        onCheckedChange = viewModel::updatePreferOriginalFormat
                    )
                    SettingsSwitchRow(
                        title = "Download Subtitles",
                        subtitle = "Automatically extract embedded or closed captions",
                        checked = settings.downloadSubtitles,
                        icon = Icons.Outlined.Subtitles,
                        onCheckedChange = viewModel::updateDownloadSubtitles
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(12.dp))
                SettingsSectionTitle("Audio Preferences")
                SettingsCard {
                    SettingsValueRow(
                        title = "Preferred Audio Bitrate",
                        subtitle = "Encoding quality for music and extracted audio",
                        value = settings.preferredAudioQuality,
                        icon = Icons.Outlined.Audiotrack,
                        onClick = { showAudioQualityDialog = true }
                    )
                    SettingsValueRow(
                        title = "Default Audio Container",
                        subtitle = "Audio file format for extracted tracks",
                        value = settings.defaultAudioFormat.uppercase(),
                        onClick = { showAudioFormatDialog = true }
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showVideoQualityDialog) {
        StringPickerDialog(
            title = "Preferred Video Quality",
            options = listOf("Highest Available", "1080p Full HD", "720p HD", "480p SD", "360p"),
            selected = settings.preferredVideoQuality,
            onSelect = {
                viewModel.updatePreferredVideoQuality(it)
                showVideoQualityDialog = false
            },
            onDismiss = { showVideoQualityDialog = false }
        )
    }

    if (showVideoFormatDialog) {
        StringPickerDialog(
            title = "Default Video Container",
            options = listOf("mp4", "mkv", "webm"),
            selected = settings.defaultVideoFormat,
            onSelect = {
                viewModel.updateDefaultVideoFormat(it)
                showVideoFormatDialog = false
            },
            onDismiss = { showVideoFormatDialog = false }
        )
    }

    if (showAudioQualityDialog) {
        StringPickerDialog(
            title = "Preferred Audio Quality",
            options = listOf("320 kbps", "256 kbps", "128 kbps"),
            selected = settings.preferredAudioQuality,
            onSelect = {
                viewModel.updatePreferredAudioQuality(it)
                showAudioQualityDialog = false
            },
            onDismiss = { showAudioQualityDialog = false }
        )
    }

    if (showAudioFormatDialog) {
        StringPickerDialog(
            title = "Default Audio Container",
            options = listOf("mp3", "m4a"),
            selected = settings.defaultAudioFormat,
            onSelect = {
                viewModel.updateDefaultAudioFormat(it)
                showAudioFormatDialog = false
            },
            onDismiss = { showAudioFormatDialog = false }
        )
    }
}

@Composable
fun StringPickerDialog(
    title: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
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
                            selected = option.equals(selected, ignoreCase = true),
                            onClick = { onSelect(option) }
                        )
                        Text(
                            text = option,
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
