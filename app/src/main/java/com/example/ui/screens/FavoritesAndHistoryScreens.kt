package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.Video
import com.example.ui.components.EmptyStateView
import com.example.ui.components.VideoCard
import com.example.ui.theme.NovaPrimary

@Composable
fun FavoritesScreen(
    favorites: List<Video>,
    currentPlayingVideoId: String? = null,
    isPlaying: Boolean = false,
    onPlayVideo: (Video, List<Video>) -> Unit,
    onToggleFavorite: (Video) -> Unit,
    onAddToPlaylist: (Video) -> Unit,
    onShowVideoInfo: (Video) -> Unit,
    onClearAllFavorites: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("favorites_screen")
    ) {
        if (favorites.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${favorites.size} Favorite Videos",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Button(
                    onClick = { onPlayVideo(favorites.first(), favorites) },
                    colors = ButtonDefaults.buttonColors(containerColor = NovaPrimary),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Play All", fontSize = 12.sp)
                }
            }
        }

        if (favorites.isEmpty()) {
            EmptyStateView(
                icon = Icons.Default.Favorite,
                title = "No Favorite Videos",
                description = "Tap the heart icon on any video to add it to your quick-access favorites."
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 90.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(favorites, key = { it.id }) { video ->
                    VideoCard(
                        video = video,
                        onClick = { onPlayVideo(video, favorites) },
                        isCurrentlyPlaying = (currentPlayingVideoId == video.id && isPlaying),
                        onToggleFavorite = { onToggleFavorite(video) },
                        onAddToPlaylist = { onAddToPlaylist(video) },
                        onShowInfo = { onShowVideoInfo(video) },
                        onDelete = { onToggleFavorite(video) }
                    )
                }
            }
        }
    }
}

@Composable
fun HistoryScreen(
    history: List<Video>,
    currentPlayingVideoId: String? = null,
    isPlaying: Boolean = false,
    onPlayVideo: (Video, List<Video>) -> Unit,
    onRemoveFromHistory: (String) -> Unit,
    onClearAllHistory: () -> Unit,
    onToggleFavorite: (Video) -> Unit,
    onAddToPlaylist: (Video) -> Unit,
    onShowVideoInfo: (Video) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("history_screen")
    ) {
        if (history.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${history.size} Watched Videos",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                OutlinedButton(
                    onClick = onClearAllHistory,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFFEF4444))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clear All", fontSize = 12.sp, color = Color(0xFFEF4444))
                }
            }
        }

        if (history.isEmpty()) {
            EmptyStateView(
                icon = Icons.Default.History,
                title = "No Watch History",
                description = "Videos you watch will appear here with your saved playback progress."
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 90.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(history, key = { it.id }) { video ->
                    VideoCard(
                        video = video,
                        onClick = { onPlayVideo(video, history) },
                        isCurrentlyPlaying = (currentPlayingVideoId == video.id && isPlaying),
                        onToggleFavorite = { onToggleFavorite(video) },
                        onAddToPlaylist = { onAddToPlaylist(video) },
                        onShowInfo = { onShowVideoInfo(video) },
                        onDelete = { onRemoveFromHistory(video.id) }
                    )
                }
            }
        }
    }
}
