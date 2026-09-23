package com.example.data.repository

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import com.example.data.datasource.SampleMediaSource
import com.example.data.local.NovaDatabase
import com.example.data.local.PlaylistEntity
import com.example.data.local.PlaylistItemEntity
import com.example.data.local.SearchHistoryEntity
import com.example.data.local.VideoEntity
import com.example.domain.model.Playlist
import com.example.domain.model.SortOption
import com.example.domain.model.Video
import com.example.domain.model.VideoFolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File

class VideoRepository(private val context: Context) {
    private val database = NovaDatabase.getDatabase(context)
    private val videoDao = database.videoDao()
    private val playlistDao = database.playlistDao()
    private val searchDao = database.searchHistoryDao()
    private val folderDao = database.folderDao()

    suspend fun initializeDatabase() = withContext(Dispatchers.IO) {
        val existingOnline = videoDao.getOnlineVideos().firstOrNull()
        if (existingOnline.isNullOrEmpty()) {
            val sampleEntities = SampleMediaSource.sampleOnlineVideos.map { it.toEntity() }
            videoDao.insertVideos(sampleEntities)
        }

        // Create default playlists if none exist
        val playlists = playlistDao.getAllPlaylists().firstOrNull()
        if (playlists.isNullOrEmpty()) {
            val favPlaylistId = playlistDao.insertPlaylist(
                PlaylistEntity(
                    name = "Quick Favorites",
                    description = "Your favorite videos and streams",
                    thumbnailUri = SampleMediaSource.sampleOnlineVideos.first().uri
                )
            )
            val demoPlaylistId = playlistDao.insertPlaylist(
                PlaylistEntity(
                    name = "Movie Trailers",
                    description = "High quality 4K and Full HD open-source film trailers",
                    thumbnailUri = SampleMediaSource.sampleOnlineVideos.getOrNull(1)?.uri ?: ""
                )
            )
            SampleMediaSource.sampleOnlineVideos.take(3).forEachIndexed { index, video ->
                playlistDao.addVideoToPlaylist(
                    PlaylistItemEntity(
                        playlistId = demoPlaylistId,
                        videoId = video.id,
                        orderIndex = index
                    )
                )
            }
        }
    }

    suspend fun scanLocalVideos(): List<Video> = withContext(Dispatchers.IO) {
        val videos = mutableListOf<Video>()
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.TITLE,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME
        )

        val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"
        val queryUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI

        try {
            val cursor: Cursor? = context.contentResolver.query(
                queryUri,
                projection,
                null,
                null,
                sortOrder
            )

            cursor?.use {
                val idCol = it.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameCol = it.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val titleCol = it.getColumnIndex(MediaStore.Video.Media.TITLE)
                val durCol = it.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                val sizeCol = it.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val widthCol = it.getColumnIndex(MediaStore.Video.Media.WIDTH)
                val heightCol = it.getColumnIndex(MediaStore.Video.Media.HEIGHT)
                val mimeCol = it.getColumnIndex(MediaStore.Video.Media.MIME_TYPE)
                val dateCol = it.getColumnIndex(MediaStore.Video.Media.DATE_ADDED)
                val dataCol = it.getColumnIndex(MediaStore.Video.Media.DATA)
                val bucketCol = it.getColumnIndex(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)

                while (it.moveToNext()) {
                    val id = it.getLong(idCol)
                    val contentUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                    val displayName = it.getString(nameCol) ?: "Video_$id"
                    val title = if (titleCol >= 0) it.getString(titleCol) ?: displayName else displayName
                    val duration = it.getLong(durCol)
                    val size = it.getLong(sizeCol)
                    val width = if (widthCol >= 0) it.getInt(widthCol) else 0
                    val height = if (heightCol >= 0) it.getInt(heightCol) else 0
                    val mime = if (mimeCol >= 0) it.getString(mimeCol) ?: "video/mp4" else "video/mp4"
                    val dateAdded = if (dateCol >= 0) it.getLong(dateCol) * 1000L else System.currentTimeMillis()
                    val path = if (dataCol >= 0) it.getString(dataCol) ?: contentUri.toString() else contentUri.toString()
                    val folderName = if (bucketCol >= 0) it.getString(bucketCol) ?: "Internal Storage" else "Internal Storage"
                    val folderPath = try {
                        File(path).parent ?: folderName
                    } catch (e: Exception) {
                        folderName
                    }

                    val ext = displayName.substringAfterLast('.', "MP4").uppercase()

                    val video = Video(
                        id = "local_$id",
                        title = title.ifBlank { displayName },
                        uri = contentUri.toString(),
                        path = path,
                        durationMs = duration,
                        sizeBytes = size,
                        width = width,
                        height = height,
                        mimeType = mime,
                        dateAdded = dateAdded,
                        isOnline = false,
                        folderName = folderName,
                        folderPath = folderPath,
                        containerFormat = ext
                    )
                    videos.add(video)
                }
            }

            if (videos.isNotEmpty()) {
                val entities = videos.map { it.toEntity() }
                videoDao.insertVideos(entities)
            }
        } catch (e: Exception) {
            Log.e("VideoRepository", "Error scanning local videos", e)
        }

