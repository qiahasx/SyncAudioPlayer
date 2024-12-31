package com.example.syncplayer.model

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableFloatStateOf

data class AudioItem(
    val name: String,
    val filePath: String,
    var id: Int = -1,
    val volume: MutableState<Float> = mutableFloatStateOf(1f),
)
