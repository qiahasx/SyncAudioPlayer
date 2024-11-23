package com.example.syncplayer.audio

import androidx.collection.ArrayMap
import com.example.syncplayer.queue.BlockQueue
import com.example.syncplayer.util.launchIO
import kotlinx.coroutines.CoroutineScope
import kotlin.math.tanh

class AudioMixer(private val scope: CoroutineScope) {
    private val map = ArrayMap<Int, Decoder>()
    val queue = BlockQueue<FloatsInfo>(4)
    private var cnt = 0

    fun addAudioSource(path: String): Int {
        map[++cnt] = Decoder(scope, path)
        return cnt
    }

    fun getSampleRate(): Int {
        if (map.isEmpty) return -1
        return map.values.maxOf { it.audioInfo.sampleRate }
    }

    fun getChannelCount(): Int {
        if (map.isEmpty) return -1
        return map.values.maxOf { it.audioInfo.channelCount }
    }

    fun start() {
        if (map.isEmpty) throw IllegalStateException("Not Add DataSource")
        scope.launchIO {
            startInner()
        }
    }

    // TODO MediaCodec输出的buffer的大小在网上没能找到控制的方式，也许不一样，不应该直接相加
    // TODO 不同音轨的声道数和采样率可能不同直接相加pcm数据时间对不上，需转换
    private suspend fun startInner() {
        map.values.forEach { it.start() }
        while (true) {
            val shortMap = ArrayMap<Int, ShortsInfo>()
            for ((id, decoder) in map.entries) {
                shortMap[id] = decoder.consume()
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
        }
    }

    private fun FloatArray.addShortInfo(info: ShortsInfo) {
        val offset = info.offset
        for (i in offset until offset + info.size) {
            this[i - offset] += info.shorts[i] / MAX_SHORT_F
        }
    }

    companion object {
        const val MAX_SHORT_F = Short.MAX_VALUE.toFloat()
    }
}
