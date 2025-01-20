package com.example.syncplayer.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.syncplayer.App
import com.example.syncplayer.audio.AudioSyncPlayer
import com.example.syncplayer.audio.AudioTranscoder
import com.example.syncplayer.model.AudioItem
import com.example.syncplayer.util.launchIO
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class MainViewModel : ViewModel() {
    private val _items = MutableStateFlow<List<AudioItem>>(emptyList())
    val items: StateFlow<List<AudioItem>> = _items.asStateFlow()

    private val _navigationEvent = MutableSharedFlow<NavigationEvent>()
    val navigationEvent = _navigationEvent.asSharedFlow()

    private val _playProgress = MutableStateFlow(0f)
    val playProgress: StateFlow<Float> get() = _playProgress

    private val _totalDuration = MutableStateFlow(330 * 1000f)
    val totalDuration: StateFlow<Float> get() = _totalDuration

    private val _snackbarMessage = MutableSharedFlow<String>()
    val snackbarMessage = _snackbarMessage.asSharedFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> get() = _isPlaying

    val audioTranscoders = MutableStateFlow<AudioTranscoder?>(null)

    private val player by lazy {
        AudioSyncPlayer(
            viewModelScope,
        ) { state ->
            viewModelScope.launchIO {
                _isPlaying.emit(state == AudioSyncPlayer.State.PLAYING)
            }
        }
    }

    init {
        updateItem()
    }

    fun initPlayer() {
        viewModelScope.launchIO {
            _items.value.forEach {
                it.id = player.setDataSource(it.filePath)
            }
            player.start()
            _totalDuration.emit(player.getDuration() / 1000f)
            player.progress.collect {
                _playProgress.emit(it / 1000f)
            }
        }
    }

    fun seekTo(ms: Float) {
        viewModelScope.launchIO {
            player.seekTo(ms.toLong() * 1000)
        }
    }

    fun forward() {
        viewModelScope.launchIO {
            seekTo(_playProgress.value + 5000)
        }
    }

    fun backward() {
        viewModelScope.launchIO {
            seekTo(_playProgress.value - 5000)
        }
    }

    fun togglePlay() {
        if (isPlaying.value) {
            player.pause()
        } else {
            player.resume()
        }
    }

    fun updateItem() {
        val audioItems =
            App.context.getExternalFilesDir(AUDIO_PATH)?.listFiles()?.map {
                AudioItem(it.name, it.absolutePath)
            } ?: emptyList()
        viewModelScope.launchIO {
            _items.emit(audioItems)
        }
    }

    fun deleteItem(item: AudioItem) {
        val file = File(item.filePath)
        if (file.exists()) {
            file.delete()
        }
        updateItem()
    }

    fun onClickStart() {
        if (items.value.isNotEmpty()) {
            viewModelScope.launch {
                _navigationEvent.emit(NavigationEvent.NavigateToNextScreen)
            }
        } else {
            viewModelScope.launch {
                _snackbarMessage.emit("No audio item available")
            }
        }
    }

    fun setVolume(item: AudioItem, volume: Float) {
        player.setVolume(item.id, volume)
        item.volume.value = volume
    }

    fun createAudioTranscoders(item: AudioItem) {
        require(audioTranscoders.value == null) { "have audioTranscoder not release" }
        viewModelScope.launchIO {
            audioTranscoders.emit(AudioTranscoder(item, viewModelScope))
        }
    }

    fun releaseAudioTranscoders() {
        audioTranscoders.value?.release()
        audioTranscoders.value = null
    }

    fun startAudioTranscoders() {
        audioTranscoders.value?.start()
        // 释放资源
        audioTranscoders.value = null
    }

    // 定义一个密封类来表示导航事件
    sealed class NavigationEvent {
        data object NavigateToNextScreen : NavigationEvent()
    }

    companion object {
        const val AUDIO_PATH = "audio"
    }
}
