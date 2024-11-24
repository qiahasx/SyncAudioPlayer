package com.example.syncplayer.audio

import androidx.collection.ArrayMap
import com.example.syncplayer.queue.BlockQueue
import com.example.syncplayer.util.launchIO
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.sync.Mutex
import kotlin.math.tanh

class AudioMixer(private val scope: CoroutineScope) {
    val queue = BlockQueue<FloatsInfo>(4)
    private val mutex = Mutex()
    private val map = ArrayMap<Int, AudioCovert>()
    private var cnt = 0

    fun addAudioSource(path: String): Int {
        map[++cnt] = AudioCovert(Decoder(scope, path), BUFFER_SIZE)
        return cnt
    }

    fun getSampleRate(): Int {
        if (map.isEmpty) return -1
        return map.values.maxOf { it.decoder.audioInfo.sampleRate }
    }

    fun getChannelCount(): Int {
        if (map.isEmpty) return -1
        return map.values.maxOf { it.decoder.audioInfo.channelCount }
    }

    fun start() {
        if (map.isEmpty) throw IllegalStateException("Not Add DataSource")
        scope.launchIO {
            startInner()
        }
    }

    suspend fun seekTo(timeUs: Long) {
        mutex.lock()
        map.values.map {
            scope.async {
                it.clearCache()
                it.decoder.seekTo(timeUs)
            }
        }.awaitAll()
        mutex.unlock()
    }

    // TODO 不同音轨的声道数和采样率可能不同直接相加pcm数据，时间对不上，需转换
    private suspend fun startInner() {
        map.values.forEach { it.decoder.start() }
        while (true) {
            mutex.lock()
            val shortMap = ArrayMap<Int, ShortsInfo>()
            for ((id, decoder) in map.entries) {
                shortMap[id] = decoder.getBuffer()
            }
            val firstInfo = shortMap.values.iterator().next()
            val length = firstInfo.size
            val floats = FloatArray(length)
            shortMap.values.forEach { info ->
                floats.addShortInfo(info)
            }
            for (i in floats.indices) {
                // TODO 这个混音逻辑不行 后续做成可以由用户配置的
                floats[i] = tanh(floats[i])
            }
            queue.produce(FloatsInfo(floats, 0, length, firstInfo.sampleTime, firstInfo.flags))
            mutex.unlock()
        }
    }

    private fun FloatArray.addShortInfo(info: ShortsInfo) {
        val offset = info.offset
        for (i in offset until offset + info.size) {
            this[i - offset] += info.shorts[i] / MAX_SHORT_F / 3
        }
    }

    companion object {
        const val MAX_SHORT_F = Short.MAX_VALUE.toFloat()
        const val BUFFER_SIZE = 256
    }
}
