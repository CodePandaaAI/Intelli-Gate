package com.liftley.intelligate

import android.app.Application
import com.liftley.intelligate.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class IntelliGateApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@IntelliGateApplication)
            modules(appModule)
        }
    }
}

