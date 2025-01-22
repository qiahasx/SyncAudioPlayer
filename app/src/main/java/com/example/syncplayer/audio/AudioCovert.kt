package com.example.syncplayer.audio

import com.example.syncplayer.audio.resample.MonoToStereoReSampler
import com.example.syncplayer.audio.resample.SampleRateDownReSampler
import com.example.syncplayer.audio.resample.SampleRateUpReSampler

class AudioCovert(
    private val audioDecoder: AudioDecoder,
    private val bufferSize: Int,
) {
    private var targetSampleRate = getInputFormat().sampleRate
    private var targetChannels = if (audioDecoder.audioInfo.channelCount > 1) AudioTranscoder.Channels.Stereo
    else AudioTranscoder.Channels.Mono
    private val processor = PcmBufferProcessor(audioDecoder.queue)

    suspend fun getBuffer(size: Int = bufferSize) = processor.getBuffer(size)

    fun getInputFormat() = audioDecoder.audioInfo

    fun setTargetFormat(sampleRate: Int, channels: Int) {
        targetSampleRate = sampleRate
        targetChannels = if (channels > 1) AudioTranscoder.Channels.Stereo
        else AudioTranscoder.Channels.Mono
    }

    suspend fun seekTo(timeUs: Long) {
        clearCache()
        audioDecoder.seekTo(timeUs)
    }

    fun release() = audioDecoder.release()

    fun start() {
        if (targetChannels.value > getInputFormat().channelCount) {
            processor.addReSampler(MonoToStereoReSampler())
        } else if (targetChannels.value < getInputFormat().channelCount) {
            processor.addReSampler(MonoToStereoReSampler())
        }
        if (targetSampleRate > getInputFormat().sampleRate) {
            processor.addReSampler(SampleRateUpReSampler(getInputFormat().sampleRate, targetSampleRate, targetChannels))
        } else if (targetSampleRate < getInputFormat().sampleRate) {
            processor.addReSampler(SampleRateDownReSampler(getInputFormat().sampleRate, targetSampleRate, targetChannels))
        }
        audioDecoder.start()
    }

    private fun clearCache() = processor.clearCache()
}
