package com.example.syncplayer

import kotlinx.coroutines.CoroutineScope
import kotlin.concurrent.thread

class SyncPlayer(val scope: CoroutineScope) {

    fun setData(path: String) {
        thread {
            setFilePath(path)
        }
    }

    fun s() {
        thread {
            start()
        }
    }

    private external fun setFilePath(path: String)

    external fun start()

    external fun stop()
}