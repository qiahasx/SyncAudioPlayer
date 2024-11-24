package com.example.syncplayer.queue

import kotlinx.coroutines.channels.Channel

class BlockQueue<T>(val capacity: Int = 50) {
    private var channel = Channel<T>(capacity)

    suspend fun produce(item: T) {
        channel.send(item)
    }

    suspend fun consume(): T {
        return channel.receive()
    }

    fun init() {
        channel.close()
        channel = Channel(capacity)
    }
}
