package com.example.syncplayer.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import com.example.syncplayer.util.launchIO
import com.example.syncplayer.util.withIO
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive

class SyncPlayer(
    private val scope: CoroutineScope,
    private val onStateChange: ((State) -> Unit)? = null,
) {
    private val mix = AudioMixer(scope)
    private lateinit var audioTrack: AudioTrack
    private var state: State = State.CREATE
    private var playJob: Job? = null
    val progress = MutableStateFlow(0L)

    @Throws(IllegalStateException::class)
    fun setDataSource(path: String): Int {
        require(state < State.START) { "must before START" }
        return mix.addAudioSource(path)
    }

    fun start() {
        require(state < State.START) { "Start prohibits repeated calls" }
        mix.start()
        audioTrack = initAudioTrack()
        state = State.START
        onStateChange?.invoke(State.START)
        audioTrack.play()
        playJob = startInner()
        state = State.PLAYING
        onStateChange?.invoke(State.PLAYING)
    }

    fun getDuration(): Long {
        return mix.getDuration()
    }

    fun resume() {
        require(state == State.PAUSE) { "Resume prohibits calls when the state is not PAUSE" }
        audioTrack.play()
        playJob = startInner()
        state = State.PLAYING
        onStateChange?.invoke(State.PLAYING)
    }

    fun pause() {
        require(state == State.PLAYING) { "Pause prohibits calls when the state is not PLAYING" }
        audioTrack.pause()
        playJob?.cancel()
        state = State.PAUSE
        onStateChange?.invoke(State.PAUSE)
    }

    fun release() {
        require(state > State.RELEASE) { "Release prohibits calls when the state is before DESTROY" }
        audioTrack.release()
        state = State.RELEASE
        onStateChange?.invoke(State.RELEASE)
    }

    suspend fun seekTo(timeUs: Long) =
        withIO {
            require(state == State.PLAYING) { "SeekTo prohibits calls when the state is not PLAYING" }
            mix.seekTo(timeUs)
        }

    private fun startInner() =
        scope.launchIO {
            while (isActive) {
                val bytesInfo = mix.queue.consume()
                progress.emit(bytesInfo.sampleTime)
                if (bytesInfo.flags == 4) {
                    completed()
                    break
                }
                audioTrack.write(bytesInfo.floats, bytesInfo.offset, bytesInfo.size, AudioTrack.WRITE_BLOCKING)
            }
        }

    private suspend fun completed() {
        seekTo(0)
        pause()
    }

    private fun initAudioTrack(): AudioTrack {
        val sampleRate = mix.getSampleRate()
        val channelCount = mix.getChannelCount()
        val channelMask = coverChannelCountToChannelMask(channelCount)
        val audioAttributes =
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
        val format =
            AudioFormat.Builder()
                .setSampleRate(sampleRate)
                .setChannelMask(channelMask)
                .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                .build()
        val bufferSize: Int =
            AudioTrack.getMinBufferSize(sampleRate, channelMask, AudioFormat.ENCODING_PCM_FLOAT)
        return AudioTrack(
            audioAttributes,
            format,
            bufferSize,
            AudioTrack.MODE_STREAM,
            AudioManager.AUDIO_SESSION_ID_GENERATE
        )
    }

    private fun coverChannelCountToChannelMask(channelCount: Int) =
        when (channelCount) {
            2 -> AudioFormat.CHANNEL_OUT_STEREO
            else -> AudioFormat.CHANNEL_OUT_MONO
        }

    fun setVolume(id: Int, volume: Float) {
        require(state > State.START) { "must after START" }
        scope.launchIO {
            mix.setVolume(id, volume)
        }
    }

    enum class State {
        RELEASE,
        CREATE,
        START,
        PAUSE,
        PLAYING,
    }
}
