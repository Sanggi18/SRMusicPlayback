package com.example.playback

import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.model.PlaybackInfo
import com.example.model.PlaybackProgress
import com.example.model.Song
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PlaybackManager(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null

    private val _playbackInfo = MutableStateFlow(PlaybackInfo())
    val playbackInfo: StateFlow<PlaybackInfo> = _playbackInfo.asStateFlow()

    private val _playbackProgress = MutableStateFlow(PlaybackProgress())
    val playbackProgress: StateFlow<PlaybackProgress> = _playbackProgress.asStateFlow()

    private var currentQueue: List<Song> = emptyList()
    private var progressJob: Job? = null

    private val playerListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            updatePlaybackInfo(player)
            if (events.contains(Player.EVENT_IS_PLAYING_CHANGED) ||
                events.contains(Player.EVENT_PLAYBACK_STATE_CHANGED) ||
                events.contains(Player.EVENT_POSITION_DISCONTINUITY)
            ) {
                updateProgress(player)
                if (player.isPlaying) {
                    startProgressPolling()
                } else {
                    stopProgressPolling()
                }
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            mediaController?.let { updatePlaybackInfo(it) }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isPlaying) {
                startProgressPolling()
            } else {
                stopProgressPolling()
            }
        }
    }

    init {
        initializeController()
    }

    private fun initializeController() {
        val sessionToken = SessionToken(context, ComponentName(context, MusicService::class.java))
        val future = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture = future

        future.addListener(
            {
                try {
                    val controller = future.get()
                    mediaController = controller
                    controller.addListener(playerListener)
                    updatePlaybackInfo(controller)
                    if (controller.isPlaying) {
                        startProgressPolling()
                    }
                } catch (e: Exception) {
                    Log.e("SRMusic:Playback", "PlaybackManager: Failed to connect to MediaController", e)
                }
            },
            ContextCompat.getMainExecutor(context)
        )
    }

    fun playSong(song: Song, playlist: List<Song>) {
        val controller = mediaController ?: return
        val isSameQueue = currentQueue.size == playlist.size &&
                currentQueue.isNotEmpty() &&
                currentQueue.firstOrNull()?.id == playlist.firstOrNull()?.id &&
                currentQueue.lastOrNull()?.id == playlist.lastOrNull()?.id

        currentQueue = playlist
        val startIndex = playlist.indexOfFirst { it.id == song.id }.coerceAtLeast(0)

        if (isSameQueue && controller.mediaItemCount == playlist.size) {
            controller.seekTo(startIndex, 0L)
            controller.play()
        } else {
            val mediaItems = playlist.map { s ->
                MediaItem.Builder()
                    .setMediaId(s.id.toString())
                    .setUri(s.contentUri)
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(s.title)
                            .setArtist(s.artist)
                            .setAlbumTitle(s.album)
                            .setArtworkUri(s.artworkUri)
                            .build()
                    )
                    .build()
            }
            controller.setMediaItems(mediaItems, startIndex, 0L)
            controller.prepare()
            controller.play()
        }
    }

    fun togglePlayPause() {
        val controller = mediaController ?: return
        if (controller.isPlaying) {
            controller.pause()
        } else {
            if (controller.playbackState == Player.STATE_ENDED) {
                controller.seekTo(0, 0L)
            }
            controller.play()
        }
    }

    fun seekTo(positionMs: Long) {
        val controller = mediaController ?: return
        controller.seekTo(positionMs.coerceIn(0L, controller.duration.coerceAtLeast(0L)))
        updateProgress(controller)
    }

    fun skipToNext() {
        val controller = mediaController ?: return
        if (controller.hasNextMediaItem()) {
            controller.seekToNextMediaItem()
        }
    }

    fun skipToPrevious() {
        val controller = mediaController ?: return
        if (controller.currentPosition > 3000L) {
            controller.seekTo(0L)
        } else if (controller.hasPreviousMediaItem()) {
            controller.seekToPreviousMediaItem()
        } else {
            controller.seekTo(0L)
        }
    }

    private fun updatePlaybackInfo(player: Player) {
        val currentMediaItem = player.currentMediaItem
        val currentSong = currentMediaItem?.let { item ->
            val id = item.mediaId.toLongOrNull() ?: -1L
            currentQueue.find { it.id == id } ?: Song(
                id = id,
                title = item.mediaMetadata.title?.toString() ?: "Unknown Title",
                artist = item.mediaMetadata.artist?.toString() ?: "Unknown Artist",
                album = item.mediaMetadata.albumTitle?.toString() ?: "Unknown Album",
                duration = player.duration.coerceAtLeast(0L),
                albumId = -1L,
                contentUri = item.requestMetadata.mediaUri ?: android.net.Uri.EMPTY,
                artworkUri = item.mediaMetadata.artworkUri ?: android.net.Uri.EMPTY
            )
        }

        val isBuffering = player.playbackState == Player.STATE_BUFFERING

        _playbackInfo.value = PlaybackInfo(
            currentSong = currentSong,
            isPlaying = player.isPlaying,
            isBuffering = isBuffering,
            hasNext = player.hasNextMediaItem()
        )
    }

    private fun updateProgress(player: Player) {
        val duration = player.duration.coerceAtLeast(0L)
        val position = player.currentPosition.coerceAtLeast(0L).coerceAtMost(if (duration > 0L) duration else Long.MAX_VALUE)

        _playbackProgress.value = PlaybackProgress(
            currentPosition = position,
            duration = duration
        )
    }

    private fun startProgressPolling() {
        if (progressJob?.isActive == true) return
        progressJob = scope.launch {
            while (isActive) {
                mediaController?.let { updateProgress(it) }
                delay(300L)
            }
        }
    }

    private fun stopProgressPolling() {
        progressJob?.cancel()
        progressJob = null
        mediaController?.let { updateProgress(it) }
    }

    fun release() {
        stopProgressPolling()
        mediaController?.removeListener(playerListener)
        controllerFuture?.let { MediaController.releaseFuture(it) }
        mediaController = null
        controllerFuture = null
    }
}
