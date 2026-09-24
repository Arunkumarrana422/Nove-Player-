package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.AspectRatioMode
import com.example.domain.model.Video
import com.example.ui.theme.NovaAccent
import com.example.ui.theme.NovaPrimary
import com.example.ui.theme.NovaSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerControlsOverlay(
    isVisible: Boolean,
    video: Video,
    isPlaying: Boolean,
    isBuffering: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
    bufferedPositionMs: Long,
    playbackSpeed: Float,
    isLocked: Boolean,
    aspectRatioMode: AspectRatioMode,
    isRepeatOne: Boolean,
    isShuffle: Boolean,
    hwDecoderEnabled: Boolean,
    hasSubtitles: Boolean,
    onBack: () -> Unit,
    onPlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSeekDelta: (Long) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onToggleLock: () -> Unit,
    onCycleAspectRatio: () -> Unit,
    onOpenAudioSelector: () -> Unit,
    onOpenSubtitleSelector: () -> Unit,
    onOpenPlaybackSettings: () -> Unit,
    onToggleRepeat: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleOrientation: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isSeeking by remember { mutableStateOf(false) }
    var seekPosition by remember { mutableFloatStateOf(0f) }

    val effectivePos = if (isSeeking) seekPosition.toLong() else currentPositionMs
    val progress = if (durationMs > 0) (effectivePos.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
    val bufferedFraction = if (durationMs > 0) (bufferedPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f

    // If locked, show only the unlock button
    if (isLocked) {
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.TopStart
            ) {
                IconButton(
                    onClick = onToggleLock,
                    modifier = Modifier
                        .size(52.dp)
                        .background(Color(0xCC151C30), CircleShape)
                        .testTag("player_unlock_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Unlock Controls",
                        tint = NovaAccent,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
        return
    }

    // Buffering indicator always shown in center if buffering
    if (isBuffering && !isPlaying) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                color = NovaAccent,
                strokeWidth = 4.dp,
                modifier = Modifier.size(56.dp)
            )
        }
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xCC0B1020),
                            Color(0x33000000),
                            Color(0xCC0B1020)
                        )
                    )
                )
        ) {
            // TOP BAR
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("player_back_button")) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp)
                ) {
                    Text(
                        text = video.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (hwDecoderEnabled) "HW+" else "SW",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = if (hwDecoderEnabled) NovaAccent else Color(0xFFF59E0B),
                            fontSize = 11.sp
                        )
                        Text(
                            text = "• ${video.containerFormat}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xCCFFFFFF),
                            fontSize = 11.sp
                        )
                    }
                }

                // Top Actions
                IconButton(onClick = onOpenAudioSelector, modifier = Modifier.testTag("player_audio_btn")) {
                    Icon(Icons.Default.Audiotrack, contentDescription = "Audio Track", tint = Color.White)
                }

                IconButton(onClick = onOpenSubtitleSelector, modifier = Modifier.testTag("player_subtitles_btn")) {
                    Icon(
                        Icons.Default.Subtitles,
                        contentDescription = "Subtitles",
                        tint = if (hasSubtitles) NovaAccent else Color.White
                    )
                }

                IconButton(onClick = onCycleAspectRatio, modifier = Modifier.testTag("player_aspect_ratio_btn")) {
                    Icon(Icons.Default.AspectRatio, contentDescription = "Aspect Ratio", tint = Color.White)
                }

                IconButton(onClick = onToggleLock, modifier = Modifier.testTag("player_lock_btn")) {
                    Icon(Icons.Default.LockOpen, contentDescription = "Lock Controls", tint = Color.White)
                }

                IconButton(onClick = onOpenPlaybackSettings, modifier = Modifier.testTag("player_settings_btn")) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More Settings", tint = Color.White)
                }
            }

            // CENTER PLAYBACK CONTROLS
            Row(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Previous
                IconButton(
                    onClick = onPrevious,
                    modifier = Modifier.size(48.dp).testTag("player_prev_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Rewind 10s
                IconButton(
                    onClick = { onSeekDelta(-10000L) },
                    modifier = Modifier.size(52.dp).testTag("player_rewind_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.FastRewind,
                        contentDescription = "Rewind 10s",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Play / Pause Button with Glow
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(
                            Brush.linearGradient(listOf(NovaPrimary, NovaSecondary)),
                            shape = CircleShape
                        )
                        .clip(CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = onPlayPause,
                        modifier = Modifier.fillMaxSize().testTag("player_play_pause_btn")
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(42.dp)
                        )
                    }
                }

                // Fast Forward 10s
                IconButton(
                    onClick = { onSeekDelta(10000L) },
                    modifier = Modifier.size(52.dp).testTag("player_ffwd_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.FastForward,
                        contentDescription = "Fast Forward 10s",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Next
                IconButton(
                    onClick = onNext,
                    modifier = Modifier.size(48.dp).testTag("player_next_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            // BOTTOM BAR
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                // Seekbar Row with Timestamps (Custom design matching Photo 3)
                NovaPlayerSeekBar(
                    currentPositionMs = effectivePos,
                    durationMs = durationMs,
                    bufferedPositionMs = bufferedPositionMs,
                    onSeekTo = { pos ->
                        onSeekTo(pos)
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                // Bottom actions row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Repeat mode toggle
                        IconButton(onClick = onToggleRepeat, modifier = Modifier.size(36.dp)) {
                            Icon(
                                imageVector = if (isRepeatOne) Icons.Default.RepeatOne else Icons.Default.Repeat,
                                contentDescription = "Repeat",
                                tint = if (isRepeatOne) NovaAccent else Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Shuffle toggle
                        IconButton(onClick = onToggleShuffle, modifier = Modifier.size(36.dp)) {
                            Icon(
                                imageVector = Icons.Default.Shuffle,
                                contentDescription = "Shuffle",
                                tint = if (isShuffle) NovaAccent else Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Playback Speed badge
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0x33FFFFFF),
                            modifier = Modifier.clip(RoundedCornerShape(6.dp))
                        ) {
                            Text(
                                text = "${playbackSpeed}x",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        // Aspect ratio badge
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0x33FFFFFF),
                            modifier = Modifier.clip(RoundedCornerShape(6.dp))
                        ) {
                            Text(
                                text = aspectRatioMode.name,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // Rotate Screen / Fullscreen
                    IconButton(
                        onClick = onToggleOrientation,
                        modifier = Modifier.size(36.dp).testTag("player_orientation_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ScreenRotation,
                            contentDescription = "Rotate Screen",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}
