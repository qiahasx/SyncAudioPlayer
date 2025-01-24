package com.example.syncplayer.audio

import com.example.syncplayer.model.AudioItem
import com.example.syncplayer.util.launchIO
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow

@OptIn(DelicateCoroutinesApi::class)
class AudioTranscoder(
    item: AudioItem,
    private val scope: CoroutineScope = GlobalScope,
) {
    val progress = MutableStateFlow(0f)
    private val decoder = AudioDecoder(scope, item.filePath)
    private lateinit var encoder: AudioEncoder
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
        if (this::encoder.isInitialized) encoder.release()
    }

    fun start() {
        encoder = AudioEncoder(decoder.audioInfo, scope)
        encoder.setOutPutFormat(targetSampleRate, targetChannels)
        encoder.setPcmData(decoder.queue)
        decoder.start()
        encoder.start()
        startCalculateProgress()
    }

    private fun startCalculateProgress() {
        scope.launchIO {
            encoder.sampleTime.collect {
                progress.emit(it / decoder.audioInfo.duration.toFloat())
            }
        }
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