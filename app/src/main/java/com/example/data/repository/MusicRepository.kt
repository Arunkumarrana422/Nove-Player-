package com.example.data.repository

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import com.example.domain.model.AudioAlbum
import com.example.domain.model.AudioArtist
import com.example.domain.model.AudioFolder
import com.example.domain.model.AudioPlaylist
import com.example.domain.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File

class MusicRepository(private val context: Context) {

    private val _allSongs = MutableStateFlow<List<Song>>(emptyList())
    val allSongsFlow: Flow<List<Song>> = _allSongs.asStateFlow()

    private val _favoriteSongIds = MutableStateFlow<Set<String>>(emptySet())
    val favoriteSongIdsFlow: Flow<Set<String>> = _favoriteSongIds.asStateFlow()

    private val _audioPlaylists = MutableStateFlow<List<AudioPlaylist>>(emptyList())
    val audioPlaylistsFlow: Flow<List<AudioPlaylist>> = _audioPlaylists.asStateFlow()

    // Sample fallback tracks with high quality royalty-free demo streams so users can test immediately
    private val sampleSongs = listOf(
        Song(
            id = "demo_song_1",
            title = "Midnight Synthwave Drift",
            artist = "Neon Horizon",
            album = "Retrowave Nights",
            durationMs = 215000L,
            sizeBytes = 6800000L,
            uri = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
            dataPath = "/Music/Retrowave/Midnight_Synthwave.mp3",
            folderName = "Retrowave",
            genre = "Electronic",
            bitRate = "320 kbps",
            year = 2026
        ),
        Song(
            id = "demo_song_2",
            title = "Acoustic Breeze & Rain",
            artist = "Clara Evans",
            album = "Unplugged Journeys",
            durationMs = 184000L,
            sizeBytes = 5900000L,
            uri = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3",
            dataPath = "/Music/Acoustic/Acoustic_Breeze.mp3",
            folderName = "Acoustic",
            genre = "Acoustic Pop",
            bitRate = "320 kbps",
            year = 2025
        ),
        Song(
            id = "demo_song_3",
            title = "Cyber Beat Pulse",
            artist = "Kavinsky Soundlab",
            album = "Neon Metropolis",
            durationMs = 248000L,
            sizeBytes = 7900000L,
            uri = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3",
            dataPath = "/Music/Electronic/Cyber_Beat_Pulse.mp3",
            folderName = "Electronic",
            genre = "Cyberpunk / EDM",
            bitRate = "320 kbps",
            year = 2026
        ),
        Song(
            id = "demo_song_4",
            title = "Peaceful Piano Echoes",
            artist = "Evelyn Ross",
            album = "Midnight Solitude",
            durationMs = 196000L,
            sizeBytes = 5200000L,
            uri = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3",
            dataPath = "/Music/Classical/Peaceful_Piano.mp3",
            folderName = "Classical",
            genre = "Piano / Ambient",
            bitRate = "320 kbps",
            year = 2025
        ),
        Song(
            id = "demo_song_5",
            title = "Deep Groove Funk & Bass",
            artist = "The Rhythm Collective",
            album = "Urban Soul Vol. 1",
            durationMs = 230000L,
            sizeBytes = 7100000L,
            uri = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-8.mp3",
            dataPath = "/Music/Funk/Deep_Groove_Funk.mp3",
            folderName = "Funk & Soul",
            genre = "Funk / Jazz",
            bitRate = "320 kbps",
            year = 2026
        )
    )

    suspend fun scanLocalMusic(): List<Song> = withContext(Dispatchers.IO) {
        val songsList = mutableListOf<Song>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.TRACK
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 OR ${MediaStore.Audio.Media.DURATION} > 5000"
        val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"

        try {
            val cursor: Cursor? = context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                sortOrder
            )

            cursor?.use {
                val idCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = it.getColumnIndex(MediaStore.Audio.Media.TITLE)
                val nameCol = it.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME)
                val artistCol = it.getColumnIndex(MediaStore.Audio.Media.ARTIST)
                val albumCol = it.getColumnIndex(MediaStore.Audio.Media.ALBUM)
                val albumIdCol = it.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)
                val durCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val sizeCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                val dataCol = it.getColumnIndex(MediaStore.Audio.Media.DATA)
                val dateCol = it.getColumnIndex(MediaStore.Audio.Media.DATE_ADDED)
                val yearCol = it.getColumnIndex(MediaStore.Audio.Media.YEAR)
                val trackCol = it.getColumnIndex(MediaStore.Audio.Media.TRACK)

