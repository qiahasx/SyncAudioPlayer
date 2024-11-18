package com.example.syncplayer.audio

import android.media.MediaCodec
import android.media.MediaCodec.Callback
import android.media.MediaExtractor
import android.media.MediaFormat
import com.example.syncplayer.debug
import java.io.File

class Decoder(
    filePath: String,
) {
    var end = false
    val sampleRate: Int
    val bitRate: Int
    val channelCount: Int
    private var trackIndex: Int = -1
    val duration: Long
    private val decoder: MediaCodec
    private var isAllAudioFed = false
    private var isDecodeDone = false
    private val extractor = MediaExtractor()

    init {
        extractor.setDataSource(filePath)
        var type = ""
        // 选择找到的第一条音频轨道
        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME)
            if (mime != null && mime.startsWith("audio/")) {
                trackIndex = i
                type = mime
                extractor.selectTrack(trackIndex)
                break
            }
        }
        if (trackIndex >= 0) {
            val format = extractor.getTrackFormat(trackIndex)
            sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            bitRate = format.getInteger(MediaFormat.KEY_BIT_RATE)
            channelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            duration = format.getLong(MediaFormat.KEY_DURATION)
            decoder = MediaCodec.createDecoderByType(type)
            decoder.configure(format, null, null, 0)
        } else {
            throw IllegalStateException("extractor not found audio track")
        }
    }

    fun decodeToPCMFile(pcmFilePath: String) {
        val ops = File(pcmFilePath).outputStream()
        val start = System.currentTimeMillis()
        decoder.setCallback(
            object : Callback() {
                override fun onInputBufferAvailable(
                    codec: MediaCodec,
                    index: Int,
                ) {
                    val inputBuffer = decoder.getInputBuffer(index) ?: return
                    val sampleSize = extractor.readSampleData(inputBuffer, 0)
                    if (sampleSize < 0) {
                        decoder.queueInputBuffer(
                            index,
                            0,
                            0,
                            0,
                            MediaCodec.BUFFER_FLAG_END_OF_STREAM,
                        )
                        isAllAudioFed = true
                    } else {
                        decoder.queueInputBuffer(index, 0, sampleSize, extractor.sampleTime, 0)
                        extractor.advance()
                    }
                }

                override fun onOutputBufferAvailable(
                    codec: MediaCodec,
                    index: Int,
                    info: MediaCodec.BufferInfo,
                ) {
                    val byteBuffer = decoder.getOutputBuffer(index) ?: return
                    val bytes = ByteArray(byteBuffer.remaining())
                    byteBuffer.get(bytes, info.offset, info.size)
                    ops.write(bytes)
                    decoder.releaseOutputBuffer(index, false)
                    if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        isDecodeDone = true
                        debug("success: ${System.currentTimeMillis() - start}")
                    }
                }

                override fun onError(
                    codec: MediaCodec,
                    e: MediaCodec.CodecException,
                ) {
                }

                override fun onOutputFormatChanged(
                    codec: MediaCodec,
                    format: MediaFormat,
                ) {
                }
            },
        )
        decoder.start()
    }
}
