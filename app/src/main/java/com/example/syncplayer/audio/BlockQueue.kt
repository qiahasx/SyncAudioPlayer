package com.example.syncplayer.audio

import kotlinx.coroutines.channels.Channel

class BlockQueue<T>(capacity: Int = 4) {
    private var channel = Channel<T>(capacity)

    suspend fun produce(item: T) {
        channel.send(item)
    }

    suspend fun consume(): T {
        return channel.receive()
    }

    fun tryConsume() = channel.tryReceive().getOrNull()

    fun clear() {
        while (channel.tryReceive().getOrNull() != null) {
        }
    }
}
