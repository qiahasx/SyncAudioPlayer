package com.example.syncplayer

import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.runtime.compositionLocalOf
import com.example.syncplayer.viewModel.DialogManager
import com.example.syncplayer.viewModel.MainViewModel
import com.example.syncplayer.viewModel.NavViewModel

val LocalPickFile = createCompositionLocal<ActivityResultLauncher<Intent>>()

val LocalMainViewModel = createCompositionLocal<MainViewModel>()

val LocalNavViewModel = createCompositionLocal<NavViewModel>()

val LocalDialogManager = createCompositionLocal<DialogManager>()

fun <T> createCompositionLocal() = compositionLocalOf<T> { error("not value") }
