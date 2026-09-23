package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoDao {
    @Query("SELECT * FROM videos ORDER BY dateAdded DESC")
    fun getAllVideos(): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos WHERE isFavorite = 1 ORDER BY dateAdded DESC")
    fun getFavoriteVideos(): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos WHERE lastPlayedTimestamp > 0 ORDER BY lastPlayedTimestamp DESC")
    fun getWatchHistory(): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos WHERE lastPositionMs > 0 AND isCompleted = 0 ORDER BY lastPlayedTimestamp DESC")
    fun getContinueWatching(): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos WHERE isOnline = 1 ORDER BY dateAdded DESC")
    fun getOnlineVideos(): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos WHERE id = :id LIMIT 1")
    suspend fun getVideoById(id: String): VideoEntity?

    @Query("SELECT * FROM videos WHERE uri = :uri LIMIT 1")
    suspend fun getVideoByUri(uri: String): VideoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideo(video: VideoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideos(videos: List<VideoEntity>)

    @Update
    suspend fun updateVideo(video: VideoEntity)

    @Query("UPDATE videos SET lastPositionMs = :positionMs, lastPlayedTimestamp = :timestamp, isCompleted = :isCompleted WHERE id = :id")
    suspend fun updatePlaybackProgress(id: String, positionMs: Long, timestamp: Long, isCompleted: Boolean)

    @Query("UPDATE videos SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun setFavorite(id: String, isFavorite: Boolean)

    @Query("DELETE FROM videos WHERE id = :id")
    suspend fun deleteVideo(id: String)

    @Query("UPDATE videos SET lastPlayedTimestamp = 0, lastPositionMs = 0 WHERE id = :id")
    suspend fun removeFromHistory(id: String)

    @Query("UPDATE videos SET lastPlayedTimestamp = 0, lastPositionMs = 0")
    suspend fun clearAllHistory()

    @Query("UPDATE videos SET isFavorite = 0")
    suspend fun clearAllFavorites()

    @Query("SELECT * FROM videos WHERE title LIKE '%' || :query || '%' OR folderName LIKE '%' || :query || '%' ORDER BY title ASC")
    fun searchVideos(query: String): Flow<List<VideoEntity>>
}

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists WHERE id = :id LIMIT 1")
    suspend fun getPlaylistById(id: Long): PlaylistEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Update
    suspend fun updatePlaylist(playlist: PlaylistEntity)

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun deletePlaylist(id: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addVideoToPlaylist(item: PlaylistItemEntity)

    @Query("DELETE FROM playlist_items WHERE playlistId = :playlistId AND videoId = :videoId")
    suspend fun removeVideoFromPlaylist(playlistId: Long, videoId: String)

    @Query("""
        SELECT v.* FROM videos v
        INNER JOIN playlist_items pi ON v.id = pi.videoId
        WHERE pi.playlistId = :playlistId
        ORDER BY pi.orderIndex ASC
    """)
    fun getVideosForPlaylist(playlistId: Long): Flow<List<VideoEntity>>

    @Query("SELECT COUNT(*) FROM playlist_items WHERE playlistId = :playlistId")
    suspend fun getPlaylistVideoCount(playlistId: Long): Int
}

@Dao
interface SearchHistoryDao {
    @Query("SELECT query FROM search_history ORDER BY timestamp DESC LIMIT 20")
    fun getRecentSearches(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSearch(search: SearchHistoryEntity)

    @Query("DELETE FROM search_history WHERE query = :query")
    suspend fun deleteSearch(query: String)

    @Query("DELETE FROM search_history")
    suspend fun clearSearchHistory()
}

@Dao
interface FolderDao {
    @Query("SELECT * FROM folder_preferences")
    fun getAllFolderPreferences(): Flow<List<FolderPreferenceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setFolderPreference(folderPref: FolderPreferenceEntity)

    @Query("DELETE FROM folder_preferences WHERE folderPath = :folderPath")
    suspend fun deleteFolderPreference(folderPath: String)
}
