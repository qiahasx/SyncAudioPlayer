package com.example.syncplayer.audio.resample

import com.example.syncplayer.audio.AudioTranscoder
import com.example.syncplayer.audio.ShortsInfo

class SampleRateUpReSampler(
    private val oldRate: Int,
    private val newRate: Int,
    private val channels: AudioTranscoder.Channels,
) : ReSampler {
    constructor(oldRate: Int, newRate: Int, channels: Int) : this(
        oldRate,
        newRate,
        if (channels == 1) AudioTranscoder.Channels.Mono else AudioTranscoder.Channels.Stereo
    )

    init {
        if (newRate <= oldRate) {
            error("New sample rate must be greater than the old sample rate for upsampling.")
        }
    }

    override fun reSampler(pcmData: ShortsInfo): ShortsInfo {
        val ratio = newRate.toFloat() / oldRate.toFloat()
        val oldSize = pcmData.size / channels.value
        val newSize = (oldSize * ratio).toInt()
        val newShorts = ShortArray(newSize * channels.value)
        for (channel in 0 until channels.value) {
            for (i in 0 until newSize) {
                val originalIndex = (i / ratio).toInt()
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