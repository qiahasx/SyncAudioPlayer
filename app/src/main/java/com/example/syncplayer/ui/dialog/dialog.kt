package com.example.syncplayer.ui.dialog

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import com.example.syncplayer.LocalDialogManager

@Composable
fun Dialog() {
    val dialogManager = LocalDialogManager.current
    val controllers = dialogManager.dialog.collectAsState()
    controllers.value.forEach {
        when (it) {
            is ProgressDialogController -> {
                ProgressDialog(it)
            }

            is AudioInfoDialog -> {
                AudioResampleDialog(it)
            }
        }
    }
}