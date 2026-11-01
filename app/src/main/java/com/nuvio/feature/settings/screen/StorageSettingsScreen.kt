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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.SdCard
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nuvio.core.designsystem.component.formatBytes
import com.nuvio.feature.settings.SettingsViewModel
import com.nuvio.feature.settings.component.SettingsActionRow
import com.nuvio.feature.settings.component.SettingsCard
import com.nuvio.feature.settings.component.SettingsSectionTitle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageSettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onNavigateToLibrary: () -> Unit,
    modifier: Modifier = Modifier
) {
    val storage by viewModel.storageMetrics.collectAsState()
    val context = LocalContext.current
    var showClearCacheDialog by remember { mutableStateOf(false) }
    var showClearStagingDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Storage Settings", fontWeight = FontWeight.SemiBold) },
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
                SettingsSectionTitle("Storage Overview")
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Device Storage",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Icon(
                                imageVector = Icons.Outlined.SdCard,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        val usedBytes = (storage.totalBytes - storage.availableBytes).coerceAtLeast(0L)
                        val progressFraction = if (storage.totalBytes > 0) {
                            (usedBytes.toFloat() / storage.totalBytes.toFloat()).coerceIn(0f, 1f)
                        } else 0f

                        LinearProgressIndicator(
                            progress = { progressFraction },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Available: ${formatBytes(storage.availableBytes)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Total: ${formatBytes(storage.totalBytes)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(12.dp))
                SettingsSectionTitle("Nuvio Storage Breakdown")
                SettingsCard {
                    StorageDetailRow(
                        title = "Downloaded Files",
                        subtitle = "Saved media, archives, and documents",
                        sizeBytes = storage.downloadsSizeBytes,
                        icon = Icons.Outlined.Folder
                    )
                    StorageDetailRow(
                        title = "Temporary Downloads (.staging)",
                        subtitle = "Active download segments and staging artifacts",
                        sizeBytes = storage.stagingSizeBytes,
                        icon = Icons.Outlined.FolderOpen
                    )
                    StorageDetailRow(
                        title = "Application Cache",
                        subtitle = "Thumbnails, web cache, and response cache",
                        sizeBytes = storage.cacheSizeBytes,
                        icon = Icons.Outlined.CleaningServices
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(12.dp))
                SettingsSectionTitle("Cleanup & Maintenance")
                SettingsCard {
                    SettingsActionRow(
                        title = "Clear Temporary Download Files",
                        subtitle = "Delete unfinished or orphaned segment files in .staging",
                        icon = Icons.Outlined.DeleteOutline,
                        isDestructive = true,
                        onClick = { showClearStagingDialog = true }
                    )
                    SettingsActionRow(
                        title = "Clear Application Cache",
                        subtitle = "Free up memory and image/web cache (never deletes downloads)",
                        icon = Icons.Outlined.CleaningServices,
                        onClick = { showClearCacheDialog = true }
                    )
                    SettingsActionRow(
                        title = "Manage Downloaded Files",
                        subtitle = "View, play, open, or delete completed files in Library",
                        icon = Icons.Outlined.Folder,
                        onClick = onNavigateToLibrary
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showClearStagingDialog) {
        AlertDialog(
            onDismissRequest = { showClearStagingDialog = false },
            title = { Text("Clear Temporary Files?") },
            text = { Text("This will remove all unfinished download segments and staging files. Active downloads may need to be restarted.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearStagingFiles()
                        showClearStagingDialog = false
                        Toast.makeText(context, "Temporary files cleared", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Clear", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearStagingDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showClearCacheDialog) {
        AlertDialog(
            onDismissRequest = { showClearCacheDialog = false },
            title = { Text("Clear Application Cache?") },
            text = { Text("This will safely clear temporary image cache and response caches. Your downloaded files will remain intact.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearCache()
                        showClearCacheDialog = false
                        Toast.makeText(context, "Cache cleared successfully", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun StorageDetailRow(
    title: String,
    subtitle: String,
    sizeBytes: Long,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(end = 16.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = formatBytes(sizeBytes),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
