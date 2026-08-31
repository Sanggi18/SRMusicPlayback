package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.screens.SongScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.MusicViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MusicViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                    val playbackInfo by viewModel.playbackInfo.collectAsStateWithLifecycle()

                    SongScreen(
                        uiState = uiState,
                        playbackInfo = playbackInfo,
                        playbackProgressFlow = viewModel.playbackProgress,
                        onPermissionGranted = { isGranted -> viewModel.onPermissionResult(isGranted) },
                        onRequestPermission = { viewModel.checkPermissionAndLoad() },
                        onRefresh = { viewModel.loadSongs(isRefreshing = true) },
                        onSongClick = { song -> viewModel.playSong(song) },
                        onPlayPause = { viewModel.togglePlayPause() },
                        onNext = { viewModel.skipToNext() },
                        onPrevious = { viewModel.skipToPrevious() },
                        onSeek = { positionMs -> viewModel.seekTo(positionMs) }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (viewModel.hasAudioPermission() && viewModel.uiState.value !is com.example.ui.viewmodel.SongListUiState.Success) {
            viewModel.loadSongs()
        }
    }
}

