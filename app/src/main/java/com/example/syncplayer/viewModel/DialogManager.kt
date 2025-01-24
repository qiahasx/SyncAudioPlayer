package com.example.syncplayer.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.syncplayer.ui.dialog.DialogController
import com.example.syncplayer.util.launchIO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class DialogManager : ViewModel() {
    private val _dialog = MutableStateFlow<List<DialogController>>(mutableListOf())
    val dialog = _dialog.asStateFlow()

    fun show(controller: DialogController) {
        viewModelScope.launchIO {
            val list = ArrayList(dialog.value)
            list.add(controller)
            _dialog.emit(list)
        }
    }

    fun dismiss(controller: DialogController) {
        viewModelScope.launchIO {
            val list = ArrayList(dialog.value)
            list.remove(controller)
            _dialog.emit(list)
        }
    }
}