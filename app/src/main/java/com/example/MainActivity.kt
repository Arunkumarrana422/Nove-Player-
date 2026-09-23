package com.example

import android.Manifest
import android.app.PictureInPictureParams
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.domain.model.Playlist
import com.example.domain.model.ThemePreference
import com.example.domain.model.Video
import com.example.ui.components.AddToPlaylistDialog
import com.example.ui.components.CreatePlaylistDialog
import com.example.ui.components.MiniPlayerView
import com.example.ui.components.NovaAppBar
import com.example.ui.components.VideoInfoBottomSheet
import com.example.ui.navigation.BottomNavItems
import com.example.ui.navigation.Screen
import com.example.ui.screens.FavoritesScreen
import com.example.ui.screens.FoldersScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.OnlineStreamScreen
import com.example.ui.screens.PlayerScreen
import com.example.ui.screens.PlaylistsScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.VideosScreen
import com.example.ui.theme.NovaAccent
import com.example.ui.theme.NovaPlayerTheme
import com.example.ui.theme.NovaPrimary
import com.example.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    private val storagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.any { it }
        viewModel.setStoragePermission(granted)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        checkAndRequestPermissions()
        handleIncomingIntent(intent)

        setContent {
            val userSettings by viewModel.userSettings.collectAsState()

            NovaPlayerTheme(themePreference = userSettings.theme) {
                NovaPlayerApp(
                    viewModel = viewModel,
                    onEnterPiP = { enterPiPMode() }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW) {
            val uri: Uri? = intent.data
            if (uri != null) {
                val videoTitle = uri.lastPathSegment ?: "External Video"
                val video = Video(
                    id = uri.toString(),
                    title = videoTitle,
                    path = uri.toString(),
                    uri = uri.toString(),
                    durationMs = 0L,
                    sizeBytes = 0L,
                    width = 0,
                    height = 0,
                    mimeType = intent.type ?: "video/*",
                    dateAdded = System.currentTimeMillis(),
                    folderName = "External"
                )
                viewModel.playerManager.playVideo(video)
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_VIDEO)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        val hasPermission = permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

        viewModel.setStoragePermission(hasPermission)

        if (!hasPermission) {
            storagePermissionLauncher.launch(permissions)
        }
    }

    private fun enterPiPMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(16, 9))
                .build()
            enterPictureInPictureMode(params)
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (viewModel.playerManager.isPlaying.value && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            enterPiPMode()
        }
    }
}

