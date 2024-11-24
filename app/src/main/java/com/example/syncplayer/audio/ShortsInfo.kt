package com.example.syncplayer.audio

import android.media.MediaCodec
import java.nio.ByteBuffer

class ShortsInfo(
    val shorts: ShortArray,
    var offset: Int = 0,
    var size: Int = 0,
    var sampleTime: Long = 0,
    var flags: Int = 0,
) {
    companion object {
        fun createShortsInfo(
            buffer: ByteBuffer,
            info: MediaCodec.BufferInfo,
        ): ShortsInfo {
            buffer.position(info.offset)
            buffer.limit(info.offset + info.size)
            val shortCount = (info.size + 1) / 2
            val shorts = ShortArray(shortCount)
            for (i in 0 until shortCount) {
                shorts[i] = buffer.getShort()
            }
            return ShortsInfo(shorts, 0, shortCount, info.presentationTimeUs, info.flags)
        }
    }
}
