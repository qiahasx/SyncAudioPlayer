package com.example.syncplayer.audio

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
        shortsInfo.flags = 0
        shortsInfo.sampleTime = 0
        val shorts = ShortArray(bufferSize) { getNext(shortsInfo) }
        return ShortsInfo(shorts, 0, bufferSize, 0L, shortsInfo.flags)
    }

    private suspend fun getNext(info: ShortsInfo): Short {
        val bufferInfo = cache ?: audioDecoder.consume().also { cache = it }
        info.flags = info.flags or bufferInfo.flags
        if (bufferInfo.size == 0) {
            cache = null
            return getNext(info)
        }
        val result = bufferInfo.shorts[bufferInfo.offset]
        bufferInfo.offset++
        bufferInfo.size--
        return result
    }
}
