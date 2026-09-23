package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.Video
import com.example.ui.components.EmptyStateView
import com.example.ui.components.VideoCard
import com.example.ui.theme.NovaAccent
import com.example.ui.theme.NovaPrimary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    query: String,
    searchResults: List<Video>,
    recentSearches: List<String>,
    onQueryChange: (String) -> Unit,
    onBack: () -> Unit,
    onDeleteRecentSearch: (String) -> Unit,
    onClearAllSearches: () -> Unit,
    onPlayVideo: (Video, List<Video>) -> Unit,
    onToggleFavorite: (Video) -> Unit,
    onAddToPlaylist: (Video) -> Unit,
    onShowVideoInfo: (Video) -> Unit,
    onDeleteVideo: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("search_screen")
    ) {
        // Search Input Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("search_back_button")) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }

            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = { Text("Search by title, format, or folder...") },
                singleLine = true,
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = NovaAccent)
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                shape = RoundedCornerShape(24.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedIndicatorColor = NovaAccent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("search_text_input")
            )
        }

        // Recent Searches when query is empty
        if (query.isBlank()) {
            if (recentSearches.isNotEmpty()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "RECENT SEARCHES",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(onClick = onClearAllSearches) {
                            Text("Clear all", color = Color(0xFFEF4444), fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        recentSearches.forEach { search ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { onQueryChange(search) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.History,
                                        contentDescription = null,
                                        tint = NovaAccent,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = search,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Delete",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clickable { onDeleteRecentSearch(search) }
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                EmptyStateView(
                    icon = Icons.Default.Search,
                    title = "Search Your Videos",
                    description = "Find any video file instantly by name, video resolution, or folder."
                )
            }
        } else {
            // Search Results
            if (searchResults.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.Search,
                    title = "No Matching Videos",
                    description = "We couldn't find any videos matching '$query'."
                )
            } else {
                Text(
                    text = "Found ${searchResults.size} matches for '$query'",
                    style = MaterialTheme.typography.bodySmall,
                    color = NovaAccent,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 90.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(searchResults, key = { it.id }) { video ->
                        VideoCard(
                            video = video,
                            onClick = { onPlayVideo(video, searchResults) },
                            onToggleFavorite = { onToggleFavorite(video) },
                            onAddToPlaylist = { onAddToPlaylist(video) },
                            onShowInfo = { onShowVideoInfo(video) },
                            onDelete = { onDeleteVideo(video.id) }
                        )
                    }
                }
            }
        }
    }
}
