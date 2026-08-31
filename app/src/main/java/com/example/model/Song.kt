package com.example.model

import android.net.Uri

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long, // in milliseconds
    val albumId: Long,
    val contentUri: Uri,
    val artworkUri: Uri,
    val trackNumber: Int = 0,
    val year: Int = 0,
    val size: Long = 0L,
    val displayName: String = ""
)
