package com.example.syncplayer

import android.util.Log

const val DEBUG_TAG = "Player_ANDROID"

fun debug(text: Any?) {
    Log.d(DEBUG_TAG, text.toString())
}

fun Any?.debug(text: String = "") {
    Log.d(DEBUG_TAG, "$text: $this")
}