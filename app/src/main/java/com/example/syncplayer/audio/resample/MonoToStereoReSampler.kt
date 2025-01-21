package com.example.syncplayer.audio.resample

import com.example.syncplayer.audio.ShortsInfo

class MonoToStereoReSampler : ReSampler {
    override fun reSampler(pcmData: ShortsInfo): ShortsInfo {
        val shorts = ShortArray(pcmData.size * 2) { index ->
            pcmData.shorts[pcmData.offset + index / 2]
        }
        return ShortsInfo(shorts, 0, shorts.size, pcmData.sampleTime, pcmData.flags)
    }
}