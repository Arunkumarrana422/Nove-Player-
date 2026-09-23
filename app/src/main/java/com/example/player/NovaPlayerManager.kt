package com.example.player

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import com.example.domain.model.AspectRatioMode
import com.example.domain.model.AudioTrack
import com.example.domain.model.SubtitleTrack
import com.example.domain.model.Video
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class NovaPlayerManager(private val context: Context) {
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val trackSelector = DefaultTrackSelector(context)
    val exoPlayer: ExoPlayer by lazy {
        val renderersFactory = DefaultRenderersFactory(context)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
            .setEnableDecoderFallback(true)

        ExoPlayer.Builder(context, renderersFactory)
            .setTrackSelector(trackSelector)
            .setSeekBackIncrementMs(10000)
            .setSeekForwardIncrementMs(10000)
            .build().apply {
                addListener(playerListener)
            }
    }

    private val _currentVideo = MutableStateFlow<Video?>(null)
    val currentVideo: StateFlow<Video?> = _currentVideo.asStateFlow()

    private val _queue = MutableStateFlow<List<Video>>(emptyList())
    val queue: StateFlow<List<Video>> = _queue.asStateFlow()

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

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _volumeFraction = MutableStateFlow(0.8f)
    val volumeFraction: StateFlow<Float> = _volumeFraction.asStateFlow()

    private val _brightnessFraction = MutableStateFlow(0.7f)
    val brightnessFraction: StateFlow<Float> = _brightnessFraction.asStateFlow()

    private val _isLocked = MutableStateFlow(false)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    private val _aspectRatioMode = MutableStateFlow(AspectRatioMode.FIT)
    val aspectRatioMode: StateFlow<AspectRatioMode> = _aspectRatioMode.asStateFlow()

    private val _isRepeatOne = MutableStateFlow(false)
    val isRepeatOne: StateFlow<Boolean> = _isRepeatOne.asStateFlow()

    private val _isShuffle = MutableStateFlow(false)
    val isShuffle: StateFlow<Boolean> = _isShuffle.asStateFlow()

    private val _availableAudioTracks = MutableStateFlow<List<AudioTrack>>(emptyList())
    val availableAudioTracks: StateFlow<List<AudioTrack>> = _availableAudioTracks.asStateFlow()

    private val _availableSubtitleTracks = MutableStateFlow<List<SubtitleTrack>>(emptyList())
    val availableSubtitleTracks: StateFlow<List<SubtitleTrack>> = _availableSubtitleTracks.asStateFlow()

    private val _activeSubtitleText = MutableStateFlow<String?>(null)
    val activeSubtitleText: StateFlow<String?> = _activeSubtitleText.asStateFlow()

    private val _subtitleDelayMs = MutableStateFlow(0L)
    val subtitleDelayMs: StateFlow<Long> = _subtitleDelayMs.asStateFlow()

    private val _externalCues = MutableStateFlow<List<SubtitleCue>>(emptyList())

    private val _isMiniPlayerActive = MutableStateFlow(false)
    val isMiniPlayerActive: StateFlow<Boolean> = _isMiniPlayerActive.asStateFlow()

    private val _sleepTimerMinutes = MutableStateFlow<Int?>(null)
    val sleepTimerMinutes: StateFlow<Int?> = _sleepTimerMinutes.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    var onProgressUpdate: ((video: Video, pos: Long, dur: Long) -> Unit)? = null

    private var progressJob: Job? = null
    private var sleepTimerJob: Job? = null

    init {
        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val curVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        if (maxVol > 0) {
            _volumeFraction.value = curVol.toFloat() / maxVol.toFloat()
        }
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
                    _durationMs.value = dur
                    _bufferedPositionMs.value = buf

                    _currentVideo.value?.let { video ->
                        onProgressUpdate?.invoke(video, pos, dur)
                    }

                    // Update external subtitle cues if any
                    val cues = _externalCues.value
                    if (cues.isNotEmpty()) {
                        _activeSubtitleText.value = SubtitleParser.getActiveCue(cues, pos, _subtitleDelayMs.value)
                    }
                }
                delay(200)
            }
        }
    }

    fun playVideo(video: Video, playlist: List<Video> = emptyList(), startPositionMs: Long = 0L) {
        _errorMessage.value = null
        _currentVideo.value = video
        val actualList = if (playlist.isNotEmpty()) playlist else listOf(video)
        _queue.value = actualList
        val idx = actualList.indexOfFirst { it.id == video.id }.coerceAtLeast(0)
        _queueIndex.value = idx

        val mediaItem = MediaItem.fromUri(Uri.parse(video.uri))
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()

        val resumePos = if (startPositionMs > 0) startPositionMs else video.lastPositionMs
        if (resumePos > 0) {
            exoPlayer.seekTo(resumePos)
        }

        exoPlayer.playWhenReady = true
        _isPlaying.value = true
        _isMiniPlayerActive.value = false
    }

    fun flushProgress() {
        _currentVideo.value?.let { video ->
            val pos = exoPlayer.currentPosition.coerceAtLeast(0L)
            val dur = exoPlayer.duration.coerceAtLeast(0L)
            if (pos > 0L) {
                onProgressUpdate?.invoke(video, pos, dur)
            }
        }
    }

    fun play() {
        exoPlayer.play()
        _isPlaying.value = true
    }

    fun pause() {
        flushProgress()
        exoPlayer.pause()
        _isPlaying.value = false
    }

    fun stopAndDismiss() {
        flushProgress()
        try {
            exoPlayer.stop()
            exoPlayer.clearMediaItems()
        } catch (_: Exception) {}
        _currentVideo.value = null
        _isPlaying.value = false
        _isMiniPlayerActive.value = false
        _currentPositionMs.value = 0L
        _durationMs.value = 0L
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

    fun setSpeed(speed: Float) {
        _playbackSpeed.value = speed
        exoPlayer.setPlaybackSpeed(speed)
    }

    fun setVolume(fraction: Float) {
        val clamped = fraction.coerceIn(0f, 1f)
        _volumeFraction.value = clamped
        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val targetVol = (clamped * maxVol).toInt()
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, 0)
    }

    fun adjustVolumeBy(deltaFraction: Float) {
        setVolume(_volumeFraction.value + deltaFraction)
    }

    fun setBrightness(fraction: Float, activity: Activity? = null) {
        val clamped = fraction.coerceIn(0.01f, 1.0f)
        _brightnessFraction.value = clamped
        activity?.let {
            val lp = it.window.attributes
            lp.screenBrightness = clamped
            it.window.attributes = lp
        }
    }

    fun restoreSystemBrightness(activity: Activity?) {
        activity?.let {
            val lp = it.window.attributes
            lp.screenBrightness = android.view.WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            it.window.attributes = lp
        }
    }

    fun applyPlayerBrightness(activity: Activity?) {
        activity?.let {
            val lp = it.window.attributes
            lp.screenBrightness = _brightnessFraction.value.coerceIn(0.01f, 1.0f)
            it.window.attributes = lp
        }
    }

    fun adjustBrightnessBy(deltaFraction: Float, activity: Activity? = null) {
        setBrightness(_brightnessFraction.value + deltaFraction, activity)
    }

    fun setLocked(locked: Boolean) {
        _isLocked.value = locked
    }

    fun cycleAspectRatio() {
        val modes = AspectRatioMode.values()
        val currentIdx = modes.indexOf(_aspectRatioMode.value)
        val nextMode = modes[(currentIdx + 1) % modes.size]
        _aspectRatioMode.value = nextMode
    }

    fun setAspectRatio(mode: AspectRatioMode) {
        _aspectRatioMode.value = mode
    }

    fun nextVideo() {
        val q = _queue.value
        if (q.isNotEmpty()) {
            val nextIdx = (_queueIndex.value + 1) % q.size
            _queueIndex.value = nextIdx
            playVideo(q[nextIdx], q, 0L)
        }
    }

    fun previousVideo() {
        val q = _queue.value
        if (q.isNotEmpty()) {
            val prevIdx = if (_queueIndex.value - 1 < 0) q.size - 1 else _queueIndex.value - 1
            _queueIndex.value = prevIdx
            playVideo(q[prevIdx], q, 0L)
        }
    }

    fun toggleRepeat() {
        val next = !_isRepeatOne.value
        _isRepeatOne.value = next
        exoPlayer.repeatMode = if (next) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
    }

    fun toggleShuffle() {
        val next = !_isShuffle.value
        _isShuffle.value = next
        exoPlayer.shuffleModeEnabled = next
    }

    fun selectAudioTrack(track: AudioTrack) {
        val tracks = exoPlayer.currentTracks
        for (trackGroup in tracks.groups) {
            if (trackGroup.type == C.TRACK_TYPE_AUDIO) {
                for (i in 0 until trackGroup.length) {
                    val format = trackGroup.getTrackFormat(i)
                    if (format.id == track.id || format.language == track.language) {
                        trackSelector.setParameters(
                            trackSelector.buildUponParameters()
                                .setOverrideForType(
                                    TrackSelectionOverride(trackGroup.mediaTrackGroup, i)
                                )
                        )
                        break
                    }
                }
            }
        }
        updateAvailableTracks(exoPlayer.currentTracks)
    }

    fun selectSubtitleTrack(subtitle: SubtitleTrack?) {
        if (subtitle == null) {
            // Disable subtitles
            trackSelector.setParameters(
                trackSelector.buildUponParameters()
                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
            )
            _externalCues.value = emptyList()
            _activeSubtitleText.value = null
        } else if (subtitle.isExternal) {
            // Handled via external parser
        } else {
            trackSelector.setParameters(
                trackSelector.buildUponParameters()
                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
            )
        }
        updateAvailableTracks(exoPlayer.currentTracks)
    }

    suspend fun loadExternalSubtitle(uri: Uri) {
        val cues = SubtitleParser.parseFromUri(context, uri)
        _externalCues.value = cues
        val extTrack = SubtitleTrack(
            id = "ext_${System.currentTimeMillis()}",
            language = "External",
            label = "External Subtitle (${cues.size} lines)",
            uri = uri.toString(),
            isExternal = true,
            isSelected = true
        )
        _availableSubtitleTracks.value = _availableSubtitleTracks.value + extTrack
    }

    fun adjustSubtitleDelay(deltaMs: Long) {
        _subtitleDelayMs.value += deltaMs
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

    fun setMiniPlayer(active: Boolean) {
        _isMiniPlayerActive.value = active
    }

    private fun updateAvailableTracks(tracks: Tracks) {
        val audioList = mutableListOf<AudioTrack>()
        val subList = mutableListOf<SubtitleTrack>()

        for (group in tracks.groups) {
            if (group.type == C.TRACK_TYPE_AUDIO) {
                for (i in 0 until group.length) {
                    val format = group.getTrackFormat(i)
                    val label = format.label ?: format.language ?: "Audio Track ${audioList.size + 1}"
                    audioList.add(
                        AudioTrack(
                            id = format.id ?: "$i",
                            label = "$label (${format.sampleMimeType ?: "Audio"})",
                            language = format.language ?: "und",
                            isSelected = group.isTrackSelected(i)
                        )
                    )
                }
            } else if (group.type == C.TRACK_TYPE_TEXT) {
                for (i in 0 until group.length) {
                    val format = group.getTrackFormat(i)
                    val label = format.label ?: format.language ?: "Subtitle ${subList.size + 1}"
                    subList.add(
                        SubtitleTrack(
                            id = format.id ?: "$i",
                            language = format.language ?: "und",
                            label = "$label (${format.language ?: "Text"})",
                            isSelected = group.isTrackSelected(i)
                        )
                    )
                }
            }
        }

        _availableAudioTracks.value = audioList
        _availableSubtitleTracks.value = subList
    }

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            _isBuffering.value = (playbackState == Player.STATE_BUFFERING)
            if (playbackState == Player.STATE_ENDED) {
                _isPlaying.value = false
                if (!_isRepeatOne.value) {
                    nextVideo()
                }
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.value = isPlaying
        }

        override fun onTracksChanged(tracks: Tracks) {
            updateAvailableTracks(tracks)
        }

        override fun onPlayerError(error: PlaybackException) {
            val message = when (error.errorCode) {
                PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
                PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ->
                    "Network error: Unable to connect to streaming server. Check your connection."
                PlaybackException.ERROR_CODE_DECODER_INIT_FAILED ->
                    "Hardware decoder failed. Switched to software decoding fallback."
                PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED,
                PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED ->
                    "Unsupported or corrupted video stream format."
                else -> error.localizedMessage ?: "Playback error occurred: ${error.errorCodeName}"
            }
            _errorMessage.value = message
            _isBuffering.value = false
        }
    }

    fun retryPlayback() {
        _errorMessage.value = null
        exoPlayer.prepare()
        exoPlayer.play()
    }

    fun release() {
        progressJob?.cancel()
        sleepTimerJob?.cancel()
        exoPlayer.removeListener(playerListener)
        exoPlayer.release()
    }
}
