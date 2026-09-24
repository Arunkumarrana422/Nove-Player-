package com.example.player

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.domain.model.AudioRepeatMode
import com.example.domain.model.EqualizerPreset
import com.example.domain.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AudioPlayerManager(private val context: Context) {
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    val exoPlayer: ExoPlayer by lazy {
        ExoPlayer.Builder(context)
            .setSeekBackIncrementMs(10000)
            .setSeekForwardIncrementMs(10000)
            .build().apply {
                addListener(playerListener)
            }
    }

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _queue = MutableStateFlow<List<Song>>(emptyList())
    val queue: StateFlow<List<Song>> = _queue.asStateFlow()

    private val _queueIndex = MutableStateFlow(0)
    val queueIndex: StateFlow<Int> = _queueIndex.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _bufferedPositionMs = MutableStateFlow(0L)
    val bufferedPositionMs: StateFlow<Long> = _bufferedPositionMs.asStateFlow()

    private val _isBuffering = MutableStateFlow(false)
    val isBuffering: StateFlow<Boolean> = _isBuffering.asStateFlow()

    private val _isShuffle = MutableStateFlow(false)
    val isShuffle: StateFlow<Boolean> = _isShuffle.asStateFlow()

    private val _repeatMode = MutableStateFlow(AudioRepeatMode.OFF)
    val repeatMode: StateFlow<AudioRepeatMode> = _repeatMode.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _sleepTimerMinutes = MutableStateFlow<Int?>(null)
    val sleepTimerMinutes: StateFlow<Int?> = _sleepTimerMinutes.asStateFlow()

    private val _isFullPlayerOpen = MutableStateFlow(false)
    val isFullPlayerOpen: StateFlow<Boolean> = _isFullPlayerOpen.asStateFlow()

    private val _selectedPreset = MutableStateFlow("Balanced")
    val selectedPreset: StateFlow<String> = _selectedPreset.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    val equalizerPresets = listOf(
        EqualizerPreset("flat", "Balanced", 0f, 0f, 0f),
        EqualizerPreset("bass_boost", "Bass Boost", 6f, 0f, -2f),
        EqualizerPreset("vocal", "Vocal Booster", -2f, 5f, 2f),
        EqualizerPreset("rock", "Rock & Metal", 4f, -1f, 5f),
        EqualizerPreset("pop", "Pop & Beats", 3f, 2f, 3f),
        EqualizerPreset("electronic", "Electronic / EDM", 5f, 1f, 4f),
        EqualizerPreset("classical", "Acoustic / Classical", 2f, 1f, 3f),
        EqualizerPreset("treble", "Treble Boost", -2f, 1f, 6f)
    )

    private var progressJob: Job? = null
    private var sleepTimerJob: Job? = null

    // Callback to stop video playback when audio starts
    var onAudioStarted: (() -> Unit)? = null
    var onToggleFavorite: ((Song) -> Unit)? = null

    fun toggleFavorite(song: Song) {
        onToggleFavorite?.invoke(song)
        val updatedSong = song.copy(isFavorite = !song.isFavorite)
        if (_currentSong.value?.id == song.id) {
            _currentSong.value = updatedSong
        }
        _queue.value = _queue.value.map { if (it.id == song.id) updatedSong else it }
    }

    fun playNext() {
        nextSong()
    }

    init {
        startProgressTracker()
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                if (exoPlayer.playbackState == Player.STATE_READY || exoPlayer.playbackState == Player.STATE_BUFFERING) {
                    val pos = exoPlayer.currentPosition.coerceAtLeast(0L)
                    val dur = exoPlayer.duration.coerceAtLeast(0L)
                    val buf = exoPlayer.bufferedPosition.coerceAtLeast(0L)

                    _currentPositionMs.value = pos
                    if (dur > 0L) {
                        _durationMs.value = dur
                    }
                    _bufferedPositionMs.value = buf
                }
                delay(200)
            }
        }
    }

    fun playSong(song: Song, playlist: List<Song> = emptyList(), startIndex: Int = -1) {
        onAudioStarted?.invoke()
        _errorMessage.value = null

        val currentId = _currentSong.value?.id
        if (currentId == song.id && exoPlayer.playbackState != Player.STATE_IDLE) {
            val actualQueue = if (playlist.isNotEmpty()) playlist else _queue.value
            if (actualQueue.isNotEmpty()) {
                _queue.value = actualQueue
                val index = if (startIndex >= 0 && startIndex < actualQueue.size) {
                    startIndex
                } else {
                    actualQueue.indexOfFirst { it.id == song.id }.coerceAtLeast(_queueIndex.value)
                }
                _queueIndex.value = index
            }
            if (!exoPlayer.isPlaying) {
                exoPlayer.play()
                _isPlaying.value = true
            }
            return
        }

        _currentSong.value = song

        val actualQueue = if (playlist.isNotEmpty()) playlist else listOf(song)
        _queue.value = actualQueue
        val index = if (startIndex >= 0 && startIndex < actualQueue.size) {
            startIndex
        } else {
            actualQueue.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
        }
        _queueIndex.value = index

        try {
            val mediaItem = MediaItem.fromUri(Uri.parse(song.uri))
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
            _isPlaying.value = true
            _durationMs.value = song.durationMs
            _currentPositionMs.value = 0L
        } catch (e: Exception) {
            _errorMessage.value = "Failed to load track: ${e.localizedMessage}"
        }
    }

    fun play() {
        onAudioStarted?.invoke()
        exoPlayer.play()
        _isPlaying.value = true
    }

    fun pause() {
        exoPlayer.pause()
        _isPlaying.value = false
    }

    fun togglePlayPause() {
        if (exoPlayer.isPlaying) {
            pause()
        } else {
            play()
        }
    }

    fun seekTo(positionMs: Long) {
        val bounded = positionMs.coerceIn(0L, _durationMs.value.coerceAtLeast(1L))
        exoPlayer.seekTo(bounded)
        _currentPositionMs.value = bounded
    }

    fun seekBy(deltaMs: Long) {
        val target = (_currentPositionMs.value + deltaMs).coerceIn(0L, _durationMs.value.coerceAtLeast(1L))
        seekTo(target)
    }

    fun nextSong() {
        val q = _queue.value
        if (q.isNotEmpty()) {
            val nextIdx = (_queueIndex.value + 1) % q.size
            _queueIndex.value = nextIdx
            playSong(q[nextIdx], q, nextIdx)
        }
    }

    fun previousSong() {
        val q = _queue.value
        if (q.isNotEmpty()) {
            // If current position is > 3 seconds, replay current song first
            if (_currentPositionMs.value > 3000L) {
                seekTo(0L)
            } else {
                val prevIdx = if (_queueIndex.value - 1 < 0) q.size - 1 else _queueIndex.value - 1
                _queueIndex.value = prevIdx
                playSong(q[prevIdx], q, prevIdx)
            }
        }
    }

    fun toggleShuffle() {
        val next = !_isShuffle.value
        _isShuffle.value = next
        exoPlayer.shuffleModeEnabled = next
    }

    fun cycleRepeatMode() {
        val next = when (_repeatMode.value) {
            AudioRepeatMode.OFF -> AudioRepeatMode.ALL
            AudioRepeatMode.ALL -> AudioRepeatMode.ONE
            AudioRepeatMode.ONE -> AudioRepeatMode.OFF
        }
        _repeatMode.value = next
        exoPlayer.repeatMode = when (next) {
            AudioRepeatMode.OFF -> Player.REPEAT_MODE_OFF
            AudioRepeatMode.ALL -> Player.REPEAT_MODE_ALL
            AudioRepeatMode.ONE -> Player.REPEAT_MODE_ONE
        }
    }

    fun setSpeed(speed: Float) {
        _playbackSpeed.value = speed
        exoPlayer.setPlaybackSpeed(speed)
    }

    fun setPreset(name: String) {
        _selectedPreset.value = name
    }

    fun setSleepTimer(minutes: Int?) {
        _sleepTimerMinutes.value = minutes
        sleepTimerJob?.cancel()
        if (minutes != null && minutes > 0) {
            sleepTimerJob = scope.launch {
                var remainingSec = minutes * 60
                while (remainingSec > 0) {
                    delay(1000)
                    remainingSec--
                    _sleepTimerMinutes.value = (remainingSec / 60) + 1
                }
                pause()
                _sleepTimerMinutes.value = null
            }
        }
    }

    fun openFullPlayer() {
        _isFullPlayerOpen.value = true
    }

    fun closeFullPlayer() {
        _isFullPlayerOpen.value = false
    }

    fun stopAndDismiss() {
        try {
            exoPlayer.stop()
            exoPlayer.clearMediaItems()
        } catch (_: Exception) {}
        _currentSong.value = null
        _isPlaying.value = false
        _isFullPlayerOpen.value = false
        _currentPositionMs.value = 0L
        _durationMs.value = 0L
    }

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            _isBuffering.value = (playbackState == Player.STATE_BUFFERING)
            if (playbackState == Player.STATE_ENDED) {
                when (_repeatMode.value) {
                    AudioRepeatMode.ONE -> {
                        seekTo(0L)
                        play()
                    }
                    AudioRepeatMode.ALL -> {
                        nextSong()
                    }
                    AudioRepeatMode.OFF -> {
                        val q = _queue.value
                        if (_queueIndex.value < q.size - 1) {
                            nextSong()
                        } else {
                            _isPlaying.value = false
                            seekTo(0L)
                        }
                    }
                }
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.value = isPlaying
        }

        override fun onPlayerError(error: PlaybackException) {
            _errorMessage.value = "Audio error: ${error.localizedMessage ?: error.errorCodeName}"
            _isBuffering.value = false
        }
    }

    fun release() {
        progressJob?.cancel()
        sleepTimerJob?.cancel()
        exoPlayer.removeListener(playerListener)
        exoPlayer.release()
    }
}
