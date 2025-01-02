# SyncAudioPlayer

核心代码框架
与音乐播放器相关的代码包括
[图片]
包括
- 音频编码器： AudioDecoder
- 音频转换器：AudioCovert
- 混音器： AudioMixer
- 播放器： SyncPlayer
- 其中BlockQueue是一个会堵塞的队列，用于传输数据
- ShortInfo 记录传输的数据与相关信息
- AudioInfo 记录音频的信息
暂时无法在飞书文档外展示此内容
解码
主要包括这几个函数与字段
[图片]
`AudioDecoder` 只有三种状态：

1. **初始状态**：在调用 `start` 方法之前，即刚创建对象时。
2. **解码状态**：调用 `start` 方法后，进入解码过程。
3. **资源释放状态**：在资源被释放后，`AudioDecoder` 进入该状态。

一旦进入解码状态，解码器将持续提取数据并进行解码，随后将解码后的数据送入队列中。如果想要暂停解码过程，只需在外部停止对队列的消费。当队列满时，协程将被阻塞，从而停止进一步的解码操作。

start() 函数会启动MediaCodec, 然后再启动startInner()去创建解码任务
fun start() {
    decoder.start()
    startInner()
}
startInner()  函数的主要目的是在后台启动解码任务，使用协程循环提取（extractorData()）和解码数据(decoderData(info)）。
并且将 job 保存下来用于在需要的时候暂停
private fun startInner() {
    decodeJob = scope.launchIO {
        val info = BufferInfo()
        while (isActive) {
            extractorData()
            decoderData(info)
        }
    }
}
seekTo 函数实现了音频的跳转功能，确保在跳转时清理旧的解码状态，并根据新的时间点启动新的解码过程。通过取消旧任务、清空队列和刷新解码器，确保新的被输出数据从正确的位置开始。
suspend fun seekTo(timeUs: Long) {
    decodeJob?.cancelAndJoin()
    queue.clear()
    decoder.flush()
    val progress = timeUs.coerceAtLeast(0).coerceAtMost(audioInfo.duration)
    extractor.seekTo(progress, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
    if (progress == audioInfo.duration) {
        val shortsInfo = ShortsInfo(ShortArray(0), 0, 0, audioInfo.duration, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
        queue.produce(shortsInfo)
    } else {
        startInner()
    }
}

长度对齐
因为音频文件不同解码器取出的数据的长度不一，不好操作
为了方便操作使用AudioCovert中做中间转换，AudioMixer需要数据不从AudioDecoder中取，而是在AudioCovert中取
AuidoCovert会在从AudioDecoder取出一个ShortInfo，将数据分配到若干个指定长度的ShortInfo
[图片]
格式对齐
如果混音的音频存在解码后 码率不一致的情况
需要对齐，
不然会导致歌的播放速度不一，码率低的歌播放速度会更快
AudioCovert主要的工作就是做这个，需要根据采样率，声道数，采样位深来把数据做一下转换

混音
AudioMiexer的状态与AudioDecoder状态一样
一旦开始就不会停下来，如果暂停也会往往队列中传递数据知道堵塞。
[图片]
工作时候的流程和AudioDecoder也类似
addAudioSource 函数 添加音频文件的路径，创建一个AudioCovert 然后返回一个int值表示 添加文件的id，后面可以用这个id来设置音量
fun addAudioSource(path: String): Int {
    map[++cnt] = AudioCovert(AudioDecoder(scope, path), BUFFER_SIZE)
    return cnt
}
调用start 后，类似AudioDecoder会去全部启动decoder
然后开一个协程域在里面做具体的混音工作
fun start() {
    if (map.isEmpty()) throw IllegalStateException("Not Add DataSource")
    map.values.forEach { it.audioDecoder.start() }
    mixJob = startInner()
}
seekTo 
这里手动delay() 是因为如果没有这个会导致播放的音乐突然跳到目标进度，会有一种很突兀的感觉
suspend fun seekTo(timeUs: Long) {
    // 暂停混音清空队列
    mixJob?.cancelAndJoin()
    queue.clear()
    delay(100)
    // 轮询调用decoder的seekTo，然后等待
    map.values.map {
        scope.async {
            it.clearCache()
            it.audioDecoder.seekTo(timeUs)
        }
    }.awaitAll()
    // 恢复混音任务
    mixJob = startInner()
}
setVolume
setVolume 目标就一个去改变volumeMap(ArrayMap<Int, Float>())中指定id为key的value
因为在混音中从AudioCovert中取出的值会去*volumeMap对应的值做为音量的调节
[图片]
如果是把Map的value类型改成AtomicReference<Float>之类的来
保证可见性
那么我们完全可以不关闭混音的协程，直接改变对应的值就行。
但是我们不是混音后立刻播放，而是放到一个队列中缓存下来，等待轮到它播放的时候再取出了
而目前设置的缓存区是2048*2048 假如填满对于常见的44.1khz 双声道 16位的音频文件来说大概是缓存了20秒，这20秒的数据都是调整音量之前的数据。
所以改完map的值后，需要清空队列
然后再让解码器seekTo到原本的队列队首的位置。
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
    mixJob = startInner()
}
混音算法
把多个音频混合到一起成为一道音频的原理很简单，就是把各自音频解码出来的PCM数据（一个数组）加起来。
目前默认pcm位数是16，一个数据使用一个Short保存
如果多个Short相加很有可能导致溢出，溢出后就会导入杂音
所以混音算法的关键就是处理溢出后的情况
目前是写死了用自适应混音加权 衰减因子法
当出现溢出情况时，首先将混合值削减到最大值。接着，我们计算比率，并从该位置开始修正后续的数据，将其乘以这个比率。如果在后续的数据处理中没有再次发生溢出，我们会按照一定的速率逐渐恢复到正常的比率。
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

播放器
播放器就简单了，主要是做一个状态的管理，调用AudioMixer的函数
和从AudioMixer取出数据后交给AudioTrack播放
[图片]
