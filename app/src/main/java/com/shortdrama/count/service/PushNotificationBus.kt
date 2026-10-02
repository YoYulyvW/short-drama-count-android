package com.shortdrama.count.service

import com.shortdrama.count.model.PushPayload
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow

/** 收到推送后广播（局域网 / 中继都通过这里） */
object PushNotificationBus {
    private val _events = MutableSharedFlow<PushPayload>(extraBufferCapacity = 16)
    val events: SharedFlow<PushPayload> = _events
    fun post(p: PushPayload) { _events.tryEmit(p) }
}

/** 通知点击后携带的待处理 payload（Activity 启动时解析） */
object PushIntentBus {
    val pending = MutableStateFlow<PushPayload?>(null)
    fun post(p: PushPayload) { pending.value = p }
    fun consume(): PushPayload? { val v = pending.value; pending.value = null; return v }
}
