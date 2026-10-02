package com.shortdrama.count.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * 常驻前台服务：
 *   - 承载 LanServer（同 WiFi 直连接收）
 *   - 承载 RelayClient（跨网中继接收）
 * 收到推送 → PushNotificationBus 广播 + 弹系统通知
 */
class PushForegroundService : Service() {

    private val serviceScope = kotlinx.coroutines.CoroutineScope(
        kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.IO
    )

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.ensureChannels(this)

        // 从设置读中继配置（懒加载）
        val sp = getSharedPreferences("drama_prefs", Context.MODE_PRIVATE)
        val relayUrl = sp.getString("relay_url", "") ?: ""
        val relayEnabled = sp.getBoolean("relay_enabled", false)

        RelayClient.onMessage = { payload ->
            val preview = payloadToPreview(payload)
            if (com.shortdrama.count.AppLifecycle.foreground) {
                // 前台：交给 App 内弹询问框
                PushNotificationBus.post(payload)
            } else {
                // 后台：弹系统通知，点击后进 App 再询问
                NotificationHelper.showPushNotification(this, payload, preview)
            }
        }

        // 启动局域网服务（如果已启用）
        if (sp.getBoolean("lan_enabled", false)) {
            LanServer.start()
            // Service 层订阅（兜底：VM 未创建时也能弹通知）
            serviceScope.launch {
                LanServer.pushEvents.collect { payload ->
                    if (!com.shortdrama.count.AppLifecycle.foreground) {
                        NotificationHelper.showPushNotification(
                            this@PushForegroundService, payload,
                            payloadToPreview(payload)
                        )
                    }
                }
            }
        }

        // 启动中继
        if (relayEnabled && relayUrl.isNotEmpty()) {
            RelayClient.start(
                url = relayUrl,
                devId = DeviceDiscovery.selfDeviceId(),
                devName = DeviceDiscovery.selfDisplayName()
            )
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notif = NotificationHelper.buildForegroundNotification(this, statusText())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceCompat.startForeground(
                this, NOTIF_ID, notif,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NOTIF_ID, notif)
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        serviceScope.cancel()
        RelayClient.stop()
        LanServer.stop()
        super.onDestroy()
    }

    private fun statusText(): String {
        return when (RelayClient.state.value) {
            RelayClient.State.CONNECTED -> "中继已连接 · 局域网监听中"
            RelayClient.State.CONNECTING -> "中继连接中…"
            RelayClient.State.ERROR -> "中继异常，重试中…"
            RelayClient.State.IDLE -> "局域网监听中"
        }
    }

    private fun payloadToPreview(p: com.shortdrama.count.model.PushPayload): String {
        val sb = StringBuilder()
        sb.append("日期 ").append(p.date).append("\n")
        for (d in p.dramas) {
            val recs = p.records.filter { it.title == d.title && it.isFast == d.isFast }
            if (recs.isEmpty()) continue
            sb.append("【").append(if (d.isFast) d.title + " - 极速" else d.title).append("】")
            sb.append(recs.joinToString(" | ") { it.platform.take(1) + ":" + it.count })
            sb.append("\n")
        }
        return sb.toString()
    }

    companion object {
        const val NOTIF_ID = 8848

        fun start(ctx: Context) {
            val i = Intent(ctx, PushForegroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ctx.startForegroundService(i)
            } else {
                ctx.startService(i)
            }
        }

        fun stop(ctx: Context) {
            ctx.stopService(Intent(ctx, PushForegroundService::class.java))
        }
    }
}
