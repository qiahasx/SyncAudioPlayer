package com.example.syncplayer.audio.transformer

import com.example.syncplayer.audio.ShortsInfo

class PassReSampler : ReSampler {
    override fun reSampler(pcmData: ShortsInfo) = pcmData
}