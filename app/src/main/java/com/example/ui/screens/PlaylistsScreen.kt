package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.Playlist
import com.example.domain.model.Video
import com.example.ui.components.EmptyStateView
import com.example.ui.components.PlaylistCard
import com.example.ui.components.VideoCard
import com.example.ui.theme.NovaAccent
import com.example.ui.theme.NovaPrimary
import kotlinx.coroutines.flow.Flow

@Composable
fun PlaylistsScreen(
    playlists: List<Playlist>,
    selectedPlaylist: Playlist?,
    playlistVideosFlow: ((Long) -> Flow<List<Video>>)?,
    currentPlayingVideoId: String? = null,
    isPlaying: Boolean = false,
    onSelectPlaylist: (Playlist?) -> Unit,
    onCreatePlaylistClick: () -> Unit,
    onDeletePlaylist: (Long) -> Unit,
    onRemoveFromPlaylist: (Long, String) -> Unit,
    onPlayVideo: (Video, List<Video>) -> Unit,
    onToggleFavorite: (Video) -> Unit,
    onAddToPlaylist: (Video) -> Unit,
    onShowVideoInfo: (Video) -> Unit
) {
    if (selectedPlaylist != null && playlistVideosFlow != null) {
        val playlistVideos by playlistVideosFlow(selectedPlaylist.id).collectAsState(initial = emptyList())

        BackHandler {
            onSelectPlaylist(null)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .testTag("playlist_detail_screen")
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { onSelectPlaylist(null) }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = selectedPlaylist.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${playlistVideos.size} videos",
                        style = MaterialTheme.typography.bodySmall,
                        color = NovaAccent
                    )
                }
                IconButton(onClick = {
                    onDeletePlaylist(selectedPlaylist.id)
                    onSelectPlaylist(null)
                }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Playlist",
                        tint = Color(0xFFEF4444)
                    )
                }
            }

            // Play / Shuffle Row
            if (playlistVideos.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { onPlayVideo(playlistVideos.first(), playlistVideos) },
                        colors = ButtonDefaults.buttonColors(containerColor = NovaPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Play All")
                    }

                    OutlinedButton(
                        onClick = { onPlayVideo(playlistVideos.shuffled().first(), playlistVideos.shuffled()) },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Shuffle")
                    }
                }
            }

            if (playlistVideos.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.PlaylistPlay,
                    title = "Playlist is Empty",
                    description = "Add videos to '${selectedPlaylist.name}' from your video library or folders."
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(playlistVideos, key = { it.id }) { video ->
                        VideoCard(
                            video = video,
                            onClick = { onPlayVideo(video, playlistVideos) },
                            isCurrentlyPlaying = (currentPlayingVideoId == video.id && isPlaying),
                            onToggleFavorite = { onToggleFavorite(video) },
                            onAddToPlaylist = { onAddToPlaylist(video) },
                            onShowInfo = { onShowVideoInfo(video) },
                            onDelete = { onRemoveFromPlaylist(selectedPlaylist.id, video.id) }
                        )
                    }
                }
            }
        }
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .testTag("playlists_screen")
        ) {
            if (playlists.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.PlaylistPlay,
                    title = "No Playlists Yet",
                    description = "Create custom playlists to organize your favorite movies, clips, and series.",
                    actionButtonText = "Create Playlist",
                    onActionClick = onCreatePlaylistClick
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(playlists, key = { it.id }) { playlist ->
                        PlaylistCard(
                            playlist = playlist,
                            onClick = { onSelectPlaylist(playlist) },
                            onDelete = { onDeletePlaylist(playlist.id) }
                        )
                    }
                }
            }

            FloatingActionButton(
                onClick = onCreatePlaylistClick,
                containerColor = NovaPrimary,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 80.dp)
                    .testTag("create_playlist_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Playlist")
            }
        }
    }
}
