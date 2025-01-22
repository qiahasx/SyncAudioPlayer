package com.example.syncplayer.audio

import android.media.MediaCodec.BUFFER_FLAG_END_OF_STREAM
import com.example.syncplayer.audio.resample.ReSampler

class PcmBufferProcessor(
    private val pcmData: BlockQueue<ShortsInfo>,
) {
    private var cache: ShortsInfo? = null
    private val shortsInfo = ShortsInfo(ShortArray(0))
    private var reSamplers = mutableListOf<ReSampler>()

    fun clearCache() {
        cache = null
    }

    fun addReSampler(reSampler: ReSampler) {
        reSamplers.add(reSampler)
    }

    suspend fun getBuffer(size: Int): ShortsInfo {
        val shorts = ShortArray(size) { getNext(shortsInfo) }
        return ShortsInfo(shorts, 0, size, shortsInfo.sampleTime, shortsInfo.flags)
    }

    private suspend fun getNext(info: ShortsInfo): Short {
        val bufferInfo = cache ?: pcmData.consume().applyResample(reSamplers).also {
            cache = it
            info.sampleTime = it.sampleTime
            info.flags = it.flags
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

    private fun ShortsInfo.applyResample(reSamplers: List<ReSampler>): ShortsInfo {
        var currentPcmData = this
        for (reSampler in reSamplers) {
            currentPcmData = reSampler.reSampler(currentPcmData)
        }
        return currentPcmData
    }
}