package com.nuvio.feature.home

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nuvio.core.designsystem.component.AppSearchBar
import com.nuvio.core.designsystem.component.DownloadCard
import com.nuvio.core.designsystem.component.EmptyState
import com.nuvio.core.designsystem.component.SectionHeader

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToBrowser: (String?) -> Unit,
    onNavigateToDownloads: () -> Unit,
    onNavigateToLibrary: () -> Unit,
    onPlayMedia: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val activeDownloads by viewModel.activeDownloads.collectAsState()
    val recentDownloads by viewModel.recentDownloads.collectAsState()
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is HomeUiEvent.ShowToast -> Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                is HomeUiEvent.NavigateToBrowser -> onNavigateToBrowser(event.url)
                is HomeUiEvent.PlayMedia -> onPlayMedia(event.uri, event.title)
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // App Branding Header
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp, bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Nuvio",
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-1).sp
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Smart Media & File Downloader",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Google-Inspired Pill Search Bar
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    AppSearchBar(
                        query = uiState.searchQuery,
                        onQueryChange = viewModel::onQueryChange,
                        onSearch = viewModel::submitQuery,
                        onPasteClick = {
                            val clip = clipboardManager.getText()?.text
                            if (!clip.isNullOrBlank()) {
                                viewModel.onQueryChange(clip)
                                viewModel.submitQuery(clip)
                            } else {
                                Toast.makeText(context, "Clipboard is empty", Toast.LENGTH_SHORT).show()
                            }
                        },
                        isLoading = uiState.isLoading
                    )
                }
            }

            // Quick Actions Row
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    QuickActionButton(
                        icon = Icons.Default.ContentPaste,
                        label = "Paste Link",
                        onClick = {
                            val clip = clipboardManager.getText()?.text
                            if (!clip.isNullOrBlank()) {
                                viewModel.onQueryChange(clip)
                                viewModel.submitQuery(clip)
                            } else {
                                Toast.makeText(context, "Clipboard is empty", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                    QuickActionButton(
                        icon = Icons.Default.Language,
                        label = "Browser",
                        onClick = { onNavigateToBrowser(null) }
                    )
                    QuickActionButton(
                        icon = Icons.Default.Folder,
                        label = "Files",
                        onClick = onNavigateToLibrary
                    )
                    QuickActionButton(
                        icon = Icons.Default.History,
                        label = "Downloads",
                        onClick = onNavigateToDownloads
                    )
                }
            }

            // Active Downloads Section
            if (activeDownloads.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = "Active Downloads",
                        actionText = "See All",
                        onActionClick = onNavigateToDownloads
                    )
                }
                items(activeDownloads, key = { it.id }) { task ->
                    DownloadCard(
                        task = task,
                        onPauseClick = { },
                        onResumeClick = { },
                        onCancelClick = { },
                        onPlayClick = { onPlayMedia(task.filePath, task.title) }
                    )
                }
            }

            // Recent Downloads Section
            if (recentDownloads.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    SectionHeader(
                        title = "Recently Downloaded",
                        actionText = "View All",
                        onActionClick = onNavigateToDownloads
                    )
                }
                items(recentDownloads.take(3), key = { it.id }) { task ->
                    DownloadCard(
                        task = task,
                        onPauseClick = { },
                        onResumeClick = { },
                        onCancelClick = { },
                        onPlayClick = { onPlayMedia(task.filePath, task.title) }
                    )
                }
            } else if (activeDownloads.isEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(32.dp))
                    EmptyState(
                        icon = Icons.Outlined.FileDownload,
                        title = "No active downloads",
                        message = "Paste a link above or browse the web to start downloading videos, music, and files"
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // Format selection bottom sheet
    if (uiState.showFormatSheet && uiState.selectedMedia != null) {
        FormatSelectionBottomSheet(
            media = uiState.selectedMedia!!,
            onFormatSelected = viewModel::startDownloadWithFormat,
            onDismiss = viewModel::dismissFormatSheet
        )
    }
}

@Composable
private fun QuickActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Surface(
            modifier = Modifier.size(52.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
