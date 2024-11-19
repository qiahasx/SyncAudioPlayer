package com.example.syncplayer.audio

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import java.io.File

data class AudioInfo(
    val channelCount: Int,
    val sampleRate: Int,
    val bitRate: Int,
    val mime: String,
    val duration: Long,
    val fileSize: Long,
    val filePath: String,
) {
    lateinit var format: MediaFormat

    companion object {
        fun createInfo(filePath: String): AudioInfo? {
            val pair = createInfoAndMediaExtractor(filePath) ?: return null
            pair.second.release()
            return pair.first
        }

        fun createInfoAndMediaExtractor(filePath: String): Pair<AudioInfo, MediaExtractor>? {
            val mediaExtractor = MediaExtractor()
            mediaExtractor.setDataSource(filePath)
            for (i in 0 until mediaExtractor.trackCount) {
                val trackFormat = mediaExtractor.getTrackFormat(i)
                val mime = trackFormat.getString(MediaFormat.KEY_MIME)
                if (mime?.startsWith("audio/") == true) {
                    mediaExtractor.selectTrack(i)
                    val audioInfo = createInfoByTrackFormat(trackFormat, filePath)
                    return Pair(audioInfo, mediaExtractor)
                }
            }
            return null
        }

        fun MediaCodec.configureByAudioInfo(audioInfo: AudioInfo) {
            val format =
                MediaFormat.createAudioFormat(
                    audioInfo.mime,
                    audioInfo.sampleRate,
                    audioInfo.channelCount,
                )
            format.setInteger(MediaFormat.KEY_BIT_RATE, audioInfo.bitRate)
            format.setLong(MediaFormat.KEY_DURATION, audioInfo.duration)
            configure(audioInfo.format, null, null, 0)
        }

        private fun createInfoByTrackFormat(
            trackFormat: MediaFormat,
            filePath: String,
        ) = AudioInfo(
            channelCount = trackFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT),
            sampleRate = trackFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE),
            bitRate = trackFormat.getInteger(MediaFormat.KEY_BIT_RATE),
            mime = trackFormat.getString(MediaFormat.KEY_MIME)!!,
            duration = trackFormat.getLong(MediaFormat.KEY_DURATION),
            fileSize = File(filePath).length(),
            filePath = filePath,
        ).apply {
            this.format = trackFormat
        }
    }
}
