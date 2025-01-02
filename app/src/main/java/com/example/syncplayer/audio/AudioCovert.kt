package com.example.syncplayer.audio

import android.media.MediaCodec.BUFFER_FLAG_END_OF_STREAM
import com.example.syncplayer.util.debug

class AudioCovert(
    val audioDecoder: AudioDecoder,
    val bufferSize: Int,
) {
    private var cache: ShortsInfo? = null
    private val shortsInfo = ShortsInfo(ShortArray(0))

    fun clearCache() {
        cache = null
    }

    suspend fun getBuffer(): ShortsInfo {
        val shorts = ShortArray(bufferSize) { getNext(shortsInfo) }
        return ShortsInfo(shorts, 0, bufferSize, shortsInfo.sampleTime, shortsInfo.flags)
    }

    private suspend fun getNext(info: ShortsInfo): Short {
        val bufferInfo = cache ?: audioDecoder.consume().also {
            cache = it
            info.sampleTime = it.sampleTime
            info.flags = it.flags
            debug("${it.offset}  ${it.size}")
        }
        if (bufferInfo.size == 0 || bufferInfo.offset >= bufferInfo.shorts.size) {
            if (bufferInfo.flags != BUFFER_FLAG_END_OF_STREAM) {
                cache = null
                return getNext(info)
            } else {
                return 0
            }
        }
        val result = bufferInfo.shorts.getOrNull(bufferInfo.offset) ?: 0
        bufferInfo.offset++
        bufferInfo.size--
        return result
    }
}
