package com.example.syncplayer.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import com.example.syncplayer.util.launchIO
import com.example.syncplayer.util.withIO
import kotlinx.coroutines.CoroutineScope

class SyncPlayer(private val scope: CoroutineScope) {
    private val mix = AudioMixer(scope)
    private lateinit var audioTrack: AudioTrack

    @Throws(IllegalStateException::class)
    fun setDataSource(path: String): Int {
        return mix.addAudioSource(path)
    }

    fun start() {
        audioTrack = initAudioTrack()
        mix.start()
        audioTrack.play()
        scope.launchIO {
            startInner()
        }
    }

    suspend fun seekTo(timeUs: Long) = withIO { mix.seekTo(timeUs) }

    private suspend fun startInner() {
        while (true) {
            val bytesInfo = mix.queue.consume()
            if (bytesInfo.flags == 4) {
                audioTrack.stop()
                break
            }
            audioTrack.write(bytesInfo.floats, bytesInfo.offset, bytesInfo.size, AudioTrack.WRITE_BLOCKING)
        }
    }

    private fun initAudioTrack(): AudioTrack {
        val sampleRate = mix.getSampleRate()
        val channelCount = mix.getChannelCount()
        val channelMask = coverChannelCountToChannelMask(channelCount)
        val audioAttributes =
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
        val format =
            AudioFormat.Builder()
                .setSampleRate(sampleRate)
                .setChannelMask(channelMask)
                .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                .build()
        val bufferSize: Int =
            AudioTrack.getMinBufferSize(sampleRate, channelMask, AudioFormat.ENCODING_PCM_FLOAT)
        return AudioTrack(audioAttributes, format, bufferSize, AudioTrack.MODE_STREAM, AudioManager.AUDIO_SESSION_ID_GENERATE)
    }

    private fun coverChannelCountToChannelMask(channelCount: Int) =
        when (channelCount) {
            2 -> AudioFormat.CHANNEL_OUT_STEREO
            else -> AudioFormat.CHANNEL_OUT_MONO
        }
}
