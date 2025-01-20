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
    private val encoder = AudioEncoder(decoder.audioInfo, scope)

    fun getInputFormat(): Format {
        return Format(decoder.audioInfo.sampleRate, if (decoder.audioInfo.channelCount == 1) Channels.Mono else Channels.Stereo)
    }

    fun setOutputFormat(sampleRate: Int, channelNum: Channels) {
        encoder.setOutPutFormat(sampleRate, channelNum)
    }

    fun release() {
        decoder.release()
//        encoder.release()
    }

    fun start() {
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