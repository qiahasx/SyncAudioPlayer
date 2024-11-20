package com.example.syncplayer.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import androidx.collection.ArrayMap
import com.example.syncplayer.util.debug
import kotlin.concurrent.thread

class SyncPlayer() {
    private val map = ArrayMap<Int, Decoder>()
    private var cnt = 0
    private var isPlaying = false
    private lateinit var audioTrack: AudioTrack

    @Throws(IllegalStateException::class)
    fun setDataSource(path: String): Int {
        if (isPlaying) throw IllegalStateException("Cannot set data source on playing")
        val decoder = Decoder(path)
        map[++cnt] = decoder
        return cnt
    }

    fun start() {
        if (isPlaying || map.isEmpty) return
        audioTrack = initAudioTrack()
        thread {
            map.values.forEach { it.start() }
            audioTrack.play()
            val decodeQueue = map.iterator().next().value.afterDecodeQueue
            while (true) {
                val bytesInfo = decodeQueue.consume()!!
                if (bytesInfo.flags == 4) {
                    audioTrack.stop()
                    break
                }
                audioTrack.write(bytesInfo.bytes, bytesInfo.offset, bytesInfo.size)
            }
        }
    }

    private fun initAudioTrack(): AudioTrack {
        val infos = map.values.map { it.audioInfo }
        val sampleRate = infos.maxOf { it.sampleRate }
        val channelCount = infos.maxOf { it.channelCount }
        val channelMask = coverChannelCountToChannelMask(channelCount)
        debug("channel count: $channelCount channel mask: $channelMask")
        val audioAttributes =
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
        val format =
            AudioFormat.Builder()
                .setSampleRate(sampleRate)
                .setChannelMask(channelMask)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .build()
        val bufferSize: Int =
            AudioTrack.getMinBufferSize(sampleRate, channelMask, AudioFormat.ENCODING_PCM_16BIT)
        return AudioTrack(
            audioAttributes,
            format,
            bufferSize,
            AudioTrack.MODE_STREAM,
            AudioManager.AUDIO_SESSION_ID_GENERATE,
        )
    }

    private fun coverChannelCountToChannelMask(channelCount: Int) =
        when (channelCount) {
            2 -> AudioFormat.CHANNEL_OUT_STEREO
            else -> AudioFormat.CHANNEL_OUT_MONO
        }
}
