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
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PlayerScreen(
    playerManager: NovaPlayerManager,
    settings: UserSettings,
    onFontSizeChange: (Int) -> Unit,
    onColorChange: (String, String) -> Unit,
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
        playerManager.setBrightness(settings.videoBrightness, activity)
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
        if (hudState !is GestureHudState.None && hudState !is GestureHudState.SpeedBoost && !isSeekingGesture) {
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
            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        // Apply saved video player brightness
        playerManager.applyPlayerBrightness(activity)

        onDispose {
            val win = activity?.window
            if (win != null) {
                val insetsController = WindowCompat.getInsetsController(win, win.decorView)
                insetsController.show(WindowInsetsCompat.Type.systemBars())
                win.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
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
                },
            contentAlignment = Alignment.Center
        ) {
            val viewModifier = when (aspectRatioMode) {
                AspectRatioMode.FIT, AspectRatioMode.FILL_CROP, AspectRatioMode.STRETCH, AspectRatioMode.ORIGINAL -> Modifier.fillMaxSize()
                AspectRatioMode.RATIO_16_9 -> Modifier.fillMaxWidth().aspectRatio(16f / 9f)
                AspectRatioMode.RATIO_4_3 -> Modifier.fillMaxWidth().aspectRatio(4f / 3f)
                AspectRatioMode.RATIO_21_9 -> Modifier.fillMaxWidth().aspectRatio(21f / 9f)
            }

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
                modifier = viewModifier
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
                .pointerInput(isLocked, settings.gesturesEnabled) {
                    if (isLocked) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val downTime = System.currentTimeMillis()
                            while (true) {
                                val event = awaitPointerEvent()
                                val active = event.changes.filter { it.pressed }
                                if (active.isEmpty()) {
                                    if (System.currentTimeMillis() - downTime < 300) {
                                        areControlsVisible = !areControlsVisible
                                    }
                                    break
                                }
                            }
                        }
                        return@pointerInput
                    }

                    var pendingSingleTapJob: Job? = null
                    var doubleTapResetJob: Job? = null
                    var isDoubleTapSequenceActive = false
                    var doubleTapSequenceSideIsRight = true
                    var cumulativeDoubleTapSeconds = 0
                    var lastTapTime = 0L
                    var lastTapOffset = Offset.Zero
                    var lastTapIsCenter = false

                    awaitEachGesture {
                        val firstDown = awaitFirstDown(requireUnconsumed = false)
                        val startTime = System.currentTimeMillis()
                        val startPos = firstDown.position
                        var isMultiTouch = false
                        var isDragging = false
                        var isDragHorizontal = false
                        var totalDragX = 0f
                        var totalDragY = 0f
                        var isLongPressActive = false
                        var originalSpeed = 1f
                        val isLeft = startPos.x < size.width / 2
                        var anyPointerConsumed = firstDown.isConsumed

                        val longPressJob = scope.launch {
                            delay(400L)
                            if (!isDragging && !isMultiTouch && !isLongPressActive && !anyPointerConsumed) {
                                isLongPressActive = true
                                originalSpeed = playbackSpeed
                                playerManager.setSpeed(2.0f)
                                hudState = GestureHudState.SpeedBoost(2.0f)
                            }
                        }

                        var previousCentroid = Offset.Zero
                        var previousDistance = 0f

                        while (true) {
                            val event = awaitPointerEvent()
                            val activePointers = event.changes.filter { it.pressed }

                            if (!isDragging && !isMultiTouch && !isLongPressActive && event.changes.any { it.isConsumed }) {
                                anyPointerConsumed = true
                            }

                            if (activePointers.isEmpty()) {
                                // All fingers lifted
                                longPressJob.cancel()
                                if (isLongPressActive) {
                                    isLongPressActive = false
                                    playerManager.setSpeed(originalSpeed)
                                    hudState = GestureHudState.None
                                } else if (isMultiTouch) {
                                    if (zoomScale <= 1.05f) {
                                        zoomScale = 1f
                                        panOffsetX = 0f
                                        panOffsetY = 0f
                                    }
                                } else if (isDragging) {
                                    if (isSeekingGesture) {
                                        playerManager.seekBy(seekDragDeltaMs)
                                        isSeekingGesture = false
                                        hudState = GestureHudState.None
                                    }
                                    if (playbackSpeed > 1.5f && hudState is GestureHudState.SpeedBoost) {
                                        playerManager.setSpeed(settings.defaultSpeed)
                                        hudState = GestureHudState.None
                                    }
                                } else if (!anyPointerConsumed) {
                                    // Check if it was a Tap or Double Tap
                                    val duration = System.currentTimeMillis() - startTime
                                    val moveDistSq = totalDragX * totalDragX + totalDragY * totalDragY
                                    if (duration < 350 && moveDistSq < 600f) {
                                        val now = System.currentTimeMillis()
                                        val timeSinceLastTap = now - lastTapTime
                                        val distFromLastTap = (startPos - lastTapOffset).getDistance()
                                        val isCenter = startPos.x >= size.width * 0.33f && startPos.x <= size.width * 0.67f
                                        val isRightSide = startPos.x > size.width / 2

                                        if (isCenter && lastTapIsCenter && settings.doubleTapPlayPauseEnabled && timeSinceLastTap < 350 && distFromLastTap < 140f) {
                                            // Center Double Tap -> Toggle Play / Pause!
                                            pendingSingleTapJob?.cancel()
                                            pendingSingleTapJob = null
                                            doubleTapResetJob?.cancel()
                                            isDoubleTapSequenceActive = false
                                            cumulativeDoubleTapSeconds = 0

                                            val willPlay = !isPlaying
                                            if (isPlaying) {
                                                playerManager.pause()
                                            } else {
                                                playerManager.play()
                                            }
                                            hudState = GestureHudState.PlayPause(willPlay)
                                            lastTapTime = 0L
                                            lastTapIsCenter = false

                                            doubleTapResetJob = scope.launch {
                                                delay(800L)
                                                if (hudState is GestureHudState.PlayPause) {
                                                    hudState = GestureHudState.None
                                                }
                                            }
                                        } else if (!isCenter && isDoubleTapSequenceActive && isRightSide == doubleTapSequenceSideIsRight && timeSinceLastTap < 800L) {
                                            // Subsequent tap in active multi-tap seek sequence (+20s, +30s, etc.)
                                            pendingSingleTapJob?.cancel()
                                            pendingSingleTapJob = null
                                            areControlsVisible = false

                                            val seekStep = settings.doubleTapSeekSeconds
                                            cumulativeDoubleTapSeconds += seekStep
                                            val seekDeltaMs = if (isRightSide) seekStep * 1000L else -seekStep * 1000L
                                            playerManager.seekBy(seekDeltaMs)
                                            hudState = GestureHudState.DoubleTapSeek(isRightSide, cumulativeDoubleTapSeconds)

                                            lastTapTime = now
                                            lastTapOffset = startPos
                                            lastTapIsCenter = false

                                            doubleTapResetJob?.cancel()
                                            doubleTapResetJob = scope.launch {
                                                delay(800L)
                                                isDoubleTapSequenceActive = false
                                                cumulativeDoubleTapSeconds = 0
                                                lastTapTime = 0L
                                            }
                                        } else if (!isCenter && timeSinceLastTap < 350 && distFromLastTap < 120f) {
                                            // Left / Right Double Tap detected (10s seek):
                                            // Cancel pending single tap immediately so player buttons NEVER appear
                                            pendingSingleTapJob?.cancel()
                                            pendingSingleTapJob = null
                                            areControlsVisible = false

                                            isDoubleTapSequenceActive = true
                                            doubleTapSequenceSideIsRight = isRightSide
                                            val seekStep = settings.doubleTapSeekSeconds
                                            cumulativeDoubleTapSeconds = seekStep
                                            val seekDeltaMs = if (isRightSide) seekStep * 1000L else -seekStep * 1000L
                                            playerManager.seekBy(seekDeltaMs)
                                            hudState = GestureHudState.DoubleTapSeek(isRightSide, cumulativeDoubleTapSeconds)

                                            lastTapTime = now
                                            lastTapOffset = startPos
                                            lastTapIsCenter = false

                                            doubleTapResetJob?.cancel()
                                            doubleTapResetJob = scope.launch {
                                                delay(800L)
                                                isDoubleTapSequenceActive = false
                                                cumulativeDoubleTapSeconds = 0
                                                lastTapTime = 0L
                                            }
                                        } else {
                                            // Single Tap candidate:
                                            // Reset any double-tap sequence
                                            isDoubleTapSequenceActive = false
                                            cumulativeDoubleTapSeconds = 0
                                            doubleTapResetJob?.cancel()

                                            // Wait 280ms before toggling controls so double tap has time to be detected
                                            lastTapTime = now
                                            lastTapOffset = startPos
                                            lastTapIsCenter = isCenter
                                            pendingSingleTapJob?.cancel()
                                            pendingSingleTapJob = scope.launch {
                                                delay(280L)
                                                areControlsVisible = !areControlsVisible
                                            }
                                        }
                                    }
                                }
                                break
                            }

                            if (activePointers.size >= 2) {
                                if (isLongPressActive) {
                                    isLongPressActive = false
                                    playerManager.setSpeed(originalSpeed)
                                    hudState = GestureHudState.None
                                }
                                // 2-FINGER PINCH TO ZOOM & PAN
                                isMultiTouch = true
                                val p1 = activePointers[0].position
                                val p2 = activePointers[1].position
                                val centroid = (p1 + p2) / 2f
                                val distance = (p1 - p2).getDistance()

                                if (previousDistance > 0f) {
                                    val scaleFactor = distance / previousDistance
                                    val newScale = (zoomScale * scaleFactor).coerceIn(1.0f, 4.5f)
                                    zoomScale = newScale

                                    if (newScale > 1.02f) {
                                        val maxPanX = (size.width * (zoomScale - 1f)) / 2f
                                        val maxPanY = (size.height * (zoomScale - 1f)) / 2f
                                        val panDelta = centroid - previousCentroid
                                        panOffsetX = (panOffsetX + panDelta.x).coerceIn(-maxPanX, maxPanX)
                                        panOffsetY = (panOffsetY + panDelta.y).coerceIn(-maxPanY, maxPanY)
                                    } else {
                                        zoomScale = 1f
                                        panOffsetX = 0f
                                        panOffsetY = 0f
                                    }
                                    hudState = GestureHudState.Zoom(zoomScale)
                                }

                                previousCentroid = centroid
                                previousDistance = distance
                                event.changes.forEach { it.consume() }

                            } else if (activePointers.size == 1 && !isMultiTouch && settings.gesturesEnabled) {
                                // 1-FINGER SWIPE / DRAG / LONG PRESS
                                val change = activePointers[0]
                                val dragAmount = change.positionChange()
                                totalDragX += dragAmount.x
                                totalDragY += dragAmount.y

                                val totalMoveSq = totalDragX * totalDragX + totalDragY * totalDragY
                                val duration = System.currentTimeMillis() - startTime

                                if (isLongPressActive) {
                                    change.consume()
                                } else {
                                    if (!isDragging && duration > 400L && totalMoveSq < 10000f) {
                                        isLongPressActive = true
                                        originalSpeed = playbackSpeed
                                        playerManager.setSpeed(2.0f)
                                        hudState = GestureHudState.SpeedBoost(2.0f)
                                        change.consume()
                                    } else if (totalMoveSq > 10000f) {
                                        if (!isDragging) {
                                            isDragging = true
                                            isDragHorizontal = kotlin.math.abs(totalDragX) > kotlin.math.abs(totalDragY)
                                        }
                                    }
                                }

                                if (isLongPressActive) {
                                    change.consume()
                                } else if (isDragging) {
                                    change.consume()
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
                            }
                        }
                    }
                }
        )

        // Top Toast HUD Overlay
        GestureOverlayIndicator(
            hudState = hudState,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 36.dp)
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

        // Arvexa Player Controls Overlay
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
            onFontSizeChange = { onFontSizeChange(it) },
            onColorChange = { text, bg -> onColorChange(text, bg) },
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
