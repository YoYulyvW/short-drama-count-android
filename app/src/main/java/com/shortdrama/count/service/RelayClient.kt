package com.shortdrama.count.service

import com.shortdrama.count.model.PushPayload
import com.shortdrama.count.model.PushDevice
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * 中继服务器客户端（跨网推送）。
 *   POST /relay/register  -> relayToken
 *   POST /relay/send      -> 转发
 *   GET  /relay/poll      -> 长轮询
 *   POST /relay/ack       -> 确认
 *   GET  /relay/devices   -> 在线设备
 */
object RelayClient {
    enum class State { IDLE, CONNECTING, CONNECTED, ERROR }

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private var scope: CoroutineScope? = null
    private var loopJob: Job? = null

    private val _state = MutableStateFlow(State.IDLE)
    val state: StateFlow<State> = _state

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError

    @Volatile private var baseUrl: String = ""
    @Volatile private var token: String = ""
    @Volatile private var deviceId: String = ""
    @Volatile private var deviceName: String = ""

    /** 收到消息的回调，Service 里注入 */
    @Volatile var onMessage: ((PushPayload) -> Unit)? = null

    fun start(url: String, devId: String, devName: String) {
        if (url.isBlank() || devId.isBlank()) return
        if (loopJob?.isActive == true && baseUrl == url.trimEnd('/') && deviceId == devId) return
        stop()
        baseUrl = url.trimEnd('/')
        deviceId = devId
        deviceName = devName
        val s = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope = s
        loopJob = s.launch { registerAndLoop() }
    }

    fun stop() {
        loopJob?.cancel()
        loopJob = null
        scope?.cancel()
        scope = null
        token = ""
        _state.value = State.IDLE
    }

    fun currentToken(): String = token

