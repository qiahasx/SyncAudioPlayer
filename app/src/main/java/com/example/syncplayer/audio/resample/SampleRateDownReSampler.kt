package com.example.syncplayer.audio.resample

import com.example.syncplayer.audio.AudioTranscoder
import com.example.syncplayer.audio.ShortsInfo
import kotlin.math.ceil

class SampleRateDownReSampler(
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
        if (oldRate > newRate) {
            error("oldRate must be greater than newRate")
        }
    }

    private fun ratio(remaining: Int, all: Int): Float {
        return remaining.toFloat() / all
    }

    override fun reSampler(pcmData: ShortsInfo): ShortsInfo {
        require(oldRate > newRate) { "oldRate must be greater than newRate" }
        val inputSamples = pcmData.size / channels.value
        val outputSamples = ceil(inputSamples * (newRate.toDouble() / oldRate)).toInt()
        val shorts = ShortArray(outputSamples * channels.value)
        val dropSamples = inputSamples - outputSamples
        var remainingOutputSamples = outputSamples
        var remainingDropSamples = dropSamples
        var remainingOutputSamplesRatio = ratio(remainingOutputSamples, outputSamples)
        var remainingDropSamplesRatio = ratio(remainingDropSamples, dropSamples)
        var inputIndex = pcmData.offset
        var outputIndex = 0
        while (remainingOutputSamples > 0 && remainingDropSamples >= 0 && inputIndex < pcmData.size) {
            if (remainingOutputSamplesRatio >= remainingDropSamplesRatio) {
                for (i in 0 until channels.value) {
                    if (inputIndex + i < pcmData.size) {
                        shorts[outputIndex++] = pcmData.shorts[inputIndex + i]
                    }
                }
                remainingOutputSamples--
                remainingOutputSamplesRatio = ratio(remainingOutputSamples, outputSamples)
            } else {
                inputIndex += channels.value
                remainingDropSamples--
                remainingDropSamplesRatio = ratio(remainingDropSamples, dropSamples)
            }
        }
        while (remainingOutputSamples > 0 && inputIndex < pcmData.size) {
            for (i in 0 until channels.value) {
                if (inputIndex + i < pcmData.size) {
                    shorts[outputIndex++] = pcmData.shorts[inputIndex + i]
                }
            }
            remainingOutputSamples--
        }
        return ShortsInfo(shorts, 0, outputSamples * channels.value, pcmData.sampleTime, pcmData.flags)
    }
}