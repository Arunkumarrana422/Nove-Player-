package com.example

import android.Manifest
import android.app.Activity
import android.app.NotificationManager
import android.app.PictureInPictureParams
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Rational
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.core.content.ContextCompat
import androidx.activity.result.IntentSenderRequest
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.sp
import com.example.ui.theme.NovaAccent
import kotlinx.coroutines.delay
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.domain.model.Playlist
import com.example.domain.model.Song
import com.example.domain.model.ThemePreference
import com.example.domain.model.Video
import com.example.ui.components.AddToPlaylistDialog
import com.example.ui.components.CreatePlaylistDialog
import com.example.ui.components.DeleteVideoDialog
import com.example.ui.components.FullMusicPlayerBottomSheet
import com.example.ui.components.MiniAudioPlayerBar
import com.example.ui.components.MiniPlayerView
import com.example.ui.components.NovaAppBar
import com.example.ui.components.VideoInfoBottomSheet
import com.example.ui.navigation.BottomNavItems
import com.example.ui.navigation.Screen
import com.example.ui.screens.FavoritesScreen
import com.example.ui.screens.FoldersScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MusicScreen
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
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    private val storagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.any { it }
        viewModel.setStoragePermission(granted)
    }

    private val mediaObserver = object : android.database.ContentObserver(android.os.Handler(android.os.Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            super.onChange(selfChange, uri)
            viewModel.scanLibrary()
        }
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

    override fun onResume() {
        super.onResume()
        viewModel.audioPlayerManager.isAppInForeground = true
        viewModel.playerManager.isAppInForeground = true
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        try {
            notificationManager.cancel(1001)
            notificationManager.cancel(1002)
        } catch (_: Exception) {}
        try {
            contentResolver.registerContentObserver(
                android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                true,
                mediaObserver
            )
        } catch (_: Exception) {}
        if (viewModel.hasStoragePermission.value) {
            viewModel.scanLibrary()
        }
    }

    override fun onPause() {
        super.onPause()
        try {
            contentResolver.unregisterContentObserver(mediaObserver)
        } catch (_: Exception) {}

        // Enforce background audio playback preference
        val bgAudio = viewModel.userSettings.value.backgroundAudioEnabled
        if (!bgAudio) {
            val isPip = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) isInPictureInPictureMode else false
            if (!isPip) {
                if (viewModel.playerManager.isPlaying.value) {
                    viewModel.playerManager.pause()
                }
                if (viewModel.audioPlayerManager.isPlaying.value) {
                    viewModel.audioPlayerManager.pause()
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        viewModel.audioPlayerManager.isAppInForeground = false
        viewModel.playerManager.isAppInForeground = false
        val bgAudio = viewModel.userSettings.value.backgroundAudioEnabled
        if (bgAudio) {
            if (viewModel.audioPlayerManager.isPlaying.value) {
                viewModel.audioPlayerManager.updateMediaNotification()
            }
            if (viewModel.playerManager.isPlaying.value) {
                viewModel.playerManager.updateVideoNotification()
            }
        } else {
            val isPip = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) isInPictureInPictureMode else false
            if (!isPip) {
                if (viewModel.playerManager.isPlaying.value) {
                    viewModel.playerManager.pause()
                }
                if (viewModel.audioPlayerManager.isPlaying.value) {
                    viewModel.audioPlayerManager.pause()
                }
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
                val videoTitle = getDisplayNameFromUri(this, uri)
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

    private fun getDisplayNameFromUri(context: Context, uri: Uri): String {
        var displayName: String? = null
        if (uri.scheme == "content") {
            try {
                context.contentResolver.query(
                    uri,
                    arrayOf(android.provider.OpenableColumns.DISPLAY_NAME),
                    null,
                    null,
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1) {
                            displayName = cursor.getString(nameIndex)
                        }
                    }
                }
            } catch (_: Exception) {}
        }
        if (displayName.isNullOrBlank()) {
            displayName = uri.lastPathSegment
        }
        if (displayName.isNullOrBlank() || displayName == "media" || displayName!!.matches(Regex("^\\d+$"))) {
            try {
                val projection = arrayOf(android.provider.MediaStore.Video.Media.DISPLAY_NAME)
                context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(android.provider.MediaStore.Video.Media.DISPLAY_NAME)
                        if (idx != -1) {
                            displayName = cursor.getString(idx)
                        }
                    }
                }
            } catch (_: Exception) {}
        }
        if (displayName.isNullOrBlank() || displayName == "media" || displayName!!.matches(Regex("^\\d+$"))) {
            displayName = uri.path?.let { path ->
                val lastSlash = path.lastIndexOf('/')
                if (lastSlash != -1 && lastSlash < path.length - 1) path.substring(lastSlash + 1) else null
            } ?: "External Video"
        }
        return displayName!!
    }

    private fun checkAndRequestPermissions() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_AUDIO,
                Manifest.permission.POST_NOTIFICATIONS
            )
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
        val bgAudio = viewModel.userSettings.value.backgroundAudioEnabled
        if (bgAudio) {
            if (viewModel.playerManager.isPlaying.value && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                enterPiPMode()
            }
        } else {
            // Strictly pause if background audio playback is turned off
            if (viewModel.playerManager.isPlaying.value) {
                viewModel.playerManager.pause()
            }
            if (viewModel.audioPlayerManager.isPlaying.value) {
                viewModel.audioPlayerManager.pause()
            }
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

    // Music states
    val allSongs by viewModel.allSongs.collectAsState()
    val audioPlaylists by viewModel.audioPlaylists.collectAsState()
    val currentSongPlaying by viewModel.audioPlayerManager.currentSong.collectAsState()
    val isAudioPlaying by viewModel.audioPlayerManager.isPlaying.collectAsState()
    val isFullPlayerOpen by viewModel.audioPlayerManager.isFullPlayerOpen.collectAsState()

    // Active Player Manager states
    val currentVideoPlaying by viewModel.playerManager.currentVideo.collectAsState()
    val isPlaying by viewModel.playerManager.isPlaying.collectAsState()
    val currentPosMs by viewModel.playerManager.currentPositionMs.collectAsState()
    val durationMs by viewModel.playerManager.durationMs.collectAsState()

    // Dialog & BottomSheet state
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var videoToAddToPlaylist by remember { mutableStateOf<Video?>(null) }
    var infoVideo by remember { mutableStateOf<Video?>(null) }
    var videoToDelete by remember { mutableStateOf<Video?>(null) }

    // Double back to exit state
    var lastBackPressTime by remember { mutableLongStateOf(0L) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val needManageStorage by viewModel.needManageStorage.collectAsState()
    LaunchedEffect(needManageStorage) {
        if (needManageStorage && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            viewModel.showToast("Please allow All Files Access once to delete files directly")
            try {
                val intent = Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                try {
                    val intent = Intent(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    context.startActivity(intent)
                } catch (_: Exception) {}
            }
            viewModel.clearManageStorageFlag()
        }
    }

    val sharedPrefs = remember { context.getSharedPreferences("nova_prefs", Context.MODE_PRIVATE) }
    val onboardingDone = remember { sharedPrefs.getBoolean("onboarding_completed", false) || userSettings.onboardingCompleted }
    val startDestination = if (onboardingDone) Screen.MainTabs.route else Screen.Onboarding.route

    // Main 4 tabs Pager (Default start at Folders = index 2)
    val pagerState = rememberPagerState(initialPage = 2) { 4 }

    val isPlayerScreen = currentRoute == Screen.Player.route
    val isMainTabs = currentRoute == Screen.MainTabs.route || currentRoute == null
    val showBottomBar = isMainTabs
    val showTopBar = isMainTabs

    val isAtMainRoot = isMainTabs && selectedFolder == null && selectedPlaylist == null

    // Double back press to exit
    BackHandler(enabled = isAtMainRoot) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastBackPressTime < 2000L) {
            (context as? Activity)?.finish()
        } else {
            lastBackPressTime = currentTime
            viewModel.showToast("Press back again to exit")
        }
    }

    fun requestDeleteVideo(videoId: String) {
        val target = allVideos.find { it.id == videoId }
            ?: onlineVideos.find { it.id == videoId }
            ?: watchHistory.find { it.id == videoId }
            ?: continueWatching.find { it.id == videoId }
        if (target != null) {
            videoToDelete = target
        } else {
            viewModel.deleteVideo(videoId)
        }
    }

    Scaffold(
        topBar = {
            if (showTopBar) {
                val topTitle = when (pagerState.currentPage) {
                    0 -> "Arvexa Player"
                    1 -> "Video Library"
                    2 -> "Folders"
                    3 -> "Playlists"
                    else -> "Arvexa Player"
                }
                NovaAppBar(
                    title = topTitle,
                    isRefreshing = isScanning,
                    onSearchClick = { navController.navigate(Screen.Search.route) },
                    onRefreshClick = { viewModel.scanLibrary() },
                    onSettingsClick = { navController.navigate(Screen.Settings.route) }
                )
            }
        },
        bottomBar = {
            if (showBottomBar) {
                Column {
                    // Floating MiniPlayer bar when playing video
                    if (currentVideoPlaying != null) {
                        MiniPlayerView(
                            video = currentVideoPlaying,
                            isPlaying = isPlaying,
                            currentPosMs = currentPosMs,
                            durationMs = durationMs,
                            exoPlayer = viewModel.playerManager.exoPlayer,
                            onExpand = {
                                navController.navigate(Screen.Player.route)
                            },
                            onPlayPause = {
                                viewModel.playerManager.togglePlayPause()
                            },
                            onClose = {
                                viewModel.playerManager.stopAndDismiss()
                            }
                        )
                    } else if (currentSongPlaying != null) {
                        // Floating MiniAudioPlayer bar when playing music
                        MiniAudioPlayerBar(
                            audioPlayerManager = viewModel.audioPlayerManager,
                            onExpand = {
                                viewModel.audioPlayerManager.openFullPlayer()
                            }
                        )
                    }

                    val indicatorColor = NovaAccent.copy(alpha = 0.2f)

                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        tonalElevation = 6.dp,
                        modifier = Modifier.testTag("bottom_navigation_bar")
                    ) {
                        BottomNavItems.forEachIndexed { index, item ->
                            val selected = if (item.route == Screen.Music.route) currentRoute == Screen.Music.route else (pagerState.currentPage == index && currentRoute == Screen.MainTabs.route)
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    if (item.route == Screen.Music.route) {
                                        navController.navigate(Screen.Music.route) {
                                            launchSingleTop = true
                                        }
                                    } else {
                                        if (item.route == Screen.Folders.route) {
                                            viewModel.selectFolder(null)
                                        } else if (item.route == Screen.Playlists.route) {
                                            viewModel.selectPlaylist(null)
                                        }
                                        if (currentRoute != Screen.MainTabs.route) {
                                            navController.popBackStack(Screen.MainTabs.route, false)
                                        }
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(index)
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
                                    selectedIconColor = NovaAccent,
                                    selectedTextColor = NovaAccent,
                                    indicatorColor = indicatorColor,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
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
                            sharedPrefs.edit().putBoolean("onboarding_completed", true).apply()
                            viewModel.setOnboardingCompleted(true)
                            navController.navigate(Screen.MainTabs.route) {
                                popUpTo(Screen.Onboarding.route) { inclusive = true }
                            }
                        }
                    )
                }

                composable(Screen.MainTabs.route) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                        userScrollEnabled = true
                    ) { page ->
                        when (page) {
                            0 -> HomeScreen(
                                continueWatching = continueWatching,
                                recentVideos = allVideos,
                                featuredStreams = onlineVideos,
                                folders = folders,
                                playlists = playlists,
                                allVideosCount = allVideos.size,
                                currentPlayingVideoId = currentVideoPlaying?.id,
                                isPlaying = isPlaying,
                                currentPosMs = currentPosMs,
                                onPlayVideo = { video, playlist ->
                                    viewModel.playerManager.playVideo(video, playlist)
                                    navController.navigate(Screen.Player.route)
                                },
                                onNavigateToMusic = {
                                    navController.navigate(Screen.Music.route)
                                },
                                onNavigateToVideos = {
                                    coroutineScope.launch { pagerState.animateScrollToPage(1) }
                                },
                                onNavigateToFolders = {
                                    coroutineScope.launch { pagerState.animateScrollToPage(2) }
                                },
                                onNavigateToPlaylists = {
                                    coroutineScope.launch { pagerState.animateScrollToPage(3) }
                                },
                                onNavigateToFavorites = { navController.navigate(Screen.Favorites.route) },
                                onNavigateToHistory = { navController.navigate(Screen.History.route) },
                                onToggleFavorite = { viewModel.toggleFavorite(it) },
                                onAddToPlaylist = { videoToAddToPlaylist = it },
                                onShowVideoInfo = { infoVideo = it },
                                onDeleteVideo = { requestDeleteVideo(it) }
                            )

                            1 -> VideosScreen(
                                videos = allVideos,
                                sortOption = userSettings.sortOption,
                                viewMode = userSettings.viewMode,
                                isScanning = isScanning,
                                currentPlayingVideoId = currentVideoPlaying?.id,
                                isPlaying = isPlaying,
                                currentPosMs = currentPosMs,
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
                                onDeleteVideo = { requestDeleteVideo(it) }
                            )

                            2 -> FoldersScreen(
                                folders = folders,
                                selectedFolder = selectedFolder,
                                currentPlayingVideoId = currentVideoPlaying?.id,
                                isPlaying = isPlaying,
                                currentPosMs = currentPosMs,
                                durationMs = durationMs,
                                currentPlayingVideo = currentVideoPlaying,
                                onSelectFolder = { viewModel.selectFolder(it) },
                                onPlayVideo = { video, playlist ->
                                    viewModel.playerManager.playVideo(video, playlist)
                                    navController.navigate(Screen.Player.route)
                                },
                                onToggleFavorite = { viewModel.toggleFavorite(it) },
                                onAddToPlaylist = { videoToAddToPlaylist = it },
                                onShowVideoInfo = { infoVideo = it },
                                onDeleteVideo = { requestDeleteVideo(it) }
                            )

                            3 -> PlaylistsScreen(
                                playlists = playlists,
                                selectedPlaylist = selectedPlaylist,
                                playlistVideosFlow = { id -> viewModel.getPlaylistVideos(id) },
                                currentPlayingVideoId = currentVideoPlaying?.id,
                                isPlaying = isPlaying,
                                currentPosMs = currentPosMs,
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
                    }
                }

                composable(Screen.Music.route) {
                    MusicScreen(
                        songs = allSongs,
                        playlists = audioPlaylists,
                        currentPlayingSongId = currentSongPlaying?.id,
                        isPlaying = isAudioPlaying,
                        onPlaySong = { song, queue ->
                            viewModel.audioPlayerManager.playSong(song, queue)
                            viewModel.audioPlayerManager.openFullPlayer()
                        },
                        onToggleFavorite = { viewModel.toggleFavoriteSong(it) },
                        onAddToPlaylist = { song ->
                            if (audioPlaylists.isNotEmpty()) {
                                viewModel.addSongToAudioPlaylist(audioPlaylists.first().id, song)
                                viewModel.showToast("Added to playlist")
                            } else {
                                viewModel.createAudioPlaylist("My Music Playlist")
                            }
                        },
                        onCreatePlaylist = { name ->
                            viewModel.createAudioPlaylist(name)
                        },
                        onRemoveFromPlaylist = { plId, songId ->
                            viewModel.removeSongFromAudioPlaylist(plId, songId)
                        },
                        onDeletePlaylist = { plId ->
                            viewModel.deleteAudioPlaylist(plId)
                        },
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.Favorites.route) {
                    FavoritesScreen(
                        favorites = favoriteVideos,
                        currentPlayingVideoId = currentVideoPlaying?.id,
                        isPlaying = isPlaying,
                        currentPosMs = currentPosMs,
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
                        currentPlayingVideoId = currentVideoPlaying?.id,
                        isPlaying = isPlaying,
                        currentPosMs = currentPosMs,
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
                        currentPlayingVideoId = currentVideoPlaying?.id,
                        isPlaying = isPlaying,
                        currentPosMs = currentPosMs,
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
                        onDeleteVideo = { requestDeleteVideo(it) }
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
                        onFontSizeChange = { viewModel.setSubtitleFontSize(it) },
                        onColorChange = { text, bg -> viewModel.setSubtitleColors(text, bg) },
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

    videoToDelete?.let { video ->
        DeleteVideoDialog(
            video = video,
            onDismiss = { videoToDelete = null },
            onConfirm = { deleteFromFileSystem ->
                viewModel.deleteVideo(video, deleteFromFileSystem)
                videoToDelete = null
                viewModel.showToast(
                    if (deleteFromFileSystem) "Video permanently deleted from storage" else "Video removed from library"
                )
            }
        )
    }

    infoVideo?.let { video ->
        VideoInfoBottomSheet(
            video = video,
            onDismiss = { infoVideo = null }
        )
    }

    if (isFullPlayerOpen && currentSongPlaying != null) {
        FullMusicPlayerBottomSheet(
            audioPlayerManager = viewModel.audioPlayerManager,
            onDismiss = { viewModel.audioPlayerManager.closeFullPlayer() }
        )
    }

    // Animated Pill Toast Overlay across the entire app
    val toastMsg by viewModel.toastMessage.collectAsState()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = if (showBottomBar && currentVideoPlaying != null) 140.dp else 80.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        AnimatedVisibility(
            visible = toastMsg != null,
            enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn() + scaleIn(initialScale = 0.9f),
            exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut() + scaleOut(targetScale = 0.9f)
        ) {
            if (toastMsg != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xEE0B1020))
                        .border(1.dp, NovaAccent.copy(alpha = 0.6f), RoundedCornerShape(50))
                        .padding(horizontal = 24.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = toastMsg ?: "",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                LaunchedEffect(toastMsg) {
                    delay(2000L)
                    viewModel.clearToast()
                }
            }
        }
    }
}
