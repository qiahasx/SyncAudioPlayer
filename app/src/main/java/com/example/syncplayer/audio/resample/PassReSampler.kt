package com.example.syncplayer.audio.resample

import com.example.syncplayer.audio.AudioTranscoder
import com.example.syncplayer.audio.ShortsInfo

class PassReSampler : ChannelReSampler, SampleRateReSampler {
    override fun reSampler(pcmData: ShortsInfo) = pcmData
    override fun reSampler(pcmData: ShortsInfo, oldRate: Int, newRate: Int, channels: AudioTranscoder.Channels) = pcmData
}