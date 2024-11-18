package com.example.syncplayer.audio

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.ShortBuffer
import kotlin.experimental.and

class PcmFileDecoder(
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

    suspend fun decodeToPCMFile(pcmFilePath: String): File {
        start()
        val file = File(pcmFilePath)
        withContext(Dispatchers.IO) {
            val outputStream = file.outputStream()
            while (!isDecodeDone) {
                outputStream.write(decodeAudioTrackToBuffer().asByteArray())
            }
            outputStream.flush()
            outputStream.close()
        }
        release()
        return file
    }

    private fun release() {
        decoder.stop()
        decoder.release()
        extractor.release()
    }

    private fun start() {
        decoder.start()
    }

    private fun createShortBuffer(capacity: Int): ShortBuffer {
        val buffer =
            ByteBuffer.allocateDirect(capacity * 2)
                .order(ByteOrder.nativeOrder())
                .asShortBuffer()
        buffer.clear()
        buffer.limit(capacity)
        return buffer
    }

    private fun decodeAudioTrackToBuffer(): ShortBuffer {
        var resultBuffer: ShortBuffer? = null
        while (resultBuffer == null && !isDecodeDone) {
            // 把数据提取到解码器
            if (!isAllAudioFed) {
                feedAudioTrackToDecoder()
            }
            // 尝试从解码器中获得数据
            resultBuffer = acquireBufferFromDecoder()
        }

        return resultBuffer ?: ShortBuffer.allocate(0)
    }

    private fun acquireBufferFromDecoder(): ShortBuffer? {
        var res: ShortBuffer? = null
        val potPutBufferInfo = MediaCodec.BufferInfo()
        val outPutBufferIndex = decoder.dequeueOutputBuffer(potPutBufferInfo, 0)
        if (outPutBufferIndex >= 0) {
            val byteBuffer =
                decoder.getOutputBuffer(outPutBufferIndex)?.asShortBuffer() ?: return null
            if ((potPutBufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                isDecodeDone =
                    true
            }
            if (byteBuffer.remaining() > 0) {
                val shortBuffer = createShortBuffer(byteBuffer.remaining())
                shortBuffer.put(byteBuffer)
                shortBuffer.limit(shortBuffer.position())
                shortBuffer.rewind()
                res = shortBuffer
                decoder.releaseOutputBuffer(outPutBufferIndex, false)
            }
        }
        return res
    }

    private fun feedAudioTrackToDecoder() {
        val inputBufferIndex = decoder.dequeueInputBuffer(0)
        if (inputBufferIndex >= 0) {
            val inputBuffer = decoder.getInputBuffer(inputBufferIndex) ?: return
            // 提取音轨数据到解码器
            val sampleSize = extractor.readSampleData(inputBuffer, 0)
            if (sampleSize < 0) {
                // 数据读取完毕
                decoder.queueInputBuffer(
                    inputBufferIndex,
                    0,
                    0,
                    0,
                    MediaCodec.BUFFER_FLAG_END_OF_STREAM,
                )
                isAllAudioFed = true
            } else {
                // 将数据送入解码器
                decoder.queueInputBuffer(inputBufferIndex, 0, sampleSize, extractor.sampleTime, 0)
                extractor.advance()
            }
        }
    }
}

fun ShortBuffer.asByteArray(): ByteArray {
    rewind()
    val bytes = ByteArray(capacity() * 2)
    var i = 0
    while (hasRemaining()) {
        val s = get()
        bytes[i++] = (s and 0x00FF).toByte()
        bytes[i++] = ((s.toInt() shr 8) and 0xFF).toByte()
    }
    return bytes
}
