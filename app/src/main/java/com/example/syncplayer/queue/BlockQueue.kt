package com.example.syncplayer.queue

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedReceiveChannelException

class BlockQueue<T>(capacity: Int = 50) {
    private val channel = Channel<T>(capacity)
    private var head: T? = null

    suspend fun produce(item: T) {
        channel.send(item)
    }

    suspend fun consume(): T? {
        return try {
            head.also { head = null } ?: channel.receive()
        } catch (_: ClosedReceiveChannelException) {
            null
        }
    }
}
