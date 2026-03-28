package com.kakao.taxi.di

import android.app.NotificationManager
import android.content.Context
import android.net.ConnectivityManager
import android.os.PowerManager
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import com.kakao.taxi.data.repository.DataStoreRepository
import com.kakao.taxi.data.repository.NetworkRepository
import com.kakao.taxi.data.repository.dataStore
import com.kakao.taxi.data.source.impl.SpeedDataSource
import com.kakao.taxi.service.NotificationHelper
import com.kakao.taxi.ui.overlay.OverlayWindow

val appModule = module {
    single { androidContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager }
    single { androidContext().getSystemService(Context.POWER_SERVICE) as PowerManager }
    single { androidContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }
    single { DataStoreRepository(androidContext().dataStore) }
    single { SpeedDataSource(get()) }

    single { NetworkRepository(get(), get()) }

    factory { NotificationHelper(androidContext()) }
    factory { OverlayWindow(androidContext(), get()) }
}
