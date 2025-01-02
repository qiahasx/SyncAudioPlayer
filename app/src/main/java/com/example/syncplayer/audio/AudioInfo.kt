package com.example.syncplayer.audio

import android.media.MediaFormat
import java.io.File

data class AudioInfo(
    val sampleRate: Int,
    val channelCount: Int,
    val bitRate: Int,
    val mime: String,
    val duration: Long,
    val fileSize: Long,
    val filePath: String,
) {
    companion object {
        fun createInfo(
            filePath: String,
            format: MediaFormat,
        ): AudioInfo {
            val mime = format.getString(MediaFormat.KEY_MIME) ?: "UNKNOWN"
            val sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            val bitRate = format.getInteger(MediaFormat.KEY_BIT_RATE)
            val channelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            val duration = format.getLong(MediaFormat.KEY_DURATION)
            return AudioInfo(
                sampleRate,
                channelCount,
                bitRate,
                mime,
                duration,
                File(filePath).length(),
                filePath,
            )
        }
    }
}
