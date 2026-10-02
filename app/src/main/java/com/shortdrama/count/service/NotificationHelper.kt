package com.shortdrama.count.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.shortdrama.count.MainActivity
import com.shortdrama.count.R
import com.shortdrama.count.model.PushPayload
import kotlinx.serialization.json.Json

object NotificationHelper {
    const val CHANNEL_PUSH = "push_incoming"
    const val CHANNEL_FOREGROUND = "lan_service"

    const val EXTRA_PUSH_PAYLOAD = "extra_push_payload"
    const val EXTRA_PUSH_FROM = "extra_push_from"

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun ensureChannels(ctx: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(CHANNEL_PUSH) == null) {
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_PUSH, "接收推送", NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "收到来自其他设备的数据推送"
                    enableVibration(true)
                }
            )
        }
        if (nm.getNotificationChannel(CHANNEL_FOREGROUND) == null) {
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_FOREGROUND, "推送服务", NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "保持局域网/中继推送服务运行"
                    setShowBadge(false)
                }
            )
        }
    }

    private fun piFlags(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        else PendingIntent.FLAG_UPDATE_CURRENT

    fun showPushNotification(ctx: Context, payload: PushPayload, preview: String) {
        ensureChannels(ctx)
        val jsonStr = try {
            json.encodeToString(PushPayload.serializer(), payload)
        } catch (_: Exception) { "" }

        val intent = Intent(ctx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_PUSH_PAYLOAD, jsonStr)
            putExtra(EXTRA_PUSH_FROM, payload.sender)
        }
        val id = (System.currentTimeMillis() and 0x7fffffff).toInt()
        val pi = PendingIntent.getActivity(ctx, id, intent, piFlags())

        val body = payload.dramas.size.toString() + " 部剧 · " +
            payload.records.sumOf { it.count } + " 条广告"

        val notif = NotificationCompat.Builder(ctx, CHANNEL_PUSH)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("来自「" + payload.sender + "」的数据")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(preview.take(240)))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()

        try {
            NotificationManagerCompat.from(ctx).notify(id, notif)
        } catch (_: SecurityException) {}
    }

    fun buildForegroundNotification(ctx: Context, text: String): Notification {
        ensureChannels(ctx)
        val intent = Intent(ctx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pi = PendingIntent.getActivity(ctx, 1001, intent, piFlags())
        return NotificationCompat.Builder(ctx, CHANNEL_FOREGROUND)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("开饭了 · 推送服务运行中")
            .setContentText(text)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pi)
            .build()
    }
}
