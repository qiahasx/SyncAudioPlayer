package com.example.syncplayer.audio

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import com.example.syncplayer.audio.resample.MonoToStereoReSampler
import com.example.syncplayer.audio.resample.SampleRateDownReSampler
import com.example.syncplayer.audio.resample.SampleRateUpReSampler
import com.example.syncplayer.util.debug
import com.example.syncplayer.util.launchIO
import kotlinx.coroutines.CoroutineScope

/**
 * 音频编码，封装
 */
class AudioEncoder(
    private val inputInfo: AudioInfo,
    private val scope: CoroutineScope,
) {
    private val codec = MediaCodec.createEncoderByType(inputInfo.mime)
    private val muxer = MediaMuxer(
        inputInfo.filePath.substringBeforeLast(".") + "_${System.currentTimeMillis()}.m4a",
        MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
    )
    private var muxerTrackIndex = 0
    private val tempInfo = MediaCodec.BufferInfo()
    private var processor: PcmBufferProcessor? = null
    private var isEndOfStreamReached = false
    private var isEndOfEncoded = false
    private val format = MediaFormat().apply {
        setString(MediaFormat.KEY_MIME, MediaFormat.MIMETYPE_AUDIO_AAC)
        setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectMain)
        setInteger(MediaFormat.KEY_SAMPLE_RATE, inputInfo.sampleRate)
        setInteger(MediaFormat.KEY_CHANNEL_COUNT, inputInfo.channelCount)
        setInteger(MediaFormat.KEY_BIT_RATE, inputInfo.bitRate)
        setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 1024 * 256)
    }

    fun setPcmData(data: BlockQueue<ShortsInfo>) {
        processor = PcmBufferProcessor(data)
    }

    fun setOutPutFormat(sampleRate: Int, channelNum: AudioTranscoder.Channels) {
        format.setInteger(MediaFormat.KEY_SAMPLE_RATE, sampleRate)
        format.setInteger(MediaFormat.KEY_CHANNEL_COUNT, channelNum.value)
    }

    fun start() {
        val processor = processor ?: error("Not Set PcmData")
        val targetSampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        val targetChannelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        if (targetChannelCount > inputInfo.channelCount) {
            processor.addReSampler(MonoToStereoReSampler())
        } else if (targetChannelCount < inputInfo.channelCount) {
            processor.addReSampler(MonoToStereoReSampler())
        }
        if (targetSampleRate > inputInfo.sampleRate) {
            processor.addReSampler(SampleRateUpReSampler(targetSampleRate, inputInfo.sampleRate, targetChannelCount))
        } else if (targetSampleRate < inputInfo.sampleRate) {
            processor.addReSampler(SampleRateDownReSampler(targetSampleRate, inputInfo.sampleRate, targetChannelCount))
        }
        codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        codec.start()
        scope.launchIO {
            val start = System.currentTimeMillis()
            debug("start: ${System.currentTimeMillis() - start}")
            while (!isEndOfEncoded) {
                submitPcmToCodec()
                processOutputBuffer()
            }
            debug("end: ${System.currentTimeMillis() - start}")
            release()
        }
    }

    private fun processOutputBuffer() {
        when (val index = codec.dequeueOutputBuffer(tempInfo, 0)) {
            MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                muxerTrackIndex = muxer.addTrack(codec.outputFormat)
                muxer.start()
            }

            MediaCodec.INFO_TRY_AGAIN_LATER -> {}
            else -> {
                if (tempInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                    isEndOfEncoded = true
                } else if (tempInfo.size > 0) {
                    val outputBuffer = codec.getOutputBuffer(index)!!
                    muxer.writeSampleData(muxerTrackIndex, outputBuffer, tempInfo)
                    codec.releaseOutputBuffer(index, false)
                }
            }
        }
    }

    private suspend fun submitPcmToCodec() {
        val processor = processor ?: error("Not Set PcmData")
        if (isEndOfStreamReached) return
        val index = codec.dequeueInputBuffer(0)
        if (index < 0) return
        val buffer = codec.getInputBuffer(index)?.asShortBuffer() ?: return
        val pcmShortInfo = processor.getBuffer(buffer.remaining())
        buffer.put(pcmShortInfo.shorts)
        codec.queueInputBuffer(index, 0, buffer.position() * 2, pcmShortInfo.sampleTime, pcmShortInfo.flags)
        if (pcmShortInfo.flags == MediaCodec.BUFFER_FLAG_END_OF_STREAM) {
            isEndOfStreamReached = true
        }
    }

    private fun release() {
        codec.stop()
        codec.release()
        muxer.stop()
        muxer.release()
    }
}