package com.example.syncplayer.audio

import java.nio.ByteBuffer

class BufferInfo(
    val buffer: ByteBuffer,
    var offset: Int = 0,
    var size: Int = 0,
    val sampleTime: Long = 0,
    val flags: Int = 0,
) {
    override fun toString(): String {
        return "BufferInfo(offset=$offset, size=$size, sampleTime=$sampleTime, flags=$flags)"
    }
}
