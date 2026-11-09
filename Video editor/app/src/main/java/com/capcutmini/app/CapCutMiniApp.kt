package com.capcutmini.app

import android.app.Application
import com.capcutmini.app.di.AppContainer

class CapCutMiniApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
