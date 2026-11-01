package com.nuvio

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.nuvio.core.designsystem.theme.NuvioTheme
import com.nuvio.domain.model.AppSettings
import com.nuvio.navigation.NuvioAppScaffold

class MainActivity : ComponentActivity() {

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Permission granted or denied */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_Nuvio)
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        checkNotificationPermission()

        val initialSharedUrl = extractSharedUrl(intent)

        setContent {
            val settingsRepo = NuvioApp.instance.container.settingsRepository
            val settings by settingsRepo.getSettings().collectAsState(initial = AppSettings())

            val isDark = when (settings.darkMode) {
                true -> true
                false -> false
                null -> isSystemInDarkTheme()
            }

            NuvioTheme(
                darkTheme = isDark,
                dynamicColor = settings.dynamicColor
            ) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    NuvioAppScaffold(initialSharedUrl = initialSharedUrl)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasPermission) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun extractSharedUrl(intent: Intent?): String? {
        if (intent == null) return null
        return when (intent.action) {
            Intent.ACTION_SEND -> {
                if (intent.type == "text/plain") {
                    intent.getStringExtra(Intent.EXTRA_TEXT)
                } else null
            }
            Intent.ACTION_VIEW -> {
                intent.dataString
            }
            else -> null
        }
    }
}
