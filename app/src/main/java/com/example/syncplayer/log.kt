package com.example.syncplayer

import android.util.Log

const val DEBUG_TAG = "Player_ANDROID"

fun debug(text: Any?) {
    Log.d(DEBUG_TAG, text.toString())
}

fun error(text: Any?) {
    Log.e(DEBUG_TAG, text.toString())
}
