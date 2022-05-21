package com.mustly.wellmedia

import android.app.Application
import android.content.Context

class MediaApplication : Application() {
    companion object {
        @JvmStatic
        private lateinit var appContext: Context

        @JvmStatic
        fun getAppContext(): Context {
            return appContext
        }
    }

    override fun onCreate() {
        super.onCreate()

        appContext = applicationContext
    }
}