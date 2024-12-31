package com.example.syncplayer.audio

import androidx.collection.ArrayMap
import com.example.syncplayer.queue.BlockQueue
import com.example.syncplayer.util.launchIO
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

class AudioMixer(private val scope: CoroutineScope) {
    val queue = BlockQueue<ShortsInfo>(BUFFER_NUM)
    private var mixJob: Job? = null
    private val map = ArrayMap<Int, AudioCovert>()
    private val volumeMap = ArrayMap<Int, Float>()
    private var cnt = 0
    private var attenuationFactor = 1f

    fun addAudioSource(path: String): Int {
        map[++cnt] = AudioCovert(AudioDecoder(scope, path), BUFFER_SIZE)
        return cnt
    }

    fun getSampleRate(): Int {
        if (map.isEmpty()) return -1
        return map.values.maxOf { it.audioDecoder.audioInfo.sampleRate }
    }

    fun getChannelCount(): Int {
        if (map.isEmpty()) return -1
        return map.values.maxOf { it.audioDecoder.audioInfo.channelCount }
    }

    fun start() {
        if (map.isEmpty()) throw IllegalStateException("Not Add DataSource")
        map.values.forEach { it.audioDecoder.start() }
        mixJob = startInner()
    }

    fun getDuration(): Long {
        if (map.isEmpty()) return -1
        return map.values.maxOf { it.audioDecoder.audioInfo.duration }
    }

    suspend fun seekTo(timeUs: Long) {
        mixJob?.cancelAndJoin()
        queue.clear()
        delay(200)
        map.values.map {
            scope.async {
                it.clearCache()
                it.audioDecoder.seekTo(timeUs)
            }
        }.awaitAll()
        mixJob = startInner()
    }

    suspend fun setVolume(id: Int, volume: Float) {
        mixJob?.cancelAndJoin()
        volumeMap[id] = volume
        val info = queue.tryConsume()
        if (info != null) {
            queue.clear()
            map.values.map {
                scope.async {
                    it.clearCache()
                    it.audioDecoder.seekTo(info.sampleTime)
                }
            }.awaitAll()
        }
        delay(200)
        mixJob = startInner()
    }

    private fun startInner() = scope.launchIO {
        while (isActive) {
            val shortMap = ArrayMap<Int, ShortsInfo>()
            for ((id, decoder) in map.entries) {
                shortMap[id] = decoder.getBuffer()
            }
            queue.produce(mix(shortMap))
        }
    }

    private fun mix(shortMap: ArrayMap<Int, ShortsInfo>): ShortsInfo {
        val firstInfo = shortMap.values.iterator().next()
        val size = BUFFER_SIZE
        val shorts = ShortArray(size)
        for (i in 0 until size) {
            var mixVal = 0f
            shortMap.forEach { (id, info) ->
                mixVal += info.shorts[info.offset++] * volumeMap.getOrPut(id) { 1f }
            }
            mixVal *= attenuationFactor
            when {
                mixVal > Short.MAX_VALUE -> {
                    attenuationFactor = Short.MAX_VALUE / mixVal
                    mixVal = Short.MAX_VALUE.toFloat()
                }

                mixVal < Short.MIN_VALUE -> {
                    attenuationFactor = Short.MIN_VALUE / mixVal
                    mixVal = Short.MIN_VALUE.toFloat()
                }
            }
            if (attenuationFactor < 1) {
                attenuationFactor += (1 - attenuationFactor) / STEP_SIZE
            }
            shorts[i] = mixVal.toInt().toShort()
        }

        return ShortsInfo(shorts, 0, size, firstInfo.sampleTime, firstInfo.flags)
    }

    private fun FloatArray.addShortInfo(id: Int, info: ShortsInfo) {
        val offset = info.offset
        for (i in offset until offset + info.size) {
            this[i - offset] += info.shorts[i] * volumeMap.getOrPut(id) { 1f } / MAX_SHORT_F
        }
    }

    companion object {
        const val MAX_SHORT_F = Short.MAX_VALUE.toFloat()
        const val BUFFER_SIZE = 2048
        const val BUFFER_NUM = 2048
        const val STEP_SIZE = 32
    }
}
