package com.example.syncplayer.audio

import android.media.MediaCodec
import android.media.MediaCodec.BufferInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import com.example.syncplayer.queue.BlockQueue
import com.example.syncplayer.util.launchIO
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.isActive
import java.nio.ByteBuffer

class AudioDecoder(
    private val scope: CoroutineScope,
    filePath: String,
) {
    val audioInfo: AudioInfo
    private var trackIndex: Int = -1
    private val decoder: MediaCodec
    private val extractor = MediaExtractor()
    private var queue = BlockQueue<ShortsInfo>(BUFFER_MAX)
    private var decodeJob: Job? = null

    init {
        extractor.setDataSource(filePath)
        var type = ""
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
        startInner()
    }

    suspend fun consume(): ShortsInfo = queue.consume()

    suspend fun seekTo(timeUs: Long) {
        decodeJob?.cancelAndJoin()
        queue.clear()
        decoder.flush()
        extractor.seekTo(timeUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
        startInner()
    }

    private fun startInner() {
        decodeJob =
            scope.launchIO {
                val info = BufferInfo()
                while (isActive) {
                    extractorData()
                    decoderData(info)
                }
            }
    }

    private fun extractorData() {
        val index = decoder.dequeueInputBuffer(0)
        if (index > 0) {
            val inputBuffer = decoder.getInputBuffer(index) ?: ByteBuffer.allocate(0)
            val sampleSize = extractor.readSampleData(inputBuffer, 0)
            if (sampleSize < 0) {
                decoder.queueInputBuffer(index, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
            } else {
                decoder.queueInputBuffer(index, 0, sampleSize, extractor.sampleTime, 0)
                extractor.advance()
            }
        }
    }

    private suspend fun decoderData(info: BufferInfo) {
        val index = decoder.dequeueOutputBuffer(info, 0)
        if (index > 0) {
            val byteBuffer = decoder.getOutputBuffer(index) ?: ByteBuffer.allocate(0)
            queue.produce(ShortsInfo.createShortsInfo(byteBuffer, info))
            decoder.releaseOutputBuffer(index, false)
        }
    }

    companion object {
        const val BUFFER_MAX = 4
    }
}
