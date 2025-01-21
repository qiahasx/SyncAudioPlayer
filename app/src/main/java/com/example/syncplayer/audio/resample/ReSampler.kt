package com.example.syncplayer.audio.resample

import com.example.syncplayer.audio.ShortsInfo

interface ReSampler {
    fun reSampler(pcmData: ShortsInfo): ShortsInfo
}