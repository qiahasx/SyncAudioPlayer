package com.example.syncplayer.util

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

fun CoroutineScope.launchIO(
    consumeException: Boolean = false,
    block: suspend CoroutineScope.() -> Unit,
) = if (consumeException) {
    launch(Dispatchers.IO + createExceptionHandler()) { block.invoke(this) }
} else {
    launch(Dispatchers.IO) { block.invoke(this) }
}

suspend fun withIO(block: suspend () -> Unit) =
    withContext(Dispatchers.IO) {
        block.invoke()
    }

fun createExceptionHandler() = CoroutineExceptionHandler { _, throwable -> error("scope$throwable") }

fun LifecycleOwner.launchMain(
    consumeException: Boolean = true,
    block: suspend CoroutineScope.() -> Unit,
) = if (consumeException) {
    lifecycleScope.launch(Dispatchers.Main + createExceptionHandler()) { block.invoke(this) }
} else {
    lifecycleScope.launch(Dispatchers.Main) { block.invoke(this) }
}
