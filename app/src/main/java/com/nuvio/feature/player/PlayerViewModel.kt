package com.nuvio.feature.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import com.nuvio.domain.repository.PlaybackRepository
import com.nuvio.domain.repository.SettingsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class PlayerUiState(
    val title: String = "",
    val mediaUri: String = "",
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val playbackSpeed: Float = 1.0f,
    val resizeMode: Int = AspectRatioFrameLayout.RESIZE_MODE_FIT,
    val isControlsVisible: Boolean = true,
    val brightnessPercent: Float = 0.5f,
    val volumePercent: Float = 0.5f
)

@OptIn(UnstableApi::class)
class PlayerViewModel(
    private val context: Context,
    private val playbackRepository: PlaybackRepository,
    private val settingsRepository: SettingsRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    var exoPlayer: ExoPlayer? = null
        private set

    private var progressTrackerJob: Job? = null

    init {
        exoPlayer = ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
            addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _uiState.value = _uiState.value.copy(isPlaying = isPlaying)
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_READY) {
                        _uiState.value = _uiState.value.copy(durationMs = duration.coerceAtLeast(0L))
                    }
                }

                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    android.widget.Toast.makeText(
                        context,
                        "Playback error: ${error.localizedMessage ?: "Unsupported or corrupted media"}",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
            })
        }
        startProgressTracker()
    }

    fun playMedia(uri: String, title: String) {
        val player = exoPlayer ?: return
        val defaultSpeed = settingsRepository?.settingsFlow?.value?.defaultPlaybackSpeed ?: 1.0f
        val rememberPos = settingsRepository?.settingsFlow?.value?.rememberPlaybackPosition ?: true

        _uiState.value = _uiState.value.copy(
            title = title,
            mediaUri = uri,
            playbackSpeed = defaultSpeed,
            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
        )

        viewModelScope.launch {
            val savedPos = if (rememberPos) playbackRepository.getLastPlaybackPosition(uri) else 0L
            val parsedUri = if (uri.startsWith("http://") || uri.startsWith("https://") || uri.startsWith("content://") || uri.startsWith("file://")) {
                android.net.Uri.parse(uri)
            } else {
                android.net.Uri.fromFile(java.io.File(uri))
            }
            val mediaItem = MediaItem.fromUri(parsedUri)
            player.playbackParameters = PlaybackParameters(defaultSpeed)
            player.setMediaItem(mediaItem)
            player.prepare()
            if (savedPos > 0) {
                player.seekTo(savedPos)
            }
            player.play()
        }
    }

    fun togglePlayPause() {
        val player = exoPlayer ?: return
        if (player.isPlaying) {
            player.pause()
        } else {
            player.play()
        }
    }

    fun seekTo(positionMs: Long) {
        exoPlayer?.seekTo(positionMs)
        _uiState.value = _uiState.value.copy(currentPositionMs = positionMs)
    }

    fun skipForward(seconds: Int = 10) {
        val player = exoPlayer ?: return
        val newPos = (player.currentPosition + (seconds * 1000)).coerceAtMost(player.duration)
        seekTo(newPos)
    }

    fun skipBackward(seconds: Int = 10) {
        val player = exoPlayer ?: return
        val newPos = (player.currentPosition - (seconds * 1000)).coerceAtLeast(0L)
        seekTo(newPos)
    }

    fun setPlaybackSpeed(speed: Float) {
        exoPlayer?.playbackParameters = PlaybackParameters(speed)
        _uiState.value = _uiState.value.copy(playbackSpeed = speed)
    }

    fun toggleAspectRatio() {
        val nextMode = when (_uiState.value.resizeMode) {
            AspectRatioFrameLayout.RESIZE_MODE_FIT -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_FILL
            else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
        }
        _uiState.value = _uiState.value.copy(resizeMode = nextMode)
    }

    fun toggleControlsVisibility() {
        _uiState.value = _uiState.value.copy(isControlsVisible = !_uiState.value.isControlsVisible)
    }

    fun updateBrightness(percent: Float) {
        _uiState.value = _uiState.value.copy(brightnessPercent = percent.coerceIn(0f, 1f))
    }

    fun updateVolume(percent: Float) {
        _uiState.value = _uiState.value.copy(volumePercent = percent.coerceIn(0f, 1f))
    }

    private fun startProgressTracker() {
        progressTrackerJob?.cancel()
        progressTrackerJob = viewModelScope.launch {
            while (isActive) {
                exoPlayer?.let { player ->
                    if (player.isPlaying) {
                        val pos = player.currentPosition
                        val dur = player.duration.coerceAtLeast(0L)
                        _uiState.value = _uiState.value.copy(
                            currentPositionMs = pos,
                            durationMs = dur
                        )
                        if (_uiState.value.mediaUri.isNotBlank()) {
                            playbackRepository.savePlaybackPosition(
                                mediaUri = _uiState.value.mediaUri,
                                title = _uiState.value.title,
                                artist = null,
                                positionMs = pos,
                                durationMs = dur,
                                isVideo = true
                            )
                        }
                    }
                }
                delay(500)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        progressTrackerJob?.cancel()
        exoPlayer?.release()
        exoPlayer = null
    }
}
