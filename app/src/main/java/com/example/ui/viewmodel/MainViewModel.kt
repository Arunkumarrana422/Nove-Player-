package com.example.ui.viewmodel

import android.app.Application
import android.content.IntentSender
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.MusicRepository
import com.example.data.repository.SettingsRepository
import com.example.data.repository.UserSettings
import com.example.data.repository.VideoRepository
import com.example.domain.model.AspectRatioMode
import com.example.domain.model.AudioPlaylist
import com.example.domain.model.Playlist
import com.example.domain.model.Song
import com.example.domain.model.SortOption
import com.example.domain.model.ThemePreference
import com.example.domain.model.Video
import com.example.domain.model.VideoFolder
import com.example.domain.model.ViewMode
import com.example.player.AudioPlayerManager
import com.example.player.NovaPlayerManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(application: Application) : AndroidViewModel(application) {
    val repository = VideoRepository(application)
    val musicRepository = MusicRepository(application)
    val settingsRepository = SettingsRepository(application)
    val playerManager = NovaPlayerManager(application)
    val audioPlayerManager = AudioPlayerManager(application)

    private val _needManageStorage = MutableStateFlow(false)
    val needManageStorage: StateFlow<Boolean> = _needManageStorage.asStateFlow()

    fun clearManageStorageFlag() {
        _needManageStorage.value = false
    }

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    fun showToast(message: String) {
        _toastMessage.value = message
    }

    fun clearToast() {
        _toastMessage.value = null
    }
    val userSettings: StateFlow<UserSettings> = settingsRepository.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = UserSettings()
    )

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _hasStoragePermission = MutableStateFlow(false)
    val hasStoragePermission: StateFlow<Boolean> = _hasStoragePermission.asStateFlow()

    init {
        // Stop video when audio starts playing
        audioPlayerManager.onAudioStarted = {
            if (playerManager.isPlaying.value) {
                playerManager.pause()
            }
        }
        // Stop audio when video starts playing
        playerManager.onVideoStarted = {
            if (audioPlayerManager.isPlaying.value) {
                audioPlayerManager.pause()
            }
        }
        audioPlayerManager.onToggleFavorite = { song ->
            toggleFavoriteSong(song)
        }

        viewModelScope.launch {
            repository.initializeDatabase()
            scanLibrary()
        }

        playerManager.onProgressUpdate = { video, pos, dur ->
            viewModelScope.launch {
                if (userSettings.value.saveHistory) {
                    repository.updatePlaybackProgress(video.id, pos, dur)
                }
            }
        }
    }

    val allSongs: StateFlow<List<Song>> = musicRepository.allSongsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val audioPlaylists: StateFlow<List<AudioPlaylist>> = musicRepository.audioPlaylistsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allVideos: StateFlow<List<Video>> = combine(
        repository.getAllVideosFlow(),
        userSettings
    ) { videos, settings ->
        sortVideos(videos, settings.sortOption)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val favoriteVideos: StateFlow<List<Video>> = repository.getFavoriteVideosFlow().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val watchHistory: StateFlow<List<Video>> = repository.getWatchHistoryFlow().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val continueWatching: StateFlow<List<Video>> = repository.getContinueWatchingFlow().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val onlineVideos: StateFlow<List<Video>> = repository.getOnlineVideosFlow().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val folders: StateFlow<List<VideoFolder>> = allVideos.combine(userSettings) { videos, _ ->
        val groups = videos.groupBy { it.folderName }
        groups.map { (name, list) ->
            val totalDuration = list.sumOf { it.durationMs }
            val firstPath = list.firstOrNull()?.folderPath ?: name
            VideoFolder(
                name = name,
                path = firstPath,
                videoCount = list.size,
                totalDurationMs = totalDuration,
                videos = list
            )
        }.sortedByDescending { it.videoCount }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val playlists: StateFlow<List<Playlist>> = repository.getPlaylistsFlow().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _selectedFolderName = MutableStateFlow<String?>(null)
    val selectedFolder: StateFlow<VideoFolder?> = combine(folders, _selectedFolderName) { fList, fName ->
        if (fName == null) null else fList.find { it.name == fName }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    private val _selectedPlaylistId = MutableStateFlow<Long?>(null)
    val selectedPlaylist: StateFlow<Playlist?> = combine(playlists, _selectedPlaylistId) { pList, pId ->
        if (pId == null) null else pList.find { it.id == pId }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val searchResults: StateFlow<List<Video>> = _searchQuery.flatMapLatest { query ->
        if (query.isBlank()) {
            MutableStateFlow(emptyList())
        } else {
            repository.searchVideos(query)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val recentSearches: StateFlow<List<String>> = repository.getRecentSearches().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun setStoragePermission(granted: Boolean) {
        _hasStoragePermission.value = granted
        if (granted) {
            scanLibrary()
        }
    }

    fun scanLibrary() {
        viewModelScope.launch {
            _isScanning.value = true
            repository.scanLocalVideos()
            musicRepository.scanLocalMusic()
            _isScanning.value = false
        }
    }

    fun toggleFavoriteSong(song: Song) {
        musicRepository.toggleFavorite(song)
    }

    fun createAudioPlaylist(name: String, description: String = "") {
        musicRepository.createPlaylist(name, description)
    }

    fun addSongToAudioPlaylist(playlistId: Long, song: Song) {
        musicRepository.addSongToPlaylist(playlistId, song)
    }

    fun removeSongFromAudioPlaylist(playlistId: Long, songId: String) {
        musicRepository.removeSongFromPlaylist(playlistId, songId)
    }

    fun deleteAudioPlaylist(playlistId: Long) {
        musicRepository.deletePlaylist(playlistId)
    }

    fun toggleFavorite(video: Video) {
        viewModelScope.launch {
            repository.toggleFavorite(video)
        }
    }

    fun deleteVideo(video: Video, deleteFromFileSystem: Boolean = false) {
        viewModelScope.launch {
            repository.deleteVideo(video, deleteFromFileSystem) {
                _needManageStorage.value = true
            }
        }
    }

    fun deleteVideo(videoId: String) {
        viewModelScope.launch {
            repository.deleteVideoById(videoId)
        }
    }

    fun removeFromHistory(videoId: String) {
        viewModelScope.launch {
            repository.removeFromHistory(videoId)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearAllHistory()
        }
    }

    fun clearAllFavorites() {
        viewModelScope.launch {
            repository.clearAllFavorites()
        }
    }

    fun addOnlineVideo(title: String, url: String, onComplete: (Video) -> Unit = {}) {
        viewModelScope.launch {
            val video = repository.addOnlineVideo(title, url)
            onComplete(video)
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        if (query.isNotBlank()) {
            viewModelScope.launch {
                repository.addSearchQuery(query)
            }
        }
    }

    fun deleteSearchQuery(query: String) {
        viewModelScope.launch {
            repository.deleteSearchQuery(query)
        }
    }

    fun clearSearchHistory() {
        viewModelScope.launch {
            repository.clearSearchHistory()
        }
    }

    fun selectFolder(folder: VideoFolder?) {
        _selectedFolderName.value = folder?.name
    }

    fun selectPlaylist(playlist: Playlist?) {
        _selectedPlaylistId.value = playlist?.id
    }

    fun createPlaylist(name: String, description: String = "") {
        viewModelScope.launch {
            repository.createPlaylist(name, description)
        }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch {
            repository.deletePlaylist(playlistId)
        }
    }

    fun addVideoToPlaylist(playlistId: Long, video: Video) {
        viewModelScope.launch {
            repository.addVideoToPlaylist(playlistId, video)
        }
    }

    fun removeVideoFromPlaylist(playlistId: Long, videoId: String) {
        viewModelScope.launch {
            repository.removeVideoFromPlaylist(playlistId, videoId)
        }
    }

    fun getPlaylistVideos(playlistId: Long) = repository.getVideosForPlaylist(playlistId)

    // Settings actions
    fun setTheme(theme: ThemePreference) = viewModelScope.launch { settingsRepository.setTheme(theme) }
    fun setSortOption(sort: SortOption) = viewModelScope.launch { settingsRepository.setSortOption(sort) }
    fun setViewMode(mode: ViewMode) = viewModelScope.launch { settingsRepository.setViewMode(mode) }
    fun setDefaultSpeed(speed: Float) = viewModelScope.launch { settingsRepository.setDefaultSpeed(speed) }
    fun setDoubleTapSeek(sec: Int) = viewModelScope.launch { settingsRepository.setDoubleTapSeekSeconds(sec) }
    fun setGesturesEnabled(enabled: Boolean) = viewModelScope.launch { settingsRepository.setGesturesEnabled(enabled) }
    fun setSwipeBrightness(enabled: Boolean) = viewModelScope.launch { settingsRepository.setSwipeBrightness(enabled) }
    fun setSwipeVolume(enabled: Boolean) = viewModelScope.launch { settingsRepository.setSwipeVolume(enabled) }
    fun setSwipeSeek(enabled: Boolean) = viewModelScope.launch { settingsRepository.setSwipeSeek(enabled) }
    fun setDefaultAspectRatio(mode: AspectRatioMode) = viewModelScope.launch { settingsRepository.setDefaultAspectRatio(mode) }
    fun setSubtitleFontSize(size: Int) = viewModelScope.launch { settingsRepository.setSubtitleFontSize(size) }
    fun setSubtitleColors(text: String, bg: String) = viewModelScope.launch { settingsRepository.setSubtitleColors(text, bg) }
    fun setAutoHideSeconds(sec: Int) = viewModelScope.launch { settingsRepository.setAutoHideSeconds(sec) }
    fun setResumePlayback(resume: Boolean) = viewModelScope.launch { settingsRepository.setResumePlayback(resume) }
    fun setBackgroundAudio(enabled: Boolean) = viewModelScope.launch { settingsRepository.setBackgroundAudio(enabled) }
    fun setHardwareDecoder(enabled: Boolean) = viewModelScope.launch { settingsRepository.setHardwareDecoder(enabled) }
    fun setSaveHistory(save: Boolean) = viewModelScope.launch { settingsRepository.setSaveHistory(save) }
    fun setOnboardingCompleted(completed: Boolean) = viewModelScope.launch { settingsRepository.setOnboardingCompleted(completed) }
    fun setVideoBrightness(brightness: Float) = viewModelScope.launch { settingsRepository.setVideoBrightness(brightness) }

    private fun sortVideos(list: List<Video>, sortOption: SortOption): List<Video> {
        return when (sortOption) {
            SortOption.DATE_DESC -> list.sortedByDescending { it.dateAdded }
            SortOption.DATE_ASC -> list.sortedBy { it.dateAdded }
            SortOption.NAME_ASC -> list.sortedBy { it.title.lowercase() }
            SortOption.NAME_DESC -> list.sortedByDescending { it.title.lowercase() }
            SortOption.DURATION_DESC -> list.sortedByDescending { it.durationMs }
            SortOption.SIZE_DESC -> list.sortedByDescending { it.sizeBytes }
            SortOption.RECENTLY_PLAYED -> list.sortedByDescending { it.lastPlayedTimestamp }
        }
    }

    override fun onCleared() {
        super.onCleared()
        playerManager.release()
        audioPlayerManager.release()
    }
}
