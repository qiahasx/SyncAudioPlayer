package com.example.syncplayer.audio

import android.media.MediaCodec
import android.media.MediaCodec.BufferInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import com.example.syncplayer.queue.BlockQueue
import com.example.syncplayer.util.debug
import com.example.syncplayer.util.launchIO
import kotlinx.coroutines.CoroutineScope
import java.nio.ByteBuffer

class Decoder(
    private val scope: CoroutineScope,
    filePath: String,
) {
    val audioInfo: AudioInfo
    private var trackIndex: Int = -1
    private val decoder: MediaCodec
    private val extractor = MediaExtractor()
    private val queue = BlockQueue<ShortsInfo>(BUFFER_MAX)

    @Volatile
    private var isRunning = false

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
        decoder.start()
        isRunning = true
        startInner()
    }

    fun seekTo(timeUs: Long) {
        extractor.seekTo(timeUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
    }

    private fun startInner() {
        scope.launchIO {
            while (true) {
                val index = decoder.dequeueInputBuffer(0)
                if (index > 0) {
                    val inputBuffer = decoder.getInputBuffer(index) ?: ByteBuffer.allocate(0)
                    val sampleSize = extractor.readSampleData(inputBuffer, 0)
                    if (sampleSize < 0) {
                        decoder.queueInputBuffer(
                            index,
                            0,
                            0,
                            0,
                            MediaCodec.BUFFER_FLAG_END_OF_STREAM,
                        )
                    } else {
                        decoder.queueInputBuffer(index, 0, sampleSize, extractor.sampleTime, 0)
                        extractor.advance()
                    }
                }
            }
        }
        scope.launchIO {
            val info = BufferInfo()
            while (true) {
                val index = decoder.dequeueOutputBuffer(info, 0)
                if (index > 0) {
                    val byteBuffer = decoder.getOutputBuffer(index) ?: ByteBuffer.allocate(0)
                    queue.produce(ShortsInfo.createShortsInfo(byteBuffer, info))
                    decoder.releaseOutputBuffer(index, false)
                    debug(info.flags)
                }
            }
        }
    }

    suspend fun consume(): ShortsInfo = queue.consume()

    companion object {
        const val BUFFER_MAX = 8
    }
}
