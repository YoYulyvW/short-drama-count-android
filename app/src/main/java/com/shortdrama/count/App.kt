package com.shortdrama.count

import android.app.Activity
import android.app.Application
import android.os.Bundle

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
        maybeStartPushService()
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            private var started = 0
            override fun onActivityStarted(activity: Activity) {
                started++
                if (started > 0) AppLifecycle.foreground = true
            }
            override fun onActivityStopped(activity: Activity) {
                started--
                if (started <= 0) { started = 0; AppLifecycle.foreground = false }
            }
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }

    private fun maybeStartPushService() {
        val sp = getSharedPreferences("drama_prefs", MODE_PRIVATE)
        val lanOn = sp.getBoolean("lan_enabled", false)
        val relayOn = sp.getBoolean("relay_enabled", false)
        if (lanOn || relayOn) {
            try {
                com.shortdrama.count.service.PushForegroundService.start(this)
            } catch (_: Exception) {}
        }
    }

    companion object {
        lateinit var instance: App
            private set
    }
}

/** 全局前后台标记（用于判断是否弹应用内弹窗还是仅系统通知） */
object AppLifecycle {
    @Volatile var foreground: Boolean = false
}
