package com.shortdrama.count.util

import com.shortdrama.count.model.PlatformConfig
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object AppConstants {
    const val fastSuffix = " - 极速"
    const val sharePrefix = "CHEN:"
    const val encPrefix = "DCT1:"
    const val shareSecret = "aCB3xKH9mQ7EpL2NwE5nR8tYoYu-short-drama-count-2026-secret-key"

    const val publicRepoOwner = "YoYulyvW"
    const val publicRepoName = "short-drama-count-releases"

    const val updateCheckMinIntervalMs = 3600_000L
    const val updateCheckDelayMs = 15_000L

    private val fastSuffixRegex = Regex("\\s*[-~－\\u2010-\\u2015]\\s*极速\\s*$")

    fun dateString(date: Date): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date)

    fun nowString(): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

    fun shortTime(): String =
        SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

    fun shortTimeNoSec(): String =
        SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date())

    fun chineseDate(date: Date): String =
        SimpleDateFormat("M月d日", Locale.getDefault()).format(date)

    fun monthTitle(date: Date): String =
        SimpleDateFormat("yyyy年M月", Locale.getDefault()).format(date)

    fun formatDateTime(millis: Long): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(millis))

    fun formatBytes(bytes: Long): String =
        String.format(Locale.US, "%.1f MB", bytes.toDouble() / 1024 / 1024)

    /** 拆分剧名尾部的"极速"标记 */
    fun splitFast(raw: String): Pair<String, Boolean> {
        val t = raw.trim()
        if (t.isEmpty()) return t to false
        val m = fastSuffixRegex.find(t)
        if (m != null) {
            val base = t.substring(0, m.range.first).trim()
            if (base.isNotEmpty()) return base to true
        }
        return t to false
    }

    fun normalizePlatform(name: String, platforms: List<PlatformConfig>): String {
        val n = name.trim()
        if (n.isEmpty()) return n
        if (platforms.any { it.name == n }) return n
        for (p in platforms) if (p.name.firstOrNull() == n.firstOrNull()) return p.name
        return n
    }

    fun isToday(date: Date): Boolean {
        val c1 = Calendar.getInstance().apply { time = date }
        val c2 = Calendar.getInstance()
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
            c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
    }

    fun isYesterday(date: Date): Boolean {
        val c1 = Calendar.getInstance().apply { time = date; add(Calendar.DAY_OF_YEAR, 1) }
        val c2 = Calendar.getInstance()
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
            c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
    }

    fun compareVersions(a: String, b: String): Int {
        val pa = a.split(".").mapNotNull { it.toIntOrNull() }
        val pb = b.split(".").mapNotNull { it.toIntOrNull() }
        val maxLen = maxOf(pa.size, pb.size)
        for (i in 0 until maxLen) {
            val va = pa.getOrElse(i) { 0 }
            val vb = pb.getOrElse(i) { 0 }
            if (va != vb) return if (va > vb) 1 else -1
        }
        return 0
    }

    fun hexToArgb(hex: String): Long {
        val h = hex.trim().removePrefix("#")
        return h.toLongOrNull(16) ?: 0L
    }
}
