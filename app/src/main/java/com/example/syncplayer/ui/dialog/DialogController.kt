package com.example.syncplayer.ui.dialog

abstract class DialogController {
    private var onDismissCallback: (() -> Unit)? = null

    fun setDismiss(callback: () -> Unit) {
        onDismissCallback = callback
    }

    fun dismiss() {
        onDismissCallback?.invoke()
    }
}