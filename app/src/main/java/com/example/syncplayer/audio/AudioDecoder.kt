package com.example.syncplayer.audio

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import com.example.syncplayer.audio.AudioInfo.Companion.configureByAudioInfo
import com.example.syncplayer.debug
import com.example.syncplayer.queue.BlockQueue
import com.example.syncplayer.queue.ByteBufferQueue
import com.example.syncplayer.util.launchIO
import kotlinx.coroutines.CoroutineScope
import java.nio.ByteBuffer

class AudioDecoder(
    val scope: CoroutineScope,
    filePath: String,
) {
    private val audioInfo: AudioInfo
    private val codec: MediaCodec
    private val extractor: MediaExtractor
    private val bufferQueue = ByteBufferQueue(BUFFER_SIZE, BUFFER_MAX)
    private val preDecodeQueue = BlockQueue<BufferInfo>(BUFFER_MAX)
    val afterDecodeQueue = BlockQueue<BufferInfo>(BUFFER_MAX)

    init {
        val pair =
            AudioInfo.createInfoAndMediaExtractor(filePath)
                ?: throw IllegalStateException("Audio file 解析失败")
        audioInfo = pair.first
        debug("Audio info: $audioInfo")
        extractor = pair.second
        codec = MediaCodec.createDecoderByType(audioInfo.mime)
        codec.configureByAudioInfo(audioInfo)
        codec.setCallback(createCallBack())
    }

    fun start() {
        codec.start()
        scope.launchIO(false) {
            bufferQueue.init()
            extractData()
        }
    }

    private suspend fun extractData() {
        while (true) {
            val buffer = bufferQueue.consume() ?: continue
            val readSize = extractor.readSampleData(buffer, 0)
            val bufferInfo =
                if (readSize > 0) {
                    BufferInfo(buffer, 0, readSize, extractor.sampleTime, extractor.sampleFlags)
                } else {
                    debug("saascsacsascasa")
                    BufferInfo(buffer, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM).also {
                        debug("saascsacsascasa$it")
                    }
                }
            preDecodeQueue.produce(bufferInfo)
            if (readSize < 0) {
                break
            }
            extractor.advance()
        }
    }

    private fun createCallBack() =
        object : MediaCodec.Callback() {
            private val inputBytes = ByteArray(BUFFER_SIZE)

            override fun onInputBufferAvailable(
                codec: MediaCodec,
                index: Int,
            ) {
                scope.launchIO {
                    val byteBuffer = codec.getInputBuffer(index) ?: return@launchIO
                    val bufferInfo = preDecodeQueue.consume() ?: return@launchIO
                    bufferInfo.buffer.get(inputBytes, 0, bufferInfo.size)
                    byteBuffer.put(inputBytes, 0, bufferInfo.size)
                    codec.queueInputBuffer(
                        index,
                        0,
                        bufferInfo.size,
                        bufferInfo.sampleTime,
                        bufferInfo.flags,
                    )
                    bufferQueue.produce(bufferInfo.buffer)
                }
            }

            override fun onOutputBufferAvailable(
                codec: MediaCodec,
                index: Int,
                info: MediaCodec.BufferInfo,
            ) {
                scope.launchIO {
                    val outputBuffer = codec.getOutputBuffer(index) ?: return@launchIO
                    val buffer = ByteBuffer.allocate(outputBuffer.remaining())
                    buffer.put(outputBuffer)
                    buffer.rewind()
                    val infoItem =
                        BufferInfo(
                            buffer,
                            info.offset,
                            info.size,
                            info.presentationTimeUs,
                            info.flags,
                        )
                    afterDecodeQueue.produce(infoItem)
                    codec.releaseOutputBuffer(index, false)
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
        }

    companion object {
        const val BUFFER_SIZE = 1024 * 16
        const val BUFFER_MAX = 50
    }
}
