package com.example.ui.components

import android.content.ContentUris
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.domain.model.Song
import com.example.domain.model.Video
import com.example.player.AudioPlayerManager

@Composable
fun IosMusicWidgetView(
    audioPlayerManager: AudioPlayerManager,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier
) {
    val song by audioPlayerManager.currentSong.collectAsState()
    val isPlaying by audioPlayerManager.isPlaying.collectAsState()
    val currentPositionMs by audioPlayerManager.currentPositionMs.collectAsState()
    val durationMs by audioPlayerManager.durationMs.collectAsState()

    if (song == null) return

    val currentSong = song!!
    val effectiveDur = if (durationMs > 0) durationMs else currentSong.durationMs.coerceAtLeast(1L)
    val remainingMs = (effectiveDur - currentPositionMs).coerceAtLeast(0L)

    var isDragging by remember { mutableStateOf(false) }
    var dragPosition by remember { mutableFloatStateOf(0f) }

    val sliderPosition = if (isDragging) dragPosition else currentPositionMs.toFloat()

    val albumArtUri = remember(currentSong.albumId) {
        ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), currentSong.albumId)
    }

    Card(
        onClick = onExpand,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF141922)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Top Row: Album Art, Title/Artist, Visualizer Icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Album Art Thumbnail
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF232A38)),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = albumArtUri,
                        contentDescription = "Album Art",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Title & Artist
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = currentSong.title,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        ),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = currentSong.artist,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 13.sp
                        ),
                        color = Color(0xFF9BA1AF),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Audio visualizer / waveform icon
                Icon(
                    imageVector = Icons.Default.GraphicEq,
                    contentDescription = "Visualizer",
                    tint = Color(0xFF9BA1AF),
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Middle Section: Progress Slider & Time stamps
            Column(modifier = Modifier.fillMaxWidth()) {
                Slider(
                    value = sliderPosition,
                    onValueChange = { newVal ->
                        isDragging = true
                        dragPosition = newVal
                    },
                    onValueChangeFinished = {
                        isDragging = false
                        audioPlayerManager.seekTo(dragPosition.toLong())
                    },
                    valueRange = 0f..effectiveDur.toFloat(),
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.White,
                        inactiveTrackColor = Color(0xFF323B4C)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(20.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = Video.formatDuration(currentPositionMs),
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = Color(0xFF9BA1AF)
                    )
                    Text(
                        text = "-${Video.formatDuration(remainingMs)}",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = Color(0xFF9BA1AF)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Bottom Controls Row: Rewind, Play/Pause, FastForward, Audio Output device
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Skip Previous
                IconButton(
                    onClick = { audioPlayerManager.previousSong() },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Play / Pause
                IconButton(
                    onClick = { audioPlayerManager.togglePlayPause() },
                    modifier = Modifier
                        .size(56.dp)
                        .background(Color.White, CircleShape)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color(0xFF141922),
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Skip Next
                IconButton(
                    onClick = { audioPlayerManager.nextSong() },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Output Device icon (AirPods / Headphones)
                IconButton(
                    onClick = { /* Audio route info */ },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Headphones,
                        contentDescription = "Audio Output",
                        tint = Color(0xFF9BA1AF),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}
