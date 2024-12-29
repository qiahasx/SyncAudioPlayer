package com.example.syncplayer

import android.app.Application

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        context = this
    }

    companion object {
        lateinit var context: App
    }
}