                val favs = _favoriteSongIds.value

                while (it.moveToNext()) {
                    val id = it.getLong(idCol)
                    val contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                    val displayName = if (nameCol >= 0) it.getString(nameCol) ?: "Track_$id" else "Track_$id"
                    val title = if (titleCol >= 0) it.getString(titleCol)?.takeIf { t -> t.isNotBlank() } ?: displayName else displayName
                    val artist = if (artistCol >= 0) it.getString(artistCol)?.takeIf { a -> a.isNotBlank() && a != "<unknown>" } ?: "Unknown Artist" else "Unknown Artist"
                    val album = if (albumCol >= 0) it.getString(albumCol)?.takeIf { al -> al.isNotBlank() && al != "<unknown>" } ?: "Unknown Album" else "Unknown Album"
                    val albumId = if (albumIdCol >= 0) it.getLong(albumIdCol) else 0L
                    val duration = it.getLong(durCol)
                    val size = it.getLong(sizeCol)
                    val path = if (dataCol >= 0) it.getString(dataCol) ?: contentUri.toString() else contentUri.toString()
                    val dateAdded = if (dateCol >= 0) it.getLong(dateCol) * 1000L else System.currentTimeMillis()
                    val year = if (yearCol >= 0) it.getInt(yearCol) else 0
                    val track = if (trackCol >= 0) it.getInt(trackCol) else 0

                    val folderName = try {
                        File(path).parentFile?.name ?: "Music"
                    } catch (e: Exception) {
                        "Music"
                    }

                    val songId = "audio_$id"

                    val song = Song(
                        id = songId,
                        title = title.substringBeforeLast('.'),
                        artist = artist,
                        album = album,
                        albumId = albumId,
                        durationMs = duration,
                        sizeBytes = size,
                        uri = contentUri.toString(),
                        dataPath = path,
                        isFavorite = favs.contains(songId),
                        dateAdded = dateAdded,
                        folderName = folderName,
                        trackNumber = track,
                        year = year
                    )
                    songsList.add(song)
                }
            }
        } catch (e: Exception) {
            Log.e("MusicRepository", "Error querying MediaStore audio", e)
        }

        val finalSongs = if (songsList.isNotEmpty()) {
            songsList
        } else {
            val favs = _favoriteSongIds.value
            sampleSongs.map { it.copy(isFavorite = favs.contains(it.id)) }
        }

        _allSongs.value = finalSongs
        finalSongs
    }

    fun toggleFavorite(song: Song) {
        val currentFavs = _favoriteSongIds.value.toMutableSet()
        val newFav = if (currentFavs.contains(song.id)) {
            currentFavs.remove(song.id)
            false
        } else {
            currentFavs.add(song.id)
            true
        }
        _favoriteSongIds.value = currentFavs

        _allSongs.value = _allSongs.value.map {
            if (it.id == song.id) it.copy(isFavorite = newFav) else it
        }
    }

    fun createPlaylist(name: String, description: String = "") {
        val newId = (System.currentTimeMillis())
        val newPlaylist = AudioPlaylist(
            id = newId,
            name = name,
            description = description,
            songCount = 0,
            songs = emptyList()
        )
        _audioPlaylists.value = _audioPlaylists.value + newPlaylist
    }

    fun addSongToPlaylist(playlistId: Long, song: Song) {
        _audioPlaylists.value = _audioPlaylists.value.map { pl ->
            if (pl.id == playlistId) {
                if (pl.songs.none { it.id == song.id }) {
                    val updated = pl.songs + song
                    pl.copy(songs = updated, songCount = updated.size)
                } else pl
            } else pl
        }
    }

    fun removeSongFromPlaylist(playlistId: Long, songId: String) {
        _audioPlaylists.value = _audioPlaylists.value.map { pl ->
            if (pl.id == playlistId) {
                val updated = pl.songs.filterNot { it.id == songId }
                pl.copy(songs = updated, songCount = updated.size)
            } else pl
        }
    }

    fun deletePlaylist(playlistId: Long) {
        _audioPlaylists.value = _audioPlaylists.value.filterNot { it.id == playlistId }
    }
}
