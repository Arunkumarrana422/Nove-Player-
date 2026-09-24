package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.Video
import com.example.domain.model.VideoFolder
import com.example.ui.components.EmptyStateView
import com.example.ui.components.FolderCard
import com.example.ui.components.VideoCard
import com.example.ui.theme.NovaAccent
import com.example.ui.theme.NovaPrimary
import com.example.ui.theme.NovaSecondary

@Composable
fun FoldersScreen(
    folders: List<VideoFolder>,
    selectedFolder: VideoFolder?,
    currentPlayingVideoId: String? = null,
    isPlaying: Boolean = false,
    currentPosMs: Long = 0L,
    durationMs: Long = 0L,
    currentPlayingVideo: Video? = null,
    onSelectFolder: (VideoFolder?) -> Unit,
    onPlayVideo: (Video, List<Video>) -> Unit,
    onToggleFavorite: (Video) -> Unit,
    onAddToPlaylist: (Video) -> Unit,
    onShowVideoInfo: (Video) -> Unit,
    onDeleteVideo: (String) -> Unit
) {
    if (selectedFolder != null) {
        BackHandler {
            onSelectFolder(null)
        }

        // Folder Detail View
        Column(
            modifier = Modifier
                .fillMaxSize()
                .testTag("folder_detail_view")
        ) {
            // Folder Breadcrumb Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { onSelectFolder(null) },
                    modifier = Modifier.testTag("folder_back_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to Folders",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = selectedFolder.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                    Text(
                        text = "${selectedFolder.videoCount} videos • ${selectedFolder.totalDurationFormatted}",
                        style = MaterialTheme.typography.bodySmall,
                        color = NovaAccent
                    )
                }

                Button(
                    onClick = {
                        if (selectedFolder.videos.isNotEmpty()) {
                            onPlayVideo(selectedFolder.videos.first(), selectedFolder.videos)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NovaPrimary),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Play All", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(selectedFolder.videos, key = { it.id }) { video ->
                    VideoCard(
                        video = video,
                        onClick = { onPlayVideo(video, selectedFolder.videos) },
                        isCurrentlyPlaying = (currentPlayingVideoId == video.id && isPlaying),
                        onToggleFavorite = { onToggleFavorite(video) },
                        onAddToPlaylist = { onAddToPlaylist(video) },
                        onShowInfo = { onShowVideoInfo(video) },
                        onDelete = { onDeleteVideo(video.id) }
                    )
                }
            }
        }
    } else {
        // Folder Overview List
        if (folders.isEmpty()) {
            EmptyStateView(
                icon = Icons.Default.Folder,
                title = "No Video Folders",
                description = "Your device has no media folders discovered yet."
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("folders_screen"),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(folders, key = { it.name }) { folder ->
                    val isFolderPlaying = isPlaying && folder.videos.any { it.id == currentPlayingVideoId }
                    FolderCard(
                        folder = folder,
                        onClick = { onSelectFolder(folder) },
                        isCurrentlyPlaying = isFolderPlaying,
                        playingVideoTitle = if (isFolderPlaying) currentPlayingVideo?.title else null,
                        playingPosMs = if (isFolderPlaying) currentPosMs else 0L,
                        playingDurMs = if (isFolderPlaying) durationMs else 0L
                    )
                }
            }
        }
    }
}
