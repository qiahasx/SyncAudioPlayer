package com.example.syncplayer.audio.transformer

import com.example.syncplayer.audio.ShortsInfo

interface ReSampler {
    fun reSampler(pcmData: ShortsInfo): ShortsInfo
}