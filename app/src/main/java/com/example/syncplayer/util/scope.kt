package com.example.syncplayer.util

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

fun CoroutineScope.launchIO(
    consumeException: Boolean = true,
    block: suspend () -> Unit,
) = if (consumeException) {
    launch(Dispatchers.IO + createExceptionHandler()) { block.invoke() }
} else {
    launch(Dispatchers.IO) { block.invoke() }
}

suspend fun withIO(block: suspend () -> Unit) =
    withContext(Dispatchers.IO) {
        block.invoke()
    }

fun createExceptionHandler() = CoroutineExceptionHandler { _, throwable -> error("scope$throwable") }
