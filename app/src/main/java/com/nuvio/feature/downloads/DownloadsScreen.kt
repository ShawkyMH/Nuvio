package com.nuvio.feature.downloads

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.DownloadDone
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nuvio.core.designsystem.component.DownloadCard
import com.nuvio.core.designsystem.component.EmptyState
import com.nuvio.core.designsystem.component.formatSpeed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(
    viewModel: DownloadsViewModel,
    onPlayMedia: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val tasks by viewModel.filteredTasks.collectAsState()
    val filter by viewModel.selectedFilter.collectAsState()
    val totalSpeed by viewModel.totalActiveSpeed.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Downloads",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (totalSpeed > 0) {
                            Text(
                                text = "Total Speed: ${formatSpeed(totalSpeed)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::resumeAll) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Resume All",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = viewModel::pauseAll) {
                        Icon(
                            imageVector = Icons.Default.Pause,
                            contentDescription = "Pause All",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Filter Chips Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DownloadFilter.values().forEach { item ->
                    val isSelected = filter == item
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.setFilter(item) },
                        label = { Text(item.name.lowercase().replaceFirstChar { it.uppercase() }) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }

            if (tasks.isEmpty()) {
                val (emptyIcon, emptyTitle, emptyMsg) = when (filter) {
                    DownloadFilter.ACTIVE -> Triple(
                        Icons.Outlined.FileDownload,
                        "No active downloads",
                        "Currently there are no downloads in progress"
                    )
                    DownloadFilter.COMPLETED -> Triple(
                        Icons.Outlined.DownloadDone,
                        "No completed downloads",
                        "Finished downloads will appear here"
                    )
                    DownloadFilter.ALL -> Triple(
                        Icons.Outlined.FileDownload,
                        "No downloads yet",
                        "Submit a link to start downloading files and media"
                    )
                }
                EmptyState(
                    icon = emptyIcon,
                    title = emptyTitle,
                    message = emptyMsg
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(tasks, key = { it.id }) { task ->
                        DownloadCard(
                            task = task,
                            onPauseClick = { viewModel.pauseTask(task.id) },
                            onResumeClick = { viewModel.resumeTask(task.id) },
                            onCancelClick = { viewModel.cancelTask(task.id) },
                            onPlayClick = { onPlayMedia(task.filePath, task.title) }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }
    }
}
