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
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Language
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
fun AppearanceSettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsState()
    var showThemeDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }

    val themeLabel = when (settings.darkMode) {
        null -> "System Default"
        true -> "Dark"
        false -> "Light"
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Appearance", fontWeight = FontWeight.SemiBold) },
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
                SettingsSectionTitle("Theme & Styling")
                SettingsCard {
                    SettingsValueRow(
                        title = "App Theme",
                        subtitle = "Select Light, Dark, or match Android System mode",
                        value = themeLabel,
                        icon = Icons.Outlined.DarkMode,
                        onClick = { showThemeDialog = true }
                    )
                    SettingsSwitchRow(
                        title = "Dynamic Colors (Material You)",
                        subtitle = "Extract theme palette from wallpaper on Android 12+",
                        checked = settings.dynamicColor,
                        icon = Icons.Outlined.ColorLens,
                        onCheckedChange = viewModel::updateDynamicColor
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(12.dp))
                SettingsSectionTitle("Localization")
                SettingsCard {
                    SettingsValueRow(
                        title = "Application Language",
                        subtitle = "Choose preferred display language",
                        value = settings.language,
                        icon = Icons.Outlined.Language,
                        onClick = { showLanguageDialog = true }
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showThemeDialog) {
        val themeOptions = listOf("System Default", "Light", "Dark")
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Choose Theme") },
            text = {
                Column {
                    themeOptions.forEach { option ->
                        val isSelected = option == themeLabel
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val mode = when (option) {
                                        "Dark" -> true
                                        "Light" -> false
                                        else -> null
                                    }
                                    viewModel.updateDarkMode(mode)
                                    showThemeDialog = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    val mode = when (option) {
                                        "Dark" -> true
                                        "Light" -> false
                                        else -> null
                                    }
                                    viewModel.updateDarkMode(mode)
                                    showThemeDialog = false
                                }
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
                TextButton(onClick = { showThemeDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showLanguageDialog) {
        StringPickerDialog(
            title = "Application Language",
            options = listOf("System Default", "English", "Arabic (العربية)"),
            selected = settings.language,
            onSelect = {
                viewModel.updateLanguage(it)
                showLanguageDialog = false
            },
            onDismiss = { showLanguageDialog = false }
        )
    }
}