        videos
    }

    fun getAllVideosFlow(): Flow<List<Video>> {
        return videoDao.getAllVideos().map { list -> list.map { it.toDomain() } }
    }

    fun getFavoriteVideosFlow(): Flow<List<Video>> {
        return videoDao.getFavoriteVideos().map { list -> list.map { it.toDomain() } }
    }

    fun getWatchHistoryFlow(): Flow<List<Video>> {
        return videoDao.getWatchHistory().map { list -> list.map { it.toDomain() } }
    }

    fun getContinueWatchingFlow(): Flow<List<Video>> {
        return videoDao.getContinueWatching().map { list -> list.map { it.toDomain() } }
    }

    fun getOnlineVideosFlow(): Flow<List<Video>> {
        return videoDao.getOnlineVideos().map { list -> list.map { it.toDomain() } }
    }

    suspend fun getVideoById(id: String): Video? = withContext(Dispatchers.IO) {
        videoDao.getVideoById(id)?.toDomain()
    }

    suspend fun updatePlaybackProgress(id: String, positionMs: Long, durationMs: Long) = withContext(Dispatchers.IO) {
        val isCompleted = durationMs > 0 && positionMs >= (durationMs * 0.95)
        videoDao.updatePlaybackProgress(
            id = id,
            positionMs = if (isCompleted) 0L else positionMs,
            timestamp = System.currentTimeMillis(),
            isCompleted = isCompleted
        )
    }

    suspend fun toggleFavorite(video: Video) = withContext(Dispatchers.IO) {
        val newFav = !video.isFavorite
        videoDao.setFavorite(video.id, newFav)
    }

    suspend fun deleteVideo(id: String) = withContext(Dispatchers.IO) {
        videoDao.deleteVideo(id)
    }

    suspend fun removeFromHistory(id: String) = withContext(Dispatchers.IO) {
        videoDao.removeFromHistory(id)
    }

    suspend fun clearAllHistory() = withContext(Dispatchers.IO) {
        videoDao.clearAllHistory()
    }

    suspend fun clearAllFavorites() = withContext(Dispatchers.IO) {
        videoDao.clearAllFavorites()
    }

    suspend fun addOnlineVideo(title: String, url: String): Video = withContext(Dispatchers.IO) {
        val ext = url.substringAfterLast('.', "").take(5).uppercase()
        val format = if (url.contains(".m3u8", ignoreCase = true)) "HLS" else if (url.contains(".mpd", ignoreCase = true)) "DASH" else if (ext.isNotEmpty()) ext else "STREAM"
        val id = "online_${System.currentTimeMillis()}"
        val video = Video(
            id = id,
            title = title.ifBlank { "Stream: ${Uri.parse(url).host ?: url}" },
            uri = url,
            path = url,
            durationMs = 0L,
            sizeBytes = 0L,
            mimeType = if (format == "HLS") "application/x-mpegURL" else "video/mp4",
            isOnline = true,
            folderName = "Online Streams",
            containerFormat = format
        )
        videoDao.insertVideo(video.toEntity())
        video
    }

    fun searchVideos(query: String): Flow<List<Video>> {
        return videoDao.searchVideos(query).map { list -> list.map { it.toDomain() } }
    }

    // Playlists
    fun getPlaylistsFlow(): Flow<List<Playlist>> {
        return playlistDao.getAllPlaylists().map { list ->
            list.map {
                Playlist(
                    id = it.id,
                    name = it.name,
                    description = it.description,
                    createdAt = it.createdAt,
                    videoCount = it.videoCount,
                    thumbnailUri = it.thumbnailUri
                )
            }
        }
    }

    suspend fun createPlaylist(name: String, description: String = ""): Long = withContext(Dispatchers.IO) {
        playlistDao.insertPlaylist(
            PlaylistEntity(
                name = name,
                description = description
            )
        )
    }

    suspend fun deletePlaylist(id: Long) = withContext(Dispatchers.IO) {
        playlistDao.deletePlaylist(id)
    }

    suspend fun addVideoToPlaylist(playlistId: Long, video: Video) = withContext(Dispatchers.IO) {
        val count = playlistDao.getPlaylistVideoCount(playlistId)
        playlistDao.addVideoToPlaylist(
            PlaylistItemEntity(
                playlistId = playlistId,
                videoId = video.id,
                orderIndex = count
            )
        )
        // Update playlist thumbnail & count
        val p = playlistDao.getPlaylistById(playlistId)
        if (p != null) {
            val thumb = if (p.thumbnailUri.isBlank()) video.uri else p.thumbnailUri
            playlistDao.updatePlaylist(p.copy(videoCount = count + 1, thumbnailUri = thumb))
        }
    }

    suspend fun removeVideoFromPlaylist(playlistId: Long, videoId: String) = withContext(Dispatchers.IO) {
        playlistDao.removeVideoFromPlaylist(playlistId, videoId)
        val p = playlistDao.getPlaylistById(playlistId)
        if (p != null) {
            val count = (p.videoCount - 1).coerceAtLeast(0)
            playlistDao.updatePlaylist(p.copy(videoCount = count))
        }
    }

    fun getVideosForPlaylist(playlistId: Long): Flow<List<Video>> {
        return playlistDao.getVideosForPlaylist(playlistId).map { list -> list.map { it.toDomain() } }
    }

    // Search History
    fun getRecentSearches(): Flow<List<String>> = searchDao.getRecentSearches()

    suspend fun addSearchQuery(query: String) = withContext(Dispatchers.IO) {
        if (query.isNotBlank()) {
            searchDao.insertSearch(SearchHistoryEntity(query.trim()))
        }
    }

    suspend fun deleteSearchQuery(query: String) = withContext(Dispatchers.IO) {
        searchDao.deleteSearch(query)
    }

    suspend fun clearSearchHistory() = withContext(Dispatchers.IO) {
        searchDao.clearSearchHistory()
    }
}

