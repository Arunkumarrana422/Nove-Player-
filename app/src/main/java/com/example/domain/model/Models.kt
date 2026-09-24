package com.example.domain.model

data class Video(
    val id: String,
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
) {
    val durationFormatted: String
        get() = formatDuration(durationMs)

    val sizeFormatted: String
        get() = formatFileSize(sizeBytes)

    val resolutionFormatted: String
        get() = if (width > 0 && height > 0) "${width}x${height}" else if (width > 0) "${width}p" else "HD"

    val progressFraction: Float
        get() = if (durationMs > 0) (lastPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f

    val isPartiallyWatched: Boolean
        get() = lastPositionMs > 5000L && !isCompleted && progressFraction in 0.02f..0.95f

    companion object {
        fun formatDuration(ms: Long): String {
            if (ms <= 0) return "00:00"
            val totalSec = ms / 1000
            val sec = totalSec % 60
            val min = (totalSec / 60) % 60
            val hrs = totalSec / 3600
            return if (hrs > 0) {
                String.format("%d:%02d:%02d", hrs, min, sec)
            } else {
                String.format("%02d:%02d", min, sec)
            }
        }

        fun formatFileSize(bytes: Long): String {
            if (bytes <= 0) return "0 MB"
            val kb = bytes / 1024.0
            val mb = kb / 1024.0
            val gb = mb / 1024.0
            return when {
                gb >= 1.0 -> String.format("%.2f GB", gb)
                mb >= 1.0 -> String.format("%.1f MB", mb)
                else -> String.format("%.0f KB", kb)
            }
        }
    }
}

data class VideoFolder(
    val name: String,
    val path: String,
    val videoCount: Int,
    val totalDurationMs: Long,
    val videos: List<Video> = emptyList()
) {
    val totalDurationFormatted: String
        get() = Video.formatDuration(totalDurationMs)
}

data class Playlist(
    val id: Long = 0L,
    val name: String,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val videoCount: Int = 0,
    val thumbnailUri: String = ""
)

data class Song(
    val id: String,
    val title: String,
    val artist: String = "Unknown Artist",
    val album: String = "Unknown Album",
    val albumId: Long = 0L,
    val durationMs: Long = 0L,
    val sizeBytes: Long = 0L,
    val uri: String,
    val dataPath: String = "",
    val isFavorite: Boolean = false,
    val dateAdded: Long = System.currentTimeMillis(),
    val folderName: String = "Music",
    val trackNumber: Int = 0,
    val year: Int = 0,
    val genre: String = "Music",
    val bitRate: String = "320 kbps"
) {
    val durationFormatted: String
        get() = Video.formatDuration(durationMs)

    val sizeFormatted: String
        get() = Video.formatFileSize(sizeBytes)
}

data class AudioAlbum(
    val id: Long,
    val name: String,
    val artist: String,
    val songCount: Int,
    val songs: List<Song> = emptyList()
)

data class AudioArtist(
    val name: String,
    val songCount: Int,
    val albumCount: Int,
    val songs: List<Song> = emptyList()
)

data class AudioFolder(
    val name: String,
    val path: String,
    val songCount: Int,
    val totalDurationMs: Long,
    val songs: List<Song> = emptyList()
) {
    val totalDurationFormatted: String
        get() = Video.formatDuration(totalDurationMs)
}

data class AudioPlaylist(
    val id: Long = 0L,
    val name: String,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val songCount: Int = 0,
    val songs: List<Song> = emptyList()
)

enum class AudioRepeatMode(val label: String) {
    OFF("Repeat Off"),
    ALL("Repeat All"),
    ONE("Repeat Current")
}

data class EqualizerPreset(
    val id: String,
    val name: String,
    val bassGain: Float = 0f,
    val midGain: Float = 0f,
    val trebleGain: Float = 0f
)

data class SubtitleTrack(
    val id: String,
    val language: String,
    val label: String,
    val uri: String = "",
    val isExternal: Boolean = false,
    val isSelected: Boolean = false,
    val delayMs: Long = 0L
)

data class AudioTrack(
    val id: String,
    val label: String,
    val language: String,
    val isSelected: Boolean = false
)

enum class SortOption(val label: String) {
    DATE_DESC("Date (Newest First)"),
    DATE_ASC("Date (Oldest First)"),
    NAME_ASC("Title (A-Z)"),
    NAME_DESC("Title (Z-A)"),
    DURATION_DESC("Duration (Longest)"),
    SIZE_DESC("File Size (Largest)"),
    RECENTLY_PLAYED("Recently Played")
}

enum class ViewMode {
    LIST,
    GRID
}

enum class AspectRatioMode(val label: String) {
    FIT("Fit to Screen"),
    FILL_CROP("Crop to Fill"),
    STRETCH("Stretch"),
    ORIGINAL("100% Original"),
    RATIO_16_9("16:9"),
    RATIO_4_3("4:3"),
    RATIO_21_9("21:9 Cinema")
}

enum class ThemePreference(val label: String) {
    SYSTEM("System Default"),
    DARK("Deep Nova Dark"),
    LIGHT("Modern Light")
}
