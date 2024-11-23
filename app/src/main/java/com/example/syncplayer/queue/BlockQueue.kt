package com.example.syncplayer.queue

import kotlinx.coroutines.channels.Channel

class BlockQueue<T>(capacity: Int = 50) {
    private val channel = Channel<T>(capacity)

    suspend fun produce(item: T) {
        channel.send(item)
    }

    suspend fun consume(): T {
        return channel.receive()
    }
}
