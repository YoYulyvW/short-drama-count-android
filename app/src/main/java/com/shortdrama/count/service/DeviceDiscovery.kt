package com.shortdrama.count.service

import android.content.Context
import android.net.wifi.WifiManager
import android.os.Build
import com.shortdrama.count.App
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.net.HttpURLConnection
import java.net.URL

object DeviceDiscovery {
    private const val deviceIdKey = "kai_fan_le_device_uuid"

    fun selfDeviceId(): String {
        val sp = App.instance.getSharedPreferences("drama_prefs", Context.MODE_PRIVATE)
        val existing = sp.getString(deviceIdKey, null)
        if (!existing.isNullOrEmpty()) return existing
        val uuid = java.util.UUID.randomUUID().toString()
        sp.edit().putString(deviceIdKey, uuid).apply()
        return uuid
    }

    fun selfDisplayName(): String =
        Build.MODEL ?: "Android 设备"

    fun localIpv4(): String? {
        try {
            val wifi = App.instance.applicationContext
                .getSystemService(Context.WIFI_SERVICE) as WifiManager
            @Suppress("DEPRECATION")
            val ip = wifi.connectionInfo.ipAddress
            if (ip == 0) return null
            return String.format("%d.%d.%d.%d",
                ip and 0xff, (ip shr 8) and 0xff,
                (ip shr 16) and 0xff, (ip shr 24) and 0xff)
        } catch (e: Exception) { return null }
    }

    suspend fun scan(port: Int, timeoutMs: Int = 500): List<com.shortdrama.count.model.PushDevice> =
        withContext(Dispatchers.IO) {
            val localIp = localIpv4() ?: return@withContext emptyList()
            val parts = localIp.split(".")
            if (parts.size != 4) return@withContext emptyList()
            val prefix = parts[0] + "." + parts[1] + "." + parts[2] + "."
            val selfId = selfDeviceId()
            val results = mutableListOf<com.shortdrama.count.model.PushDevice>()
            val jobs = (1..254).map { i ->
                val ip = prefix + i
                if (ip == localIp) null
                else Thread {
                    val d = pingHost(ip, port, selfId, timeoutMs)
                    if (d != null) synchronized(results) { results.add(d) }
                }
            }
            val threads = jobs.filterNotNull()
            threads.forEach { it.start() }
            val deadline = System.currentTimeMillis() + timeoutMs + 2000
            for (t in threads) {
                val remain = deadline - System.currentTimeMillis()
                if (remain > 0) t.join(remain)
            }
            results.sortedBy { dev ->
                dev.ip.split(".").mapNotNull { p -> p.toIntOrNull() }
                    .joinToString(".") { n -> "%03d".format(n) }
            }
        }

    private fun pingHost(ip: String, port: Int, selfId: String, timeoutMs: Int): com.shortdrama.count.model.PushDevice? {
        return try {
            val url = URL("http://" + ip + ":" + port + "/ping")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = timeoutMs
            conn.readTimeout = timeoutMs
            conn.requestMethod = "GET"
            if (conn.responseCode != 200) return null
            val text = conn.inputStream.bufferedReader().use { it.readText() }
            val obj = Json.parseToJsonElement(text).jsonObject
            val ok = obj["ok"]?.jsonPrimitive?.content == "true"
            if (!ok) return null
            val deviceId = obj["deviceId"]?.jsonPrimitive?.content ?: ""
            if (deviceId.isNotEmpty() && deviceId == selfId) return null
            val name = obj["device"]?.jsonPrimitive?.content ?: "开饭了"
            com.shortdrama.count.model.PushDevice(ip = ip, name = name, deviceId = deviceId, port = port)
        } catch (e: Exception) { null }
    }

    suspend fun push(payload: com.shortdrama.count.model.PushPayload, device: com.shortdrama.count.model.PushDevice, timeoutMs: Int = 8000): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val url = URL("http://" + device.ip + ":" + device.port + "/push")
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = timeoutMs
                conn.readTimeout = timeoutMs
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                val body = Json.encodeToString(com.shortdrama.count.model.PushPayload.serializer(), payload)
                conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                if (conn.responseCode != 200) return@withContext false
                val text = conn.inputStream.bufferedReader().use { it.readText() }
                val obj = Json.parseToJsonElement(text).jsonObject
                obj["ok"]?.jsonPrimitive?.content == "true"
            } catch (e: Exception) { false }
        }
}
