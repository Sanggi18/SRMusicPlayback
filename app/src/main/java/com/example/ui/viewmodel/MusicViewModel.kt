package com.example.ui.viewmodel

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.MediaStoreRepository
import com.example.model.PlaybackInfo
import com.example.model.PlaybackProgress
import com.example.model.Song
import com.example.playback.PlaybackManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface SongListUiState {
    data object Loading : SongListUiState
    data class Success(val songs: List<Song>, val isRefreshing: Boolean = false) : SongListUiState
    data object Empty : SongListUiState
    data class PermissionRequired(val permission: String) : SongListUiState
}

class MusicViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MediaStoreRepository(application)
    private val playbackManager = PlaybackManager(application)

    private val _uiState = MutableStateFlow<SongListUiState>(SongListUiState.Loading)
    val uiState: StateFlow<SongListUiState> = _uiState.asStateFlow()

    val playbackInfo: StateFlow<PlaybackInfo> = playbackManager.playbackInfo
    val playbackProgress: StateFlow<PlaybackProgress> = playbackManager.playbackProgress

    init {
        checkPermissionAndLoad()
    }

    fun getRequiredAudioPermission(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    }

    fun hasAudioPermission(): Boolean {
        val permission = getRequiredAudioPermission()
        return ContextCompat.checkSelfPermission(
            getApplication(),
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun checkPermissionAndLoad() {
        if (hasAudioPermission()) {
            loadSongs()
        } else {
            _uiState.value = SongListUiState.PermissionRequired(getRequiredAudioPermission())
        }
    }

    fun onPermissionResult(isGranted: Boolean) {
        if (isGranted) {
            loadSongs()
        } else {
            _uiState.value = SongListUiState.PermissionRequired(getRequiredAudioPermission())
        }
    }

    fun loadSongs(isRefreshing: Boolean = false) {
        viewModelScope.launch {
            val currentState = _uiState.value
            if (currentState is SongListUiState.Success && isRefreshing) {
                _uiState.value = currentState.copy(isRefreshing = true)
            } else if (!isRefreshing) {
                _uiState.value = SongListUiState.Loading
            }

            val songs = repository.loadSongs()
            if (songs.isEmpty()) {
                _uiState.value = SongListUiState.Empty
            } else {
                _uiState.value = SongListUiState.Success(songs = songs, isRefreshing = false)
            }
        }
    }

    fun playSong(song: Song) {
        val currentState = _uiState.value
        val playlist = if (currentState is SongListUiState.Success) currentState.songs else listOf(song)
        playbackManager.playSong(song, playlist)
    }

    fun togglePlayPause() {
        playbackManager.togglePlayPause()
    }

    fun seekTo(positionMs: Long) {
        playbackManager.seekTo(positionMs)
    }

    fun skipToNext() {
        playbackManager.skipToNext()
    }

    fun skipToPrevious() {
        playbackManager.skipToPrevious()
    }

    override fun onCleared() {
        super.onCleared()
        playbackManager.release()
    }
}
