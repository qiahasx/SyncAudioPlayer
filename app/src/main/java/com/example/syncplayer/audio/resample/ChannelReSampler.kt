package com.example.syncplayer.audio.resample

import com.example.syncplayer.audio.ShortsInfo

interface ChannelReSampler {
    fun reSampler(pcmData: ShortsInfo): ShortsInfo
}