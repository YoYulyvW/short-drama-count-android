package com.shortdrama.count.service

import com.shortdrama.count.App
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

object UpdateDownloader {
    private val _progress = MutableStateFlow(0.0)
    val progress: StateFlow<Double> = _progress

    private val _isDownloading = MutableStateFlow(false)
    val isDownloading: StateFlow<Boolean> = _isDownloading

    private val _currentVersion = MutableStateFlow("")
    val currentVersion: StateFlow<String> = _currentVersion

    private val updatesDir: File
        get() = File(App.instance.filesDir, "Updates").apply { if (!exists()) mkdirs() }

    fun targetApkPath(): File = File(updatesDir, "ShortDramaCount.apk")

    fun existingApkPath(): File? = targetApkPath().takeIf { it.exists() }

    fun deleteExistingApk() {
        try { targetApkPath().delete() } catch (_: Exception) {}
    }

    suspend fun startDownload(url: String, version: String, proxy: String = ""): Result<File> =
        withContext(Dispatchers.IO) {
            if (_isDownloading.value) return@withContext Result.failure(Exception("已有下载任务"))
            _isDownloading.value = true
            _progress.value = 0.0
            _currentVersion.value = version
            deleteExistingApk()

            // 依次尝试：代理URL → 直连
            val candidates = mutableListOf<String>()
            val np = UpdateChecker.normalizeProxy(proxy)
            if (np.isNotEmpty()) candidates.add(np + url)
            if (!candidates.contains(url)) candidates.add(url)

            var lastErr: Exception? = null
            for (u in candidates) {
                try {
                    val file = download(u)
                    _isDownloading.value = false
                    return@withContext Result.success(file)
                } catch (e: Exception) {
                    lastErr = e
                    _progress.value = 0.0
                }
            }
            _isDownloading.value = false
            Result.failure(lastErr ?: Exception("下载失败"))
        }

    private fun download(urlStr: String): File {
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            connectTimeout = 30000
            readTimeout = 60000
            requestMethod = "GET"
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 13)")
            setRequestProperty("Accept", "application/octet-stream")
        }
        val code = conn.responseCode
        if (code !in 200..299) throw Exception("HTTP $code")
        val total = conn.contentLengthLong
        val tmp = File(updatesDir, "ShortDramaCount.apk.part")
        conn.inputStream.use { input ->
            tmp.outputStream().use { out ->
                val buf = ByteArray(64 * 1024)
                var written = 0L
                while (true) {
                    val n = input.read(buf)
                    if (n <= 0) break
                    out.write(buf, 0, n)
                    written += n
                    if (total > 0) _progress.value = written.toDouble() / total
                }
            }
        }
        val dest = targetApkPath()
        if (dest.exists()) dest.delete()
        if (!tmp.renameTo(dest)) { tmp.copyTo(dest, overwrite = true); tmp.delete() }
        _progress.value = 1.0
        return dest
    }

    fun reset() {
        _isDownloading.value = false
        _progress.value = 0.0
    }
}
