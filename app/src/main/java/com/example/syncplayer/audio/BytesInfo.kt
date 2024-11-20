package com.example.syncplayer.audio

class BytesInfo(
    val bytes: ByteArray,
    var offset: Int = 0,
    var size: Int = 0,
    val sampleTime: Long = 0,
    val flags: Int = 0,
)