package com.shortdrama.count.model

import com.shortdrama.count.App

object AppVersion {
    val name: String
        get() = try {
            App.instance.packageManager
                .getPackageInfo(App.instance.packageName, 0).versionName ?: "1.0.0"
        } catch (_: Exception) { "1.0.0" }

    val code: Long
        get() = try {
            val pi = App.instance.packageManager.getPackageInfo(App.instance.packageName, 0)
            if (android.os.Build.VERSION.SDK_INT >= 28) pi.longVersionCode
            else @Suppress("DEPRECATION") pi.versionCode.toLong()
        } catch (_: Exception) { 1L }

    val full: String get() = "$name ($code)"
}
