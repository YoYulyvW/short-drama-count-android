package com.shortdrama.count.service

import com.shortdrama.count.model.PushPayload
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.BufferedOutputStream
import java.io.InputStream
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 极简 HTTP 服务器，仅服务于局域网握手/推送：
 *   GET  /ping   -> {"ok":true,"deviceId":"...","device":"...","port":N}
 *   POST /push   -> 接收 PushPayload,返回 {"ok":true}
 *   GET  /       -> 简单提示页
 */
object LanServer {
    private val preferredPorts = listOf(8080, 8081, 8181, 8888, 9988)

    private var serverSocket: ServerSocket? = null
    private var acceptJob: Job? = null
    private var scope: CoroutineScope? = null

    @Volatile var running: Boolean = false
        private set
    @Volatile var port: Int = 0
        private set

    private val _pushEvents = MutableSharedFlow<PushPayload>(extraBufferCapacity = 8)
    val pushEvents: SharedFlow<PushPayload> = _pushEvents

    fun start() {
        if (running) return
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        acceptJob = scope?.launch { listenLoop() }
    }

    private fun listenLoop() {
        for (tryPort in preferredPorts) {
            try {
                val ss = ServerSocket(tryPort)
                serverSocket = ss
                port = tryPort
                running = true
                while (running) {
                    val sock = try { ss.accept() } catch (e: Exception) { break }
                    scope?.launch { handleConnection(sock) }
                }
                break
            } catch (e: Exception) {
                continue
            }
        }
    }

    fun stop() {
        running = false
        try { serverSocket?.close() } catch (_: Exception) {}
        serverSocket = null
        acceptJob?.cancel()
        scope?.cancel()
        scope = null
        port = 0
    }

    private fun handleConnection(sock: Socket) {
        try {
            sock.soTimeout = 8000
            val input = sock.getInputStream()
            val output = BufferedOutputStream(sock.getOutputStream())
            val header = readHeader(input) ?: run { sock.close(); return }
            val (method, path, contentLen) = header
            val bodyBytes = if (method == "POST" && contentLen > 0) {
                val buf = ByteArray(contentLen)
                var read = 0
                while (read < contentLen) {
                    val n = input.read(buf, read, contentLen - read)
                    if (n <= 0) break
                    read += n
                }
                buf
            } else ByteArray(0)

            val cleanPath = path.substringBefore("?").trimEnd('/')
            when {
                method == "GET" && cleanPath == "/ping" -> {
                    val json = buildPingJson()
                    writeResponse(output, 200, "application/json", json.toByteArray(Charsets.UTF_8))
                }
                method == "POST" && cleanPath == "/push" -> {
                    try {
                        val payload = Json { ignoreUnknownKeys = true }
                            .decodeFromString(PushPayload.serializer(), String(bodyBytes, Charsets.UTF_8))
                        _pushEvents.tryEmit(payload)
                        writeResponse(output, 200, "application/json", "{\"ok\":true}".toByteArray())
                    } catch (e: Exception) {
                        writeResponse(output, 200, "application/json", "{\"ok\":false}".toByteArray())
                    }
                }
                method == "GET" && (cleanPath == "" || cleanPath == "/") -> {
                    val html = "<html><body><h3>开饭了 · 局域网服务</h3><p>设备：" +
                        DeviceDiscovery.selfDisplayName() + "</p></body></html>"
                    writeResponse(output, 200, "text/html; charset=utf-8", html.toByteArray(Charsets.UTF_8))
                }
                else -> writeResponse(output, 404, "text/plain", "Not Found".toByteArray())
            }
            output.flush()
            sock.close()
        } catch (e: Exception) {
            try { sock.close() } catch (_: Exception) {}
        }
    }

    private data class HeaderLine(val method: String, val path: String, val contentLen: Int)

    private fun readHeader(input: InputStream): HeaderLine? {
        val sb = StringBuilder()
        val buf = ByteArray(1)
        while (true) {
            val n = input.read(buf)
            if (n <= 0) return null
            sb.append(buf[0].toInt().toChar())
            if (sb.endsWith("\r\n\r\n")) break
            if (sb.length > 16_384) return null
        }
        val lines = sb.toString().split("\r\n").filter { it.isNotEmpty() }
        if (lines.isEmpty()) return null
        val first = lines[0].split(" ")
        if (first.size < 2) return null
        val method = first[0]
        val path = first[1]
        var contentLen = 0
        for (l in lines.drop(1)) {
            val idx = l.indexOf(":")
            if (idx <= 0) continue
            val name = l.substring(0, idx).trim().lowercase()
            val v = l.substring(idx + 1).trim()
            if (name == "content-length") contentLen = v.toIntOrNull() ?: 0
        }
        return HeaderLine(method, path, contentLen)
    }

    private fun writeResponse(out: BufferedOutputStream, code: Int, contentType: String, body: ByteArray) {
        val header = "HTTP/1.1 " + code + " " + statusText(code) + "\r\n" +
            "Content-Type: " + contentType + "\r\n" +
            "Content-Length: " + body.size + "\r\n" +
            "Connection: close\r\n\r\n"
        out.write(header.toByteArray(Charsets.UTF_8))
        out.write(body)
    }

    private fun statusText(code: Int) = when (code) {
        200 -> "OK"; 404 -> "Not Found"; else -> "OK"
    }

    private fun buildPingJson(): String {
        val deviceId = DeviceDiscovery.selfDeviceId()
        val device = DeviceDiscovery.selfDisplayName().replace("\"", "")
        return "{\"ok\":true,\"deviceId\":\"" + deviceId + "\",\"device\":\"" + device + "\",\"port\":" + port + "}"
    }
}
