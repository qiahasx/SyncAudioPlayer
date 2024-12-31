package com.example.syncplayer.util

import android.util.Log

const val DEBUG_TAG = "Player_ANDROID"

fun debug(text: Any?) {
    Log.d(DEBUG_TAG, text.toString())
}