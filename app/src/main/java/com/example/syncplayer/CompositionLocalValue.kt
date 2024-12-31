package com.example.syncplayer

import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.runtime.compositionLocalOf
import androidx.navigation.NavHostController
import com.example.syncplayer.audio.AudioSyncPlayer
import com.example.syncplayer.viewModel.MainViewModel

val LocalPickFile = createCompositionLocal<ActivityResultLauncher<Intent>>()

val LocalSyncPlayer = createCompositionLocal<AudioSyncPlayer>()

val LocalMainViewModel = createCompositionLocal<MainViewModel>()

val LocalNavController = createCompositionLocal<NavHostController>()

fun <T> createCompositionLocal() = compositionLocalOf<T> { error("not value") }
