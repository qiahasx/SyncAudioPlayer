package com.example.syncplayer.audio

class AudioCovert(
    private val audioDecoder: AudioDecoder,
    bufferSize: Int,
) {
    private val processor = PcmBufferProcessor(audioDecoder.queue, bufferSize)

    suspend fun getBuffer() = processor.getBuffer()

    fun getInputFormat() = audioDecoder.audioInfo

    suspend fun seekTo(timeUs: Long) {
        clearCache()
        audioDecoder.seekTo(timeUs)
    }

    fun release() = audioDecoder.release()

    fun start() = audioDecoder.start()

    private fun clearCache() = processor.clearCache()
}
