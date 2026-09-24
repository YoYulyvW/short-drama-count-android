package com.shortdrama.count.service

import com.shortdrama.count.model.ReleaseInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import java.net.HttpURLConnection
import java.net.URL

object UpdateChecker {
    private const val owner = "YoYulyvW"
    private const val repo = "short-drama-count-releases"

    val builtinProxies = listOf(
        "https://oo6.cc/proxy/",
        "https://lyvw.eu.org/proxy/",
    )

    private val rawLatestURL = "https://raw.githubusercontent.com/" + owner + "/" + repo + "/refs/heads/main/latest.json"

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
            val proxies = effectiveProxies(customProxy)
            var lastError: Exception? = null
            // 先尝试代理读取 latest.json
            for (proxy in proxies) {
                try {
                    val url = proxy + rawLatestURL
                    val text = httpGetText(url)
                    if (text.isNotEmpty()) return@withContext parseLatestJson(text)
                } catch (e: Exception) { lastError = e }
            }
            // 兜底：GitHub API
            try {
                val apiURL = "https://api.github.com/repos/" + owner + "/" + repo + "/releases/latest"
                val text = httpGetText(apiURL)
                if (text.isNotEmpty()) return@withContext parseReleaseJson(text)
            } catch (e: Exception) { lastError = e }
            throw lastError ?: Exception("无法获取版本信息")
        }

    suspend fun testProxy(proxy: String): String? = withContext(Dispatchers.IO) {
        try {
            val p = normalizeProxy(proxy)
            if (p.isEmpty()) return@withContext "未配置代理"
            val text = httpGetText(p + rawLatestURL)
            if (text.isEmpty()) "空响应" else null
        } catch (e: Exception) { e.message ?: "请求失败" }
    }

    private fun httpGetText(urlStr: String): String {
        val url = URL(urlStr)
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = 15000
        conn.readTimeout = 15000
        conn.requestMethod = "GET"
        conn.setRequestProperty("Accept", "application/json")
        conn.setRequestProperty("User-Agent", "ShortDramaCount-Android")
        val code = conn.responseCode
        if (code !in 200..299) return ""
        return conn.inputStream.bufferedReader().use { it.readText() }
    }

    private fun parseLatestJson(text: String): ReleaseInfo {
        val obj = Json.parseToJsonElement(text).jsonObject
        return ReleaseInfo(
            version = obj["version"]?.jsonPrimitive?.content ?: "",
            tagName = obj["tag_name"]?.jsonPrimitive?.content ?: "",
            assetId = obj["asset_id"]?.jsonPrimitive?.long ?: 0L,
            assetName = obj["asset_name"]?.jsonPrimitive?.content ?: "",
            apiAssetURL = obj["download_url"]?.jsonPrimitive?.content
                ?: obj["browser_download_url"]?.jsonPrimitive?.content ?: "",
            browserDownloadURL = obj["browser_download_url"]?.jsonPrimitive?.content ?: "",
            releaseNotes = obj["release_notes"]?.jsonPrimitive?.content ?: "",
            publishedAt = obj["published_at"]?.jsonPrimitive?.content ?: "",
            htmlURL = obj["html_url"]?.jsonPrimitive?.content ?: "",
        )
    }

    private fun parseReleaseJson(text: String): ReleaseInfo {
        val obj = Json.parseToJsonElement(text).jsonObject
        val tag = obj["tag_name"]?.jsonPrimitive?.content ?: ""
        val version = tag.trimStart('v')
        val assets = obj["assets"]?.let { a ->
            runCatching { a.jsonObject }.getOrNull()
        }
        var assetName = ""
        var apiAssetURL = ""
        var assetId = 0L
        val arr = obj["assets"]
        if (arr is kotlinx.serialization.json.JsonArray && arr.isNotEmpty()) {
            val first = arr[0].jsonObject
            assetName = first["name"]?.jsonPrimitive?.content ?: ""
            apiAssetURL = first["browser_download_url"]?.jsonPrimitive?.content ?: ""
            assetId = first["id"]?.jsonPrimitive?.long ?: 0L
        }
        return ReleaseInfo(
            version = version,
            tagName = tag,
            assetId = assetId,
            assetName = assetName,
            apiAssetURL = apiAssetURL,
            browserDownloadURL = apiAssetURL,
            releaseNotes = obj["body"]?.jsonPrimitive?.content ?: "",
            publishedAt = obj["published_at"]?.jsonPrimitive?.content ?: "",
            htmlURL = obj["html_url"]?.jsonPrimitive?.content ?: "",
        )
    }
}
