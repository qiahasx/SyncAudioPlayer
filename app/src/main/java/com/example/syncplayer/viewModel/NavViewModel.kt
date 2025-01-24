package com.example.syncplayer.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.syncplayer.util.launchIO
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class NavViewModel : ViewModel() {
    private val _navigationEvent = MutableSharedFlow<NavigationEvent>()
    val navigationEvent = _navigationEvent.asSharedFlow()

    fun navPlay() {
        viewModelScope.launchIO {
            _navigationEvent.emit(NavigationEvent.NavigationPlay)
        }
    }

    // 定义一个密封类来表示导航事件
    sealed class NavigationEvent {
        data object NavigationPlay : NavigationEvent()
    }
}