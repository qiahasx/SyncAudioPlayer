package com.example.syncplayer.audio.resample

import com.example.syncplayer.audio.AudioTranscoder
import com.example.syncplayer.audio.ShortsInfo
import kotlin.math.roundToInt

class SampleRateUpReSampler : SampleRateReSampler {
    override fun reSampler(pcmData: ShortsInfo, oldRate: Int, newRate: Int, channels: AudioTranscoder.Channels): ShortsInfo {
        if (newRate <= oldRate) {
            throw IllegalArgumentException("New sample rate must be greater than the old sample rate for upsampling.")
        }
        val ratio = newRate.toFloat() / oldRate.toFloat()
        val oldSize = pcmData.size / channels.value
        val newSize = (oldSize * ratio).toInt()
        val newShorts = ShortArray(newSize * channels.value)
        for (channel in 0 until channels.value) {
            for (i in 0 until newSize) {
                val originalIndex = (i / ratio).roundToInt()
                val nextIndex = minOf(originalIndex + 1, oldSize - 1)
                val originalSample = pcmData.shorts[originalIndex * channels.value + channel]
                val nextSample = pcmData.shorts[nextIndex * channels.value + channel]
                val alpha = i / ratio - originalIndex
                newShorts[i * channels.value + channel] =
                    ((1 - alpha) * originalSample + alpha * nextSample).toInt().toShort()
            }
        }
        return ShortsInfo(newShorts, 0, newShorts.size, pcmData.sampleTime, pcmData.flags)
    }
}