package com.example.syncplayer.audio

import BlockingQueueWithLocks
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import com.example.syncplayer.util.debug
import com.example.syncplayer.util.launchIO
import kotlinx.coroutines.CoroutineScope
import java.io.File

class PCMPlayer(
    private val scope: CoroutineScope,
    private val file: File,
) {
    private val audioTrack: AudioTrack
    private val bufferSize: Int =
        AudioTrack.getMinBufferSize(
            48000,
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
                .setSampleRate(48000)
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

    fun play(afterDecodeQueue: BlockingQueueWithLocks<BytesInfo>) {
        scope.launchIO {
            val start = System.currentTimeMillis()
            audioTrack.play()
            while (true) {
                val bytesInfo =
                    afterDecodeQueue.consume() ?: throw IllegalStateException("怎么拿不到了")
                if (bytesInfo.flags == 4) {
                    release()
                    debug("结束了，播放了 ${System.currentTimeMillis() - start}")
                    break
                }
                audioTrack.write(bytesInfo.bytes, bytesInfo.offset, bytesInfo.size)
            }
        }
    }

    private fun release() {
        audioTrack.stop()
        audioTrack.release()
    }
}
