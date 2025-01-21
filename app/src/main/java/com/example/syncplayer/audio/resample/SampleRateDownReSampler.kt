package com.example.syncplayer.audio.resample

import com.example.syncplayer.audio.AudioTranscoder
import com.example.syncplayer.audio.ShortsInfo
import kotlin.math.ceil

class SampleRateDownReSampler : SampleRateReSampler {
    private fun ratio(remaining: Int, all: Int): Float {
        return remaining.toFloat() / all
    }

    override fun reSampler(pcmData: ShortsInfo, oldRate: Int, newRate: Int, channels: AudioTranscoder.Channels): ShortsInfo {
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
            // 判断是保留样本还是丢弃样本
            if (remainingOutputSamplesRatio >= remainingDropSamplesRatio) {
                for (i in 0 until channels.value) {
                    if (inputIndex + i < pcmData.size) {
                        shorts[outputIndex++] = pcmData.shorts[inputIndex + i]
                    }
                }
                remainingOutputSamples--
                remainingOutputSamplesRatio = ratio(remainingOutputSamples, outputSamples)
            } else {
                // 丢弃样本 - 仅更新输入索引而不写入输出数组
                inputIndex += channels.value
                remainingDropSamples--
                remainingDropSamplesRatio = ratio(remainingDropSamples, dropSamples)
            }
        }
        // 处理剩余的输出样本或丢弃样本
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