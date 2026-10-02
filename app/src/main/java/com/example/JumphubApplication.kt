package com.example

import android.app.Application
import com.example.data.AppContainer

class JumphubApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
