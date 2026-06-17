package com.bruce.controller

import android.app.Application
import com.bruce.controller.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class BruceApp : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger()
            androidContext(this@BruceApp)
            modules(appModule)
        }
    }
}
