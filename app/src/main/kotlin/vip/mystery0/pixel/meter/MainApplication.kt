package com.kakao.taxi

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import com.kakao.taxi.di.appModule
import com.kakao.taxi.service.NotificationHelper

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger()
            androidContext(this@MainApplication)
            modules(appModule)
        }

        // Initialize Notification Channel immediately
        NotificationHelper.createNotificationChannel(this)
    }
}
