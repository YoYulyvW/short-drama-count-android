package com.shortdrama.count.util

import android.media.AudioAttributes
import android.media.AudioManager
import android.media.RingtoneManager
import com.shortdrama.count.App

/**
 * 前台收到推送时的提示音。用系统通知音，不占用媒体通道，不影响正在播放的音乐。
 */
object SoundAlert {
    fun playNotification() {
        try {
            val ctx = App.instance
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ?: return
            val ringtone = RingtoneManager.getRingtone(ctx, uri) ?: return
            ringtone.audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            ringtone.play()
        } catch (_: Exception) {}
    }
}
