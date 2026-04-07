package com.lastaosi.mycat

import android.app.Application
import com.google.android.gms.ads.MobileAds
import com.google.android.libraries.places.api.Places
import com.lastaosi.mycat.di.appModule
import com.lastaosi.mycat.di.databaseModule
import com.lastaosi.mycat.util.NotificationHelper
import com.lastaosi.mycat.worker.WorkManagerScheduler
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class MyCatApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Places 초기화 추가
        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, BuildConfig.MAPS_API_KEY)
        }
        startKoin {
            androidLogger()
            androidContext(this@MyCatApplication)
            modules(
                databaseModule,
                appModule
            )
        }
        NotificationHelper.createChannels(this)
        // WorkManager 스케줄 등록
        WorkManagerScheduler.scheduleVaccinationReminder(this)
        MobileAds.initialize(this)
    }
}