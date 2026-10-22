package com.medicitas.movil

import android.app.Application
import com.medicitas.movil.di.AppContainer

class MediCitasApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