    // ---------------- 主循环 ----------------
    private suspend fun registerAndLoop() {
        var backoff = 1000L
        while (currentCoroutineContext().isActive) {
            try {
                if (token.isEmpty()) {
                    _state.value = State.CONNECTING
                    val t = doRegister()
                    if (t.isNullOrEmpty()) {
                        _state.value = State.ERROR
                        _lastError.value = "注册失败"
                        delay(backoff); backoff = minOf(backoff * 2, 30_000L)
                        continue
                    }
                    token = t
                    _state.value = State.CONNECTED
                    _lastError.value = null
                    backoff = 1000L
                }

                val msgs = doPoll()
                if (msgs == null) {
                    token = ""
                    _state.value = State.ERROR
                    _lastError.value = "长轮询断开"
                    delay(backoff); backoff = minOf(backoff * 2, 30_000L)
                    continue
                }
                backoff = 1000L
                _state.value = State.CONNECTED
                for (m in msgs) {
                    val payload = m.second
                    val mid = m.first
                    try { onMessage?.invoke(payload) } catch (_: Exception) {}
                    try { doAck(listOf(mid)) } catch (_: Exception) {}
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                token = ""
                _state.value = State.ERROR
                _lastError.value = e.message
                delay(backoff); backoff = minOf(backoff * 2, 30_000L)
            }
        }
    }

    // ---------------- HTTP ----------------
    private fun open(path: String, method: String, withAuth: Boolean): HttpURLConnection {
        val conn = URL(baseUrl + path).openConnection() as HttpURLConnection
        conn.requestMethod = method
        conn.connectTimeout = 10_000
        conn.readTimeout = 35_000
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
        conn.setRequestProperty("Accept", "application/json")
        if (withAuth && token.isNotEmpty()) {
            conn.setRequestProperty("Authorization", "Bearer " + token)
        }
        return conn
    }

    private fun readAll(conn: HttpURLConnection): String? {
        return try {
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            stream?.bufferedReader()?.use(BufferedReader::readText)
        } catch (e: Exception) { null }
    }

    private fun doRegister(): String? {
        val conn = open("/relay/register", "POST", withAuth = false)
        val body = buildJsonObject {
            put("deviceId", deviceId)
            put("deviceName", deviceName)
            put("platform", "android")
            put("pushMode", "poll")
            put("version", com.shortdrama.count.model.AppVersion.name)
        }.toString()
        return try {
            conn.doOutput = true
            conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val text = readAll(conn) ?: return null
            val obj = json.parseToJsonElement(text).jsonObject
            if (obj["ok"]?.jsonPrimitive?.content != "true") return null
            obj["relayToken"]?.jsonPrimitive?.content
        } catch (e: Exception) { null } finally {
            try { conn.disconnect() } catch (_: Exception) {}
        }
    }

    /** 返回 [messageId -> payload] 列表 */
    private fun doPoll(): List<Pair<String, PushPayload>>? {
        val conn = open("/relay/poll?timeout=25", "GET", withAuth = true)
        return try {
            val text = readAll(conn) ?: return null
            val obj = json.parseToJsonElement(text).jsonObject
            if (obj["ok"]?.jsonPrimitive?.content != "true") return null
            val arr = obj["messages"]?.jsonArray ?: return emptyList()
            val out = mutableListOf<Pair<String, PushPayload>>()
            for (el in arr) {
                val o = el.jsonObject
                val mid = o["messageId"]?.jsonPrimitive?.content ?: continue
                val type = o["type"]?.jsonPrimitive?.content ?: "push"
                if (type != "push") continue
                val payloadEl = o["payload"] ?: continue
                try {
                    val p = json.decodeFromJsonElement(PushPayload.serializer(), payloadEl)
                    out.add(mid to p)
                } catch (_: Exception) {}
            }
            out
        } catch (e: Exception) { null } finally {
            try { conn.disconnect() } catch (_: Exception) {}
        }
    }

    private fun doAck(ids: List<String>) {
        if (ids.isEmpty()) return
        val conn = open("/relay/ack", "POST", withAuth = true)
        val body = buildJsonObject {
            putJsonArray("messageIds") { ids.forEach { add(kotlinx.serialization.json.JsonPrimitive(it)) } }
        }.toString()
        try {
            conn.doOutput = true
            conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            readAll(conn)
        } catch (_: Exception) {} finally {
            try { conn.disconnect() } catch (_: Exception) {}
        }
    }

    /** 发送（挂起） */
    suspend fun send(toDeviceId: String, payload: PushPayload, ttlHours: Int = 24): Boolean {
        return kotlinx.coroutines.withContext(Dispatchers.IO) {
            if (token.isEmpty() || baseUrl.isEmpty()) return@withContext false
            val conn = open("/relay/send", "POST", withAuth = true)
            val body = buildJsonObject {
                put("to", toDeviceId)
                put("type", "push")
                put("ttlHours", ttlHours)
                putJsonObject("payload") {
                    put("version", payload.version)
                    put("sender", payload.sender)
                    put("senderId", payload.senderId)
                    put("date", payload.date)
                    putJsonArray("dramas") {
                        payload.dramas.forEach { d ->
                            add(buildJsonObject {
                                put("title", d.title)
                                put("isFast", d.isFast)
                            })
                        }
                    }
                    putJsonArray("records") {
                        payload.records.forEach { r ->
                            add(buildJsonObject {
                                put("title", r.title)
                                put("platform", r.platform)
                                put("isFast", r.isFast)
                                put("count", r.count)
                                put("updatedAt", r.updatedAt)
                            })
                        }
                    }
                }
            }.toString()
            try {
                conn.doOutput = true
                conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                val text = readAll(conn) ?: return@withContext false
                val obj = json.parseToJsonElement(text).jsonObject
                obj["ok"]?.jsonPrimitive?.content == "true"
            } catch (e: Exception) { false } finally {
                try { conn.disconnect() } catch (_: Exception) {}
            }
        }
    }

    /** 拉取中继在线设备（仅按 ID 过滤自己，不能按名称——同型号设备名会冲突） */
    suspend fun fetchDevices(selfId: String): List<PushDevice> {
        return kotlinx.coroutines.withContext(Dispatchers.IO) {
            if (token.isEmpty() || baseUrl.isEmpty()) return@withContext emptyList()
            val conn = open("/relay/devices", "GET", withAuth = true)
            try {
                val text = readAll(conn) ?: return@withContext emptyList()
                val obj = json.parseToJsonElement(text).jsonObject
                if (obj["ok"]?.jsonPrimitive?.content != "true") return@withContext emptyList()
                val arr = obj["devices"]?.jsonArray ?: return@withContext emptyList()
                val list = mutableListOf<PushDevice>()
                for (el in arr) {
                    val o = el.jsonObject
                    val id = o["deviceId"]?.jsonPrimitive?.content ?: continue
                    if (id == selfId) continue
                    // 只保留在线设备（服务端标记 online=false 时跳过）
                    val online = o["online"]?.jsonPrimitive?.booleanOrNull
                    if (online == false) continue
                    val name = o["deviceName"]?.jsonPrimitive?.content ?: "中继设备"
                    val platform = o["platform"]?.jsonPrimitive?.content ?: ""
                    list.add(
                        PushDevice(
                            ip = "",
                            name = if (platform.isNotEmpty()) name + " · " + platform else name,
                            deviceId = id,
                            port = 0,
                            source = "relay"
                        )
                    )
                }
                list
            } catch (e: Exception) { emptyList() } finally {
                try { conn.disconnect() } catch (_: Exception) {}
            }
        }
    }
}
