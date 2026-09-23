package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.data.repository.UserSettings
import com.example.domain.model.AspectRatioMode
import com.example.domain.model.Video
import com.example.player.NovaPlayerManager
import com.example.ui.components.ContinueWatchingBanner
import com.example.ui.components.GestureHudState
import com.example.ui.components.GestureOverlayIndicator
import com.example.ui.components.PlaybackSettingsBottomSheet
import com.example.ui.components.PlayerControlsOverlay
import com.example.ui.components.SubtitleOverlay
import com.example.ui.components.SubtitleSettingsBottomSheet
import com.example.ui.theme.NovaPrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PlayerScreen(
    playerManager: NovaPlayerManager,
    settings: UserSettings,
    onBack: () -> Unit,
    onEnterPiP: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()

    val currentVideo by playerManager.currentVideo.collectAsState()
    val isPlaying by playerManager.isPlaying.collectAsState()
    val currentPosMs by playerManager.currentPositionMs.collectAsState()
    val durationMs by playerManager.durationMs.collectAsState()
    val bufferedPosMs by playerManager.bufferedPositionMs.collectAsState()
    val isBuffering by playerManager.isBuffering.collectAsState()
    val playbackSpeed by playerManager.playbackSpeed.collectAsState()
    val volumeFraction by playerManager.volumeFraction.collectAsState()
    val brightnessFraction by playerManager.brightnessFraction.collectAsState()
    val isLocked by playerManager.isLocked.collectAsState()
    val aspectRatioMode by playerManager.aspectRatioMode.collectAsState()
    val isRepeatOne by playerManager.isRepeatOne.collectAsState()
    val isShuffle by playerManager.isShuffle.collectAsState()
    val audioTracks by playerManager.availableAudioTracks.collectAsState()
    val subtitleTracks by playerManager.availableSubtitleTracks.collectAsState()
    val activeSubtitleText by playerManager.activeSubtitleText.collectAsState()
    val subtitleDelayMs by playerManager.subtitleDelayMs.collectAsState()
    val sleepTimerMinutes by playerManager.sleepTimerMinutes.collectAsState()
    val errorMessage by playerManager.errorMessage.collectAsState()

    var areControlsVisible by remember { mutableStateOf(true) }
    var hudState by remember { mutableStateOf<GestureHudState>(GestureHudState.None) }
    var showSubtitleSheet by remember { mutableStateOf(false) }
    var showPlaybackSheet by remember { mutableStateOf(false) }
    var isLandscape by remember { mutableStateOf(false) }

    var seekDragDeltaMs by remember { mutableLongStateOf(0L) }
    var isSeekingGesture by remember { mutableStateOf(false) }

    // 2-finger Pinch-to-Zoom & Pan state
    var zoomScale by remember { mutableFloatStateOf(1f) }
    var panOffsetX by remember { mutableFloatStateOf(0f) }
    var panOffsetY by remember { mutableFloatStateOf(0f) }

    val video = currentVideo

    LaunchedEffect(video?.id) {
        zoomScale = 1f
        panOffsetX = 0f
        panOffsetY = 0f
    }

    // Continue from where you stopped banner state
    var showResumeBanner by remember(video?.id) {
        val lastPos = video?.lastPositionMs ?: 0L
        val dur = video?.durationMs ?: 0L
        mutableStateOf(lastPos > 3000L && (dur == 0L || lastPos < dur - 5000L))
    }

    // Auto-dismiss resume banner after 6 seconds
    LaunchedEffect(showResumeBanner) {
        if (showResumeBanner) {
            delay(6000)
            showResumeBanner = false
        }
    }

    // Auto-rotation based on aspect ratio:
    // 9:16 (vertical/portrait video) -> no sensor auto rotation (locked to portrait)
    // 16:9 (horizontal video) -> sensor auto rotation enabled
    LaunchedEffect(video, isLocked) {
        if (isLocked) return@LaunchedEffect
        if (video != null) {
            val isPortraitVideo = (video.height > video.width && video.height > 0 && video.width > 0)
            activity?.requestedOrientation = if (isPortraitVideo) {
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            } else {
                ActivityInfo.SCREEN_ORIENTATION_SENSOR
            }
        }
    }

    // Auto-hide controls timer
    LaunchedEffect(areControlsVisible, isPlaying, isLocked) {
        if (areControlsVisible && isPlaying && !isLocked) {
            delay(settings.autoHideControlsSeconds * 1000L)
            areControlsVisible = false
        }
    }

    // Auto-dismiss HUD timer
    LaunchedEffect(hudState) {
        if (hudState !is GestureHudState.None && !isSeekingGesture) {
            delay(1200)
            hudState = GestureHudState.None
        }
    }

    // Immersive Fullscreen Mode & Brightness Management
    DisposableEffect(Unit) {
        val window = activity?.window
        if (window != null) {
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
        }
        // Apply saved video player brightness
        playerManager.applyPlayerBrightness(activity)

        onDispose {
            val win = activity?.window
            if (win != null) {
                val insetsController = WindowCompat.getInsetsController(win, win.decorView)
                insetsController.show(WindowInsetsCompat.Type.systemBars())
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                // Restore phone's automatic / default system brightness
                playerManager.restoreSystemBrightness(activity)
            }
            playerManager.flushProgress()
        }
    }

    BackHandler {
        if (isLocked) {
            playerManager.setLocked(false)
        } else {
            onBack()
        }
    }

    if (video == null) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Text("No video playing", color = Color.White)
        }
        return
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("player_screen_root")
    ) {
        val screenWidth = maxWidth
        val screenHeight = maxHeight

        // Media3 PlayerView inside Scalable Box for 2-finger zoom and pan
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = zoomScale
                    scaleY = zoomScale
                    translationX = panOffsetX
                    translationY = panOffsetY
                }
        ) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = playerManager.exoPlayer
                        useController = false
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                    }
                },
                update = { playerView ->
                    playerView.player = playerManager.exoPlayer
                    playerView.resizeMode = when (aspectRatioMode) {
                        AspectRatioMode.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                        AspectRatioMode.FILL_CROP -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        AspectRatioMode.STRETCH -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                        AspectRatioMode.ORIGINAL -> AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH
                        AspectRatioMode.RATIO_16_9,
                        AspectRatioMode.RATIO_4_3,
                        AspectRatioMode.RATIO_21_9 -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Subtitle Overlay
        SubtitleOverlay(
            subtitleText = activeSubtitleText,
            fontSizeSp = settings.subtitleFontSize,
            textColorHex = settings.subtitleTextColor,
            bgColorHex = settings.subtitleBgColor,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = if (areControlsVisible) 80.dp else 24.dp)
        )

        // Gesture Detection Overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(isLocked) {
                    if (isLocked) return@pointerInput
                    detectTransformGestures { _, pan, zoom, _ ->
                        if (zoom != 1f || zoomScale > 1.01f) {
                            val newScale = (zoomScale * zoom).coerceIn(1f, 4.5f)
                            zoomScale = newScale
                            if (newScale <= 1.02f) {
                                zoomScale = 1f
                                panOffsetX = 0f
                                panOffsetY = 0f
                            } else {
                                val maxPanX = (size.width * (zoomScale - 1f)) / 2f
                                val maxPanY = (size.height * (zoomScale - 1f)) / 2f
                                panOffsetX = (panOffsetX + pan.x * zoomScale).coerceIn(-maxPanX, maxPanX)
                                panOffsetY = (panOffsetY + pan.y * zoomScale).coerceIn(-maxPanY, maxPanY)
                            }
                            hudState = GestureHudState.Zoom(zoomScale)
                        }
                    }
                }
                .pointerInput(isLocked, settings.gesturesEnabled) {
                    if (isLocked) {
                        detectTapGestures(onTap = { areControlsVisible = !areControlsVisible })
                        return@pointerInput
                    }

                    detectTapGestures(
                        onTap = {
                            areControlsVisible = !areControlsVisible
                        },
                        onDoubleTap = { offset ->
                            if (zoomScale > 1.05f) {
                                zoomScale = 1f
                                panOffsetX = 0f
                                panOffsetY = 0f
                                hudState = GestureHudState.Zoom(1f)
                            } else {
                                val isRightSide = offset.x > size.width / 2
                                val seekDeltaMs = if (isRightSide) {
                                    settings.doubleTapSeekSeconds * 1000L
                                } else {
                                    -settings.doubleTapSeekSeconds * 1000L
                                }
                                playerManager.seekBy(seekDeltaMs)
                                hudState = GestureHudState.DoubleTapSeek(isRightSide, settings.doubleTapSeekSeconds)
                            }
                        },
                        onLongPress = {
                            // 2X Speed Boost while held
                            playerManager.setSpeed(2.0f)
                            hudState = GestureHudState.SpeedBoost(2.0f)
                        }
                    )
                }
                .pointerInput(isLocked, settings.gesturesEnabled) {
                    if (isLocked || !settings.gesturesEnabled) return@pointerInput

                    var totalDragX = 0f
                    var totalDragY = 0f
                    var isLeft = false
                    var isDragHorizontal = false

                    detectDragGestures(
                        onDragStart = { offset ->
                            totalDragX = 0f
                            totalDragY = 0f
                            isLeft = offset.x < size.width / 2
                            isDragHorizontal = false
                            isSeekingGesture = false
                            seekDragDeltaMs = 0L
                        },
                        onDragEnd = {
                            if (isSeekingGesture) {
                                playerManager.seekBy(seekDragDeltaMs)
                                isSeekingGesture = false
                                hudState = GestureHudState.None
                            }
                            if (playbackSpeed > 1.5f && hudState is GestureHudState.SpeedBoost) {
                                playerManager.setSpeed(settings.defaultSpeed)
                                hudState = GestureHudState.None
                            }
                        },
                        onDragCancel = {
                            isSeekingGesture = false
                            hudState = GestureHudState.None
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            totalDragX += dragAmount.x
                            totalDragY += dragAmount.y

                            if (!isDragHorizontal && kotlin.math.abs(totalDragX) > kotlin.math.abs(totalDragY) && kotlin.math.abs(totalDragX) > 20) {
                                isDragHorizontal = true
                            }

                            if (isDragHorizontal && settings.swipeSeekEnabled) {
                                isSeekingGesture = true
                                val seekRatio = totalDragX / size.width
                                val maxSeekSpan = (durationMs * 0.15f).coerceAtLeast(30000f)
                                seekDragDeltaMs = (seekRatio * maxSeekSpan).toLong()
                                val target = (currentPosMs + seekDragDeltaMs).coerceIn(0L, durationMs.coerceAtLeast(1L))
                                hudState = GestureHudState.Seek(target, seekDragDeltaMs, durationMs)
                            } else {
                                val deltaFraction = -dragAmount.y / (size.height * 0.75f)
                                if (isLeft && settings.swipeBrightnessEnabled) {
                                    playerManager.adjustBrightnessBy(deltaFraction, activity)
                                    hudState = GestureHudState.Brightness(playerManager.brightnessFraction.value)
                                } else if (!isLeft && settings.swipeVolumeEnabled) {
                                    playerManager.adjustVolumeBy(deltaFraction)
                                    hudState = GestureHudState.Volume(playerManager.volumeFraction.value)
                                }
                            }
                        }
                    )
                }
        )

        // Center HUD Overlay
        GestureOverlayIndicator(
            hudState = hudState,
            modifier = Modifier.align(Alignment.Center)
        )

        // Error Banner
        if (errorMessage != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp)
            ) {
                Box(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = errorMessage ?: "Playback Error",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        // Continue Watching Floating Banner (Photo 2)
        ContinueWatchingBanner(
            isVisible = showResumeBanner,
            onStartOver = {
                playerManager.seekTo(0L)
                showResumeBanner = false
            },
            onDismiss = {
                showResumeBanner = false
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = if (areControlsVisible) 115.dp else 24.dp)
        )

        // MX Player Controls Overlay
        PlayerControlsOverlay(
            isVisible = areControlsVisible,
            video = video,
            isPlaying = isPlaying,
            isBuffering = isBuffering,
            currentPositionMs = currentPosMs,
            durationMs = durationMs,
            bufferedPositionMs = bufferedPosMs,
            playbackSpeed = playbackSpeed,
            isLocked = isLocked,
            aspectRatioMode = aspectRatioMode,
            isRepeatOne = isRepeatOne,
            isShuffle = isShuffle,
            hwDecoderEnabled = settings.hardwareDecoderEnabled,
            hasSubtitles = subtitleTracks.isNotEmpty(),
            onBack = onBack,
            onPlayPause = { playerManager.togglePlayPause() },
            onSeekTo = { playerManager.seekTo(it) },
            onSeekDelta = { playerManager.seekBy(it) },
            onNext = { playerManager.nextVideo() },
            onPrevious = { playerManager.previousVideo() },
            onToggleLock = { playerManager.setLocked(!isLocked) },
            onCycleAspectRatio = { playerManager.cycleAspectRatio() },
            onOpenAudioSelector = { showPlaybackSheet = true },
            onOpenSubtitleSelector = { showSubtitleSheet = true },
            onOpenPlaybackSettings = { showPlaybackSheet = true },
            onToggleRepeat = { playerManager.toggleRepeat() },
            onToggleShuffle = { playerManager.toggleShuffle() },
            onToggleOrientation = {
                isLandscape = !isLandscape
                activity?.requestedOrientation = if (isLandscape) {
                    ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                } else {
                    ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                }
            }
        )
    }

    // Subtitle Settings Bottom Sheet
    if (showSubtitleSheet) {
        SubtitleSettingsBottomSheet(
            tracks = subtitleTracks,
            selectedTrack = subtitleTracks.firstOrNull { it.isSelected },
            subtitleDelayMs = subtitleDelayMs,
            fontSize = settings.subtitleFontSize,
            textColor = settings.subtitleTextColor,
            bgColor = settings.subtitleBgColor,
            onSelectTrack = { playerManager.selectSubtitleTrack(it) },
            onLoadExternalSubtitle = { uri ->
                scope.launch { playerManager.loadExternalSubtitle(uri) }
            },
            onAdjustDelay = { delta -> playerManager.adjustSubtitleDelay(delta) },
            onFontSizeChange = { /* Updated through MainViewModel in real-time */ },
            onColorChange = { _, _ -> },
            onDismiss = { showSubtitleSheet = false }
        )
    }

    // Playback Settings Bottom Sheet
    if (showPlaybackSheet) {
        PlaybackSettingsBottomSheet(
            currentSpeed = playbackSpeed,
            currentAspectRatio = aspectRatioMode,
            audioTracks = audioTracks,
            selectedAudioTrack = audioTracks.firstOrNull { it.isSelected },
            sleepTimerMinutes = sleepTimerMinutes,
            backgroundAudioEnabled = settings.backgroundAudioEnabled,
            hwDecoderEnabled = settings.hardwareDecoderEnabled,
            onSpeedChange = { playerManager.setSpeed(it) },
            onAspectRatioChange = { playerManager.setAspectRatio(it) },
            onSelectAudioTrack = { playerManager.selectAudioTrack(it) },
            onSleepTimerChange = { playerManager.setSleepTimer(it) },
            onBackgroundAudioToggle = { /* Handled in settings */ },
            onHwDecoderToggle = { /* Handled in settings */ },
            onEnterPiP = {
                showPlaybackSheet = false
                onEnterPiP()
            },
            onDismiss = { showPlaybackSheet = false }
        )
    }
}
