package com.example.syncplayer.audio.resample

import com.example.syncplayer.audio.ShortsInfo

class StereoToMonoReSampler : ReSampler {
    private var attenuationFactor = 1f

    override fun reSampler(pcmData: ShortsInfo): ShortsInfo {
        val size = pcmData.size / 2
        val shorts = ShortArray(size) { index ->
            var mixValue = pcmData.shorts[index * 2 + pcmData.offset] + pcmData.shorts[index * 2 + 1 + pcmData.offset]
            mixValue = (mixValue * attenuationFactor).toInt()
            when {
                mixValue > SHORT_MAX -> {
                    attenuationFactor = SHORT_MAX_FLOAT / mixValue
                    mixValue = SHORT_MAX
                }

                mixValue < SHORT_MIN -> {
                    attenuationFactor = SHORT_MIN_FLOAT / mixValue
                    mixValue = SHORT_MIN
                }
            }
            if (attenuationFactor < 1) {
                attenuationFactor += (1 - attenuationFactor) / STEP_SIZE
            }
            mixValue.toShort()
        }
        return ShortsInfo(shorts, 0, shorts.size, pcmData.sampleTime, pcmData.flags)
    }

    companion object {
        const val SHORT_MAX = 32767
        const val SHORT_MAX_FLOAT = 32767f
        const val SHORT_MIN = -32768
        const val SHORT_MIN_FLOAT = -32768f
        const val STEP_SIZE = 32
    }
}