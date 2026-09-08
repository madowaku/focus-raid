package com.madowaku.focusraid

import android.app.Application
import com.madowaku.focusraid.billing.RevenueCatRuntime

class FocusRaidApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        RevenueCatRuntime.configure(this)
    }
}
