package com.sora.android

import android.app.Application
import android.util.Log
import com.sora.android.core.startup.DataInitializer
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class SoraApplication : Application() {

    @Inject
    lateinit var dataInitializer: DataInitializer

    override fun onCreate() {
        super.onCreate()

        Log.d("SoraApplication", "Application starting - initializing essential data...")

        dataInitializer.initializeEssentialData()

        Log.d("SoraApplication", "Application initialized successfully")
    }
}