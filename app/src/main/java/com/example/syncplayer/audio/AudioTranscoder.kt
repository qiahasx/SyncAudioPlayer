package com.example.syncplayer.audio

import com.example.syncplayer.model.AudioItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope

@OptIn(DelicateCoroutinesApi::class)
class AudioTranscoder(
    item: AudioItem,
    scope: CoroutineScope = GlobalScope,
) {
    private val decoder = AudioDecoder(scope, item.filePath)
    private val encoder by lazy {
        AudioEncoder(decoder.audioInfo, scope)
    }
    private var targetSampleRate = decoder.audioInfo.sampleRate
    private var targetChannels = if (decoder.audioInfo.channelCount > 1) Channels.Stereo else Channels.Mono

    fun getInputFormat(): Format {
        return Format(decoder.audioInfo.sampleRate, if (decoder.audioInfo.channelCount == 1) Channels.Mono else Channels.Stereo)
    }

    fun setOutputFormat(sampleRate: Int, channelNum: Channels) {
        targetSampleRate = sampleRate
        targetChannels = channelNum
    }

    fun release() {
        decoder.release()
//        encoder.release()
    }

    fun start() {
        encoder.setOutPutFormat(targetSampleRate, targetChannels)
        encoder.setPcmData(decoder.queue)
        decoder.start()
        encoder.start()
    }

    class Format(
        val sampleRate: Int,
        val channelNum: Channels,
    )

    enum class Channels(val value: Int) {
        Mono(1),
        Stereo(2)
    }
}