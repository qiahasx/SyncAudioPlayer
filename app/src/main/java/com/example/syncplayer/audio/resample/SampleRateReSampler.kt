package com.example.syncplayer.audio.resample

import com.example.syncplayer.audio.AudioTranscoder
import com.example.syncplayer.audio.ShortsInfo

// TODO: libsamplerate
interface SampleRateReSampler {
    fun reSampler(pcmData: ShortsInfo, oldRate: Int, newRate: Int, channels: AudioTranscoder.Channels): ShortsInfo
}