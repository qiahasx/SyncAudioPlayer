package com.example.syncplayer.queue

import java.nio.ByteBuffer

class ByteBufferQueue(
    private val bufferSize: Int,
    val capacity: Int,
) : BlockQueue<ByteBuffer>(capacity) {
    suspend fun init() {
        for (i in 0 until capacity) {
            produce(ByteBuffer.allocate(bufferSize))
        }
    }
}
