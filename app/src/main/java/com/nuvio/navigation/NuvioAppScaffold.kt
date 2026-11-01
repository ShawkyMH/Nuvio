package com.nuvio.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.nuvio.NuvioApp
import com.nuvio.core.designsystem.component.NuvioBottomNavigation
import com.nuvio.core.update.UpdateViewModel
import com.nuvio.feature.browser.BrowserScreen
import com.nuvio.feature.browser.BrowserViewModel
import com.nuvio.feature.downloads.DownloadsScreen
import com.nuvio.feature.downloads.DownloadsViewModel
import com.nuvio.feature.home.HomeScreen
import com.nuvio.feature.home.HomeViewModel
import com.nuvio.feature.library.LibraryScreen
import com.nuvio.feature.library.LibraryViewModel
import com.nuvio.feature.player.PlayerScreen
import com.nuvio.feature.player.PlayerViewModel
import com.nuvio.feature.settings.SettingsScreen
import com.nuvio.feature.settings.SettingsViewModel

@Composable
fun NuvioAppScaffold(
    initialSharedUrl: String? = null,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Destination.HOME.route

    val currentDestination = Destination.values().firstOrNull { it.route == currentRoute } ?: Destination.HOME

    val container = NuvioApp.instance.container

    // ViewModels instantiated with container dependencies
    val homeViewModel = remember {
        HomeViewModel(
            downloadRepository = container.downloadRepository,
            mediaRepository = container.mediaRepository,
            fileManager = container.fileManager
        )
    }

    val downloadsViewModel = remember {
        DownloadsViewModel(
            repository = container.downloadRepository
        )
    }

    val browserViewModel = remember {
        BrowserViewModel(
            downloadRepository = container.downloadRepository,
            fileManager = container.fileManager
        )
    }

    val libraryViewModel = remember {
        LibraryViewModel(
            fileManager = container.fileManager
        )
    }

    val settingsViewModel = remember {
        SettingsViewModel(
            settingsRepository = container.settingsRepository,
            playbackRepository = container.playbackRepository
        )
    }

    val updateViewModel = remember {
        UpdateViewModel(
            repository = container.updateRepository
        )
    }

    val playerViewModel = remember {
        PlayerViewModel(
            context = NuvioApp.instance,
            playbackRepository = container.playbackRepository
        )
    }

    var isPlayerVisible by remember { mutableStateOf(false) }

    // Handle shared incoming URL
    remember(initialSharedUrl) {
        if (!initialSharedUrl.isNullOrBlank()) {
            homeViewModel.onQueryChange(initialSharedUrl)
            homeViewModel.submitQuery(initialSharedUrl)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = {
                if (!isPlayerVisible) {
                    NuvioBottomNavigation(
                        currentDestination = currentDestination,
                        onNavigateToDestination = { destination ->
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Destination.HOME.route,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                composable(Destination.HOME.route) {
                    HomeScreen(
                        viewModel = homeViewModel,
                        onNavigateToBrowser = { url ->
                            if (url != null) browserViewModel.loadUrl(url)
                            navController.navigate(Destination.BROWSER.route)
                        },
                        onNavigateToDownloads = {
                            navController.navigate(Destination.DOWNLOADS.route)
                        },
                        onNavigateToLibrary = {
                            navController.navigate(Destination.LIBRARY.route)
                        },
                        onPlayMedia = { uri, title ->
                            playerViewModel.playMedia(uri, title)
                            isPlayerVisible = true
                        }
                    )
                }

                composable(Destination.DOWNLOADS.route) {
                    DownloadsScreen(
                        viewModel = downloadsViewModel,
                        onPlayMedia = { uri, title ->
                            playerViewModel.playMedia(uri, title)
                            isPlayerVisible = true
                        }
                    )
                }

                composable(Destination.BROWSER.route) {
                    BrowserScreen(viewModel = browserViewModel)
                }

                composable(Destination.LIBRARY.route) {
                    LibraryScreen(
                        viewModel = libraryViewModel,
                        onPlayMedia = { uri, title ->
                            playerViewModel.playMedia(uri, title)
                            isPlayerVisible = true
                        }
                    )
                }

                composable(Destination.SETTINGS.route) {
                    SettingsScreen(
                        viewModel = settingsViewModel,
                        updateViewModel = updateViewModel
                    )
                }
            }
        }

        // Fullscreen Player Overlay
        AnimatedVisibility(
            visible = isPlayerVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            PlayerScreen(
                viewModel = playerViewModel,
                onClose = {
                    isPlayerVisible = false
                    playerViewModel.exoPlayer?.pause()
                }
            )
        }
    }
}
