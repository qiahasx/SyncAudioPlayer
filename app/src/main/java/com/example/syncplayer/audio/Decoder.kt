package com.example.syncplayer.audio

import BlockingQueueWithLocks
import android.media.MediaCodec
import android.media.MediaCodec.Callback
import android.media.MediaExtractor
import android.media.MediaFormat

class Decoder(
    filePath: String,
) {
    val audioInfo: AudioInfo
    private var trackIndex: Int = -1
    private val decoder: MediaCodec
    private val extractor = MediaExtractor()
    val afterDecodeQueue = BlockingQueueWithLocks<BytesInfo>(BUFFER_MAX)

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
        if (trackIndex < 0) {
            throw IllegalStateException("extractor not found audio track")
        }
        val format = extractor.getTrackFormat(trackIndex)
        audioInfo = AudioInfo.createInfo(filePath, format)
        decoder = MediaCodec.createDecoderByType(type)
        decoder.configure(format, null, null, 0)
    }

    fun start() {
        decoder.setCallback(createCallBack())
        decoder.start()
    }

    private fun createCallBack() =
        object : Callback() {
            override fun onInputBufferAvailable(
                codec: MediaCodec,
                index: Int,
            ) {
                val inputBuffer = decoder.getInputBuffer(index) ?: return
                val sampleSize = extractor.readSampleData(inputBuffer, 0)
                if (sampleSize < 0) {
                    decoder.queueInputBuffer(index, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
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
                val bytes = ByteArray(info.offset + info.size)
                byteBuffer.get(bytes, info.offset, info.size)
                val bytesInfo =
                    BytesInfo(bytes, info.offset, info.size, info.presentationTimeUs, info.flags)
                afterDecodeQueue.produce(bytesInfo)
                decoder.releaseOutputBuffer(index, false)
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
        }

    companion object {
        const val BUFFER_MAX = 3
    }
}
