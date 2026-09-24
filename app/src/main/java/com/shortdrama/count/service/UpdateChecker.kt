package com.shortdrama.count.service

import com.shortdrama.count.model.ReleaseInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import java.net.HttpURLConnection
import java.net.URL

object UpdateChecker {
    private const val owner = "YoYulyvW"
    private const val repo = "short-drama-count-android"

    val builtinProxies = listOf(
        "https://oo6.cc/proxy/",
        "https://lyvw.eu.org/proxy/",
    )

    fun normalizeProxy(raw: String): String {
        var s = raw.trim()
        if (s.isEmpty()) return ""
        if (!s.startsWith("http://") && !s.startsWith("https://")) s = "https://$s"
        if (!s.endsWith("/")) s += "/"
        return s
    }

    fun effectiveProxies(customProxy: String): List<String> {
        val result = mutableListOf<String>()
        val up = normalizeProxy(customProxy)
        if (up.isNotEmpty()) result.add(up)
        for (p in builtinProxies) if (!result.contains(p)) result.add(p)
        return result
    }

    suspend fun fetchLatestInfo(customProxy: String = ""): ReleaseInfo =
        withContext(Dispatchers.IO) {
            val apiURL = "https://api.github.com/repos/$owner/$repo/releases/latest"
            var lastError: Exception? = null

            // 1) 先尝试代理读取 GitHub API
            for (proxy in effectiveProxies(customProxy)) {
                try {
                    val text = httpGetText(proxy + apiURL)
                    if (text.isNotEmpty()) return@withContext parseReleaseJson(text)
                } catch (e: Exception) { lastError = e }
            }
            // 2) 直连
            try {
                val text = httpGetText(apiURL)
                if (text.isNotEmpty()) return@withContext parseReleaseJson(text)
            } catch (e: Exception) { lastError = e }

            throw lastError ?: Exception("无法获取版本信息")
        }

    suspend fun testProxy(proxy: String): String? = withContext(Dispatchers.IO) {
        try {
            val p = normalizeProxy(proxy)
            if (p.isEmpty()) return@withContext "未配置代理"
            val text = httpGetText(p + "https://api.github.com/repos/$owner/$repo/releases/latest")
            if (text.isEmpty()) "空响应" else null
        } catch (e: Exception) { e.message ?: "请求失败" }
    }

    private fun httpGetText(urlStr: String): String {
        val url = URL(urlStr)
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = 15000
        conn.readTimeout = 15000
        conn.requestMethod = "GET"
        conn.setRequestProperty("Accept", "application/vnd.github+json")
        conn.setRequestProperty("User-Agent", "ShortDramaCount-Android")
        val code = conn.responseCode
        if (code !in 200..299) return ""
        return conn.inputStream.bufferedReader().use { it.readText() }
    }

    private fun parseReleaseJson(text: String): ReleaseInfo {
        val obj = Json.parseToJsonElement(text).jsonObject
        val tag = obj["tag_name"]?.jsonPrimitive?.content ?: ""
        val version = tag.trimStart('v')
        var assetName = ""
        var downloadURL = ""
        var assetId = 0L
        val arr = obj["assets"]
        if (arr != null) {
            try {
                val a = arr.jsonArray
                if (a.isNotEmpty()) {
                    val first = a[0].jsonObject
                    assetName = first["name"]?.jsonPrimitive?.content ?: ""
                    downloadURL = first["browser_download_url"]?.jsonPrimitive?.content ?: ""
                    assetId = first["id"]?.jsonPrimitive?.long ?: 0L
                }
            } catch (_: Exception) {}
        }
        return ReleaseInfo(
            version = version,
            tagName = tag,
            assetId = assetId,
            assetName = assetName,
            apiAssetURL = downloadURL,
            browserDownloadURL = downloadURL,
            releaseNotes = obj["body"]?.jsonPrimitive?.content ?: "",
            publishedAt = obj["published_at"]?.jsonPrimitive?.content ?: "",
            htmlURL = obj["html_url"]?.jsonPrimitive?.content ?: "",
        )
    }
}
