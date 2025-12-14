package com.example.imageloading

import android.app.Application

/**
 * Application class to initialize ImageLoader
 */
class ImageLoadingApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        // Initialize ImageLoader on app startup
        ImageLoader.init(this)
    }
}

