package com.example.syncplayer.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream

class PCMPlayer(
    private val scope: CoroutineScope,
    private val file: File,
) {
    private lateinit var fis: FileInputStream
    private val audioTrack: AudioTrack
    private val bufferSize: Int =
        AudioTrack.getMinBufferSize(
            44100,
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_16BIT,
        )

    init {
        val audioAttributes =
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()
        val format =
            AudioFormat.Builder()
                .setSampleRate(44100)
                .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .build()
        audioTrack =
            AudioTrack(
                audioAttributes,
                format,
                bufferSize,
                AudioTrack.MODE_STREAM,
                AudioManager.AUDIO_SESSION_ID_GENERATE,
            )
    }

    fun play() {
        scope.launch {
            audioTrack.play()
            val byteArray = ByteArray(bufferSize)
            fis = file.inputStream()
            while (true) {
                val readSize = fis.read(byteArray)
                if (readSize <= 0) {
                    release()
                    break
                }
                audioTrack.write(byteArray, 0, readSize)
            }
        }
    }

    private fun release() {
        audioTrack.stop()
        audioTrack.release()
        fis.close()
    }
}