@Composable
fun NovaPlayerApp(
    viewModel: MainViewModel,
    onEnterPiP: () -> Unit
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val userSettings by viewModel.userSettings.collectAsState()
    val allVideos by viewModel.allVideos.collectAsState()
    val favoriteVideos by viewModel.favoriteVideos.collectAsState()
    val watchHistory by viewModel.watchHistory.collectAsState()
    val continueWatching by viewModel.continueWatching.collectAsState()
    val onlineVideos by viewModel.onlineVideos.collectAsState()
    val folders by viewModel.folders.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val recentSearches by viewModel.recentSearches.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedFolder by viewModel.selectedFolder.collectAsState()
    val selectedPlaylist by viewModel.selectedPlaylist.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()

    // Active Player Manager states
    val currentVideoPlaying by viewModel.playerManager.currentVideo.collectAsState()
    val isPlaying by viewModel.playerManager.isPlaying.collectAsState()
    val currentPosMs by viewModel.playerManager.currentPositionMs.collectAsState()
    val durationMs by viewModel.playerManager.durationMs.collectAsState()

    // Dialog & BottomSheet state
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var videoToAddToPlaylist by remember { mutableStateOf<Video?>(null) }
    var infoVideo by remember { mutableStateOf<Video?>(null) }

    val startDestination = if (userSettings.onboardingCompleted) Screen.Home.route else Screen.Onboarding.route

    val isPlayerScreen = currentRoute == Screen.Player.route
    val isOnboardingScreen = currentRoute == Screen.Onboarding.route
    val showBottomBar = !isPlayerScreen && !isOnboardingScreen

    Scaffold(
        topBar = {
            if (!isPlayerScreen && !isOnboardingScreen && currentRoute != Screen.Search.route && currentRoute != Screen.Settings.route) {
                NovaAppBar(
                    title = when (currentRoute) {
                        Screen.Home.route -> "Nova Player"
                        Screen.Videos.route -> "Video Library"
                        Screen.Folders.route -> "Folders"
                        Screen.Playlists.route -> "Playlists"
                        Screen.Online.route -> "Network Stream"
                        Screen.Favorites.route -> "Favorites"
                        Screen.History.route -> "Watch History"
                        else -> "Nova Player"
                    },
                    themePreference = userSettings.theme,
                    onSearchClick = { navController.navigate(Screen.Search.route) },
                    onRefreshClick = { viewModel.scanLibrary() },
                    onThemeToggle = {
                        val nextTheme = if (userSettings.theme == ThemePreference.DARK) ThemePreference.LIGHT else ThemePreference.DARK
                        viewModel.setTheme(nextTheme)
                    },
                    onSettingsClick = { navController.navigate(Screen.Settings.route) }
                )
            }
        },
        bottomBar = {
            if (showBottomBar) {
                Column {
                    // Floating MiniPlayer bar when playing
                    if (currentVideoPlaying != null) {
                        MiniPlayerView(
                            video = currentVideoPlaying,
                            isPlaying = isPlaying,
                            currentPosMs = currentPosMs,
                            durationMs = durationMs,
                            onExpand = {
                                navController.navigate(Screen.Player.route)
                            },
                            onPlayPause = {
                                viewModel.playerManager.togglePlayPause()
                            },
                            onClose = {
                                viewModel.playerManager.pause()
                            }
                        )
                    }

                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        tonalElevation = 6.dp,
                        modifier = Modifier.testTag("bottom_navigation_bar")
                    ) {
                        BottomNavItems.forEach { item ->
                            val selected = currentRoute == item.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    if (currentRoute != item.route) {
                                        navController.navigate(item.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                                icon = {
                                    item.icon?.let { icon ->
                                        Icon(imageVector = icon, contentDescription = item.title)
                                    }
                                },
                                label = { Text(item.title) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.White,
                                    selectedTextColor = NovaAccent,
                                    indicatorColor = NovaPrimary,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.testTag("nav_tab_${item.route}")
                            )
                        }
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isPlayerScreen) androidx.compose.foundation.layout.PaddingValues(0.dp) else innerPadding)
        ) {
            NavHost(
                navController = navController,
                startDestination = startDestination,
                modifier = Modifier.fillMaxSize()
            ) {
                composable(Screen.Onboarding.route) {
                    OnboardingScreen(
                        onGetStarted = {
                            viewModel.setOnboardingCompleted(true)
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Onboarding.route) { inclusive = true }
                            }
                        }
                    )
                }

                composable(Screen.Home.route) {
                    HomeScreen(
                        continueWatching = continueWatching,
                        recentVideos = allVideos,
                        featuredStreams = onlineVideos,
                        folders = folders,
                        playlists = playlists,
                        allVideosCount = allVideos.size,
                        onPlayVideo = { video, playlist ->
                            viewModel.playerManager.playVideo(video, playlist)
                            navController.navigate(Screen.Player.route)
                        },
                        onOpenStreamDialog = { navController.navigate(Screen.Online.route) },
                        onNavigateToVideos = { navController.navigate(Screen.Videos.route) },
                        onNavigateToFolders = { navController.navigate(Screen.Folders.route) },
                        onNavigateToPlaylists = { navController.navigate(Screen.Playlists.route) },
                        onNavigateToFavorites = { navController.navigate(Screen.Favorites.route) },
                        onNavigateToHistory = { navController.navigate(Screen.History.route) },
                        onToggleFavorite = { viewModel.toggleFavorite(it) },
                        onAddToPlaylist = { videoToAddToPlaylist = it },
                        onShowVideoInfo = { infoVideo = it },
                        onDeleteVideo = { viewModel.deleteVideo(it) }
                    )
                }

                composable(Screen.Videos.route) {
                    VideosScreen(
                        videos = allVideos,
                        sortOption = userSettings.sortOption,
                        viewMode = userSettings.viewMode,
                        isScanning = isScanning,
                        onSortChange = { viewModel.setSortOption(it) },
                        onViewModeChange = { viewModel.setViewMode(it) },
                        onRefresh = { viewModel.scanLibrary() },
                        onPlayVideo = { video, playlist ->
                            viewModel.playerManager.playVideo(video, playlist)
                            navController.navigate(Screen.Player.route)
                        },
                        onToggleFavorite = { viewModel.toggleFavorite(it) },
                        onAddToPlaylist = { videoToAddToPlaylist = it },
                        onShowVideoInfo = { infoVideo = it },
                        onDeleteVideo = { viewModel.deleteVideo(it) }
                    )
                }

                composable(Screen.Folders.route) {
                    FoldersScreen(
                        folders = folders,
                        selectedFolder = selectedFolder,
                        onSelectFolder = { viewModel.selectFolder(it) },
                        onPlayVideo = { video, playlist ->
                            viewModel.playerManager.playVideo(video, playlist)
                            navController.navigate(Screen.Player.route)
                        },
                        onToggleFavorite = { viewModel.toggleFavorite(it) },
                        onAddToPlaylist = { videoToAddToPlaylist = it },
                        onShowVideoInfo = { infoVideo = it },
                        onDeleteVideo = { viewModel.deleteVideo(it) }
                    )
                }

                composable(Screen.Playlists.route) {
                    PlaylistsScreen(
                        playlists = playlists,
                        selectedPlaylist = selectedPlaylist,
                        playlistVideosFlow = { id -> viewModel.getPlaylistVideos(id) },
                        onSelectPlaylist = { viewModel.selectPlaylist(it) },
                        onCreatePlaylistClick = { showCreatePlaylistDialog = true },
                        onDeletePlaylist = { viewModel.deletePlaylist(it) },
                        onRemoveFromPlaylist = { pId, vId -> viewModel.removeVideoFromPlaylist(pId, vId) },
                        onPlayVideo = { video, playlist ->
                            viewModel.playerManager.playVideo(video, playlist)
                            navController.navigate(Screen.Player.route)
                        },
                        onToggleFavorite = { viewModel.toggleFavorite(it) },
                        onAddToPlaylist = { videoToAddToPlaylist = it },
                        onShowVideoInfo = { infoVideo = it }
                    )
                }

                composable(Screen.Online.route) {
                    OnlineStreamScreen(
                        onlineVideos = onlineVideos,
                        onPlayOnlineVideo = { video, playlist ->
                            viewModel.playerManager.playVideo(video, playlist)
                            navController.navigate(Screen.Player.route)
                        },
                        onAddAndPlay = { title, url ->
                            viewModel.addOnlineVideo(title, url) { created ->
                                viewModel.playerManager.playVideo(created)
                                navController.navigate(Screen.Player.route)
                            }
                        },
                        onToggleFavorite = { viewModel.toggleFavorite(it) },
                        onAddToPlaylist = { videoToAddToPlaylist = it },
                        onShowVideoInfo = { infoVideo = it },
                        onDeleteVideo = { viewModel.deleteVideo(it) }
                    )
                }

                composable(Screen.Favorites.route) {
                    FavoritesScreen(
                        favorites = favoriteVideos,
                        onPlayVideo = { video, playlist ->
                            viewModel.playerManager.playVideo(video, playlist)
                            navController.navigate(Screen.Player.route)
                        },
                        onToggleFavorite = { viewModel.toggleFavorite(it) },
                        onAddToPlaylist = { videoToAddToPlaylist = it },
                        onShowVideoInfo = { infoVideo = it },
                        onClearAllFavorites = { viewModel.clearAllFavorites() }
                    )
                }

                composable(Screen.History.route) {
                    HistoryScreen(
                        history = watchHistory,
                        onPlayVideo = { video, playlist ->
                            viewModel.playerManager.playVideo(video, playlist)
                            navController.navigate(Screen.Player.route)
                        },
                        onRemoveFromHistory = { viewModel.removeFromHistory(it) },
                        onClearAllHistory = { viewModel.clearAllHistory() },
                        onToggleFavorite = { viewModel.toggleFavorite(it) },
                        onAddToPlaylist = { videoToAddToPlaylist = it },
                        onShowVideoInfo = { infoVideo = it }
                    )
                }

                composable(Screen.Search.route) {
                    SearchScreen(
                        query = searchQuery,
                        searchResults = searchResults,
                        recentSearches = recentSearches,
                        onQueryChange = { viewModel.setSearchQuery(it) },
                        onBack = { navController.popBackStack() },
                        onDeleteRecentSearch = { viewModel.deleteSearchQuery(it) },
                        onClearAllSearches = { viewModel.clearSearchHistory() },
                        onPlayVideo = { video, playlist ->
                            viewModel.playerManager.playVideo(video, playlist)
                            navController.navigate(Screen.Player.route)
                        },
                        onToggleFavorite = { viewModel.toggleFavorite(it) },
                        onAddToPlaylist = { videoToAddToPlaylist = it },
                        onShowVideoInfo = { infoVideo = it },
                        onDeleteVideo = { viewModel.deleteVideo(it) }
                    )
                }

                composable(Screen.Settings.route) {
                    SettingsScreen(
                        settings = userSettings,
                        onBack = { navController.popBackStack() },
                        onThemeChange = { viewModel.setTheme(it) },
                        onDefaultSpeedChange = { viewModel.setDefaultSpeed(it) },
                        onDoubleTapSeekChange = { viewModel.setDoubleTapSeek(it) },
                        onGesturesToggle = { viewModel.setGesturesEnabled(it) },
                        onSwipeBrightnessToggle = { viewModel.setSwipeBrightness(it) },
                        onSwipeVolumeToggle = { viewModel.setSwipeVolume(it) },
                        onSwipeSeekToggle = { viewModel.setSwipeSeek(it) },
                        onBackgroundAudioToggle = { viewModel.setBackgroundAudio(it) },
                        onHwDecoderToggle = { viewModel.setHardwareDecoder(it) },
                        onSaveHistoryToggle = { viewModel.setSaveHistory(it) },
                        onRescanLibrary = { viewModel.scanLibrary() },
                        onClearWatchHistory = { viewModel.clearAllHistory() },
                        onClearSearchHistory = { viewModel.clearSearchHistory() }
                    )
                }

                composable(Screen.Player.route) {
                    PlayerScreen(
                        playerManager = viewModel.playerManager,
                        settings = userSettings,
                        onBack = { navController.popBackStack() },
                        onEnterPiP = onEnterPiP
                    )
                }
            }
        }
    }

    // Modal dialogs
    if (showCreatePlaylistDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreatePlaylistDialog = false },
            onCreate = { name, desc ->
                viewModel.createPlaylist(name, desc)
                showCreatePlaylistDialog = false
            }
        )
    }

    videoToAddToPlaylist?.let { video ->
        AddToPlaylistDialog(
            video = video,
            playlists = playlists,
            onDismiss = { videoToAddToPlaylist = null },
            onSelectPlaylist = { playlist ->
                viewModel.addVideoToPlaylist(playlist.id, video)
                videoToAddToPlaylist = null
            },
            onCreateNewPlaylist = {
                videoToAddToPlaylist = null
                showCreatePlaylistDialog = true
            }
        )
    }

    infoVideo?.let { video ->
        VideoInfoBottomSheet(
            video = video,
            onDismiss = { infoVideo = null }
        )
    }
}
