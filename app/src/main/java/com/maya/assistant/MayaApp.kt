package com.maya.assistant

import android.app.Application
import com.maya.assistant.data.PreferencesManager

class MayaApp : Application() {
    lateinit var prefs: PreferencesManager
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        prefs = PreferencesManager(this)
    }

    companion object {
        lateinit var instance: MayaApp
            private set
    }
}
