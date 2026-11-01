package com.nuvio.feature.settings.screen

import android.webkit.CookieManager
import android.webkit.WebStorage
import android.widget.Toast
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
import androidx.compose.material.icons.outlined.DesktopWindows
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nuvio.feature.settings.SettingsViewModel
import com.nuvio.feature.settings.component.SettingsActionRow
import com.nuvio.feature.settings.component.SettingsCard
import com.nuvio.feature.settings.component.SettingsSectionTitle
import com.nuvio.feature.settings.component.SettingsSwitchRow
import com.nuvio.feature.settings.component.SettingsValueRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserSettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsState()
    val context = LocalContext.current
    var showHomepageDialog by remember { mutableStateOf(false) }
    var showSearchEngineDialog by remember { mutableStateOf(false) }
    var showClearCookiesDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Browser Settings", fontWeight = FontWeight.SemiBold) },
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
                SettingsSectionTitle("Navigation & Search")
                SettingsCard {
                    SettingsValueRow(
                        title = "Default Search Engine",
                        subtitle = "Provider used for address bar queries",
                        value = settings.searchEngine,
                        icon = Icons.Outlined.Search,
                        onClick = { showSearchEngineDialog = true }
                    )
                    SettingsValueRow(
                        title = "Browser Homepage",
                        subtitle = "URL loaded on initial browser launch",
                        value = settings.browserHomepage,
                        icon = Icons.Outlined.Home,
                        onClick = { showHomepageDialog = true }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(12.dp))
                SettingsSectionTitle("Web Experience & Media")
                SettingsCard {
                    SettingsSwitchRow(
                        title = "Always Request Desktop Site",
                        subtitle = "Send desktop browser User-Agent to web servers",
                        checked = settings.desktopSite,
                        icon = Icons.Outlined.DesktopWindows,
                        onCheckedChange = viewModel::updateDesktopSite
                    )
                    SettingsSwitchRow(
                        title = "Confirm Media Download Bar",
                        subtitle = "Show floating banner when video or audio stream is detected",
                        checked = settings.askBeforeDownloadMedia,
                        onCheckedChange = viewModel::updateAskBeforeDownloadMedia
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(12.dp))
                SettingsSectionTitle("Privacy & Cache Maintenance")
                SettingsCard {
                    SettingsActionRow(
                        title = "Clear Web Storage & Cache",
                        subtitle = "Deletes cached web assets and HTML localStorage",
                        icon = Icons.Outlined.Security,
                        onClick = {
                            runCatching { WebStorage.getInstance().deleteAllData() }
                            Toast.makeText(context, "Web storage cleared", Toast.LENGTH_SHORT).show()
                        }
                    )
                    SettingsActionRow(
                        title = "Clear Cookies & Sessions",
                        subtitle = "Signs out of active web browser login sessions",
                        icon = Icons.Outlined.Language,
                        isDestructive = true,
                        onClick = { showClearCookiesDialog = true }
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showHomepageDialog) {
        var tempUrl by remember { mutableStateOf(settings.browserHomepage) }
        AlertDialog(
            onDismissRequest = { showHomepageDialog = false },
            title = { Text("Set Browser Homepage") },
            text = {
                OutlinedTextField(
                    value = tempUrl,
                    onValueChange = { tempUrl = it },
                    label = { Text("Homepage URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val trimmed = tempUrl.trim()
                        if (trimmed.isNotBlank()) {
                            viewModel.updateBrowserHomepage(trimmed)
                        }
                        showHomepageDialog = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showHomepageDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showSearchEngineDialog) {
        StringPickerDialog(
            title = "Default Search Engine",
            options = listOf("Google", "Bing", "DuckDuckGo"),
            selected = settings.searchEngine,
            onSelect = {
                viewModel.updateSearchEngine(it)
                showSearchEngineDialog = false
            },
            onDismiss = { showSearchEngineDialog = false }
        )
    }

    if (showClearCookiesDialog) {
        AlertDialog(
            onDismissRequest = { showClearCookiesDialog = false },
            title = { Text("Clear All Web Cookies?") },
            text = { Text("This will sign you out of all accounts logged in via the built-in browser.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        runCatching {
                            CookieManager.getInstance().removeAllCookies(null)
                            CookieManager.getInstance().flush()
                        }
                        showClearCookiesDialog = false
                        Toast.makeText(context, "Cookies cleared", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Clear", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearCookiesDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
