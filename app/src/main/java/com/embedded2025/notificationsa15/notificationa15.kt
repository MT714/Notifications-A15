package com.embedded2025.notificationsa15

import android.app.Application

class notificationa15 : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationsHelper.initialize(this)
    }
}