fun Video.toEntity() = VideoEntity(
    id = id,
    title = title,
    uri = uri,
    path = path,
    durationMs = durationMs,
    sizeBytes = sizeBytes,
    width = width,
    height = height,
    mimeType = mimeType,
    dateAdded = dateAdded,
    lastPlayedTimestamp = lastPlayedTimestamp,
    lastPositionMs = lastPositionMs,
    isFavorite = isFavorite,
    isOnline = isOnline,
    folderName = folderName,
    folderPath = folderPath,
    videoCodec = videoCodec,
    audioCodec = audioCodec,
    containerFormat = containerFormat,
    isCompleted = isCompleted
)

fun VideoEntity.toDomain() = Video(
    id = id,
    title = title,
    uri = uri,
    path = path,
    durationMs = durationMs,
    sizeBytes = sizeBytes,
    width = width,
    height = height,
    mimeType = mimeType,
    dateAdded = dateAdded,
    lastPlayedTimestamp = lastPlayedTimestamp,
    lastPositionMs = lastPositionMs,
    isFavorite = isFavorite,
    isOnline = isOnline,
    folderName = folderName,
    folderPath = folderPath,
    videoCodec = videoCodec,
    audioCodec = audioCodec,
    containerFormat = containerFormat,
    isCompleted = isCompleted
)
