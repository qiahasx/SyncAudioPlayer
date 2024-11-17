package com.example.syncplayer

import java.io.File

fun main() {
    val path = "E:\\android\\SyncPlayer\\app\\src\\main\\cpp"
    File(path).listFiles()?.forEach { file ->
        val fileName = file.name
        if (fileName.endsWith(".c") || fileName.endsWith(".cpp") || fileName.endsWith(".h")) {
            println(fileName)
        }
    }
}