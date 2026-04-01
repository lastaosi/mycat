package com.lastaosi.mycat

import android.app.Application
import com.lastaosi.mycat.di.appModule
import com.lastaosi.mycat.di.databaseModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class MyCatApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@MyCatApplication)
            modules(
                databaseModule,
                appModule
            )
        }
    }
}