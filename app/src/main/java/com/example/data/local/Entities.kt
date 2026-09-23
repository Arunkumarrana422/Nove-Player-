package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "videos")
data class VideoEntity(
    @PrimaryKey val id: String,
    val title: String,
    val uri: String,
    val path: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val width: Int = 0,
    val height: Int = 0,
    val mimeType: String = "video/mp4",
    val dateAdded: Long = System.currentTimeMillis(),
    val lastPlayedTimestamp: Long = 0L,
    val lastPositionMs: Long = 0L,
    val isFavorite: Boolean = false,
    val isOnline: Boolean = false,
    val folderName: String = "Internal",
    val folderPath: String = "",
    val videoCodec: String = "H.264",
    val audioCodec: String = "AAC",
    val containerFormat: String = "MP4",
    val isCompleted: Boolean = false
)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val videoCount: Int = 0,
    val thumbnailUri: String = ""
)

@Entity(tableName = "playlist_items", primaryKeys = ["playlistId", "videoId"])
data class PlaylistItemEntity(
    val playlistId: Long,
    val videoId: String,
    val orderIndex: Int = 0,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "search_history")
data class SearchHistoryEntity(
    @PrimaryKey val query: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "folder_preferences")
data class FolderPreferenceEntity(
    @PrimaryKey val folderPath: String,
    val folderName: String,
    val isHidden: Boolean = false,
    val isExcluded: Boolean = false
)
