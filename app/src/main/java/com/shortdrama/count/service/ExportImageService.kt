package com.shortdrama.count.service

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import com.shortdrama.count.model.DayData
import com.shortdrama.count.util.AppConstants
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExportImageService {
    private val W = 390f
    private val M = 16f
    private val heroH = 180f
    private val rowH = 56f
    private val sumH = 150f
    private val footerH = 42f

    fun render(date: String, day: DayData): Bitmap? {
        val dramas = day.dramas.reversed()
        if (dramas.isEmpty()) return null

        val valid = dramas.filter { d -> day.records.any { r -> r.title == d.title && r.isFast == d.isFast } }
        val rowsTop = M + heroH + 24
        val rowsBottom = rowsTop + valid.size * (rowH + 8)
        val sumTop = rowsBottom + 16
        val H = sumTop + sumH + footerH + M
        val total = day.records.sumOf { r -> r.count }
        val sums = mutableMapOf<String, Int>()
        day.records.forEach { r -> sums[r.platform] = (sums[r.platform] ?: 0) + r.count }
        val dramaCount = valid.size
        val dataDate: Date = try {
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(date) ?: Date()
        } catch (e: Exception) { Date() }
        val dataDateText = AppConstants.chineseDate(dataDate)

        val bmp = Bitmap.createBitmap(W.toInt(), H.toInt(), Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.rgb(245, 245, 250))

        val heroRect = RectF(M, M, W - M, M + heroH)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.shader = LinearGradient(0f, M, W, M + heroH,
            Color.rgb(166, 139, 250), Color.rgb(95, 168, 245), Shader.TileMode.CLAMP)
        c.drawRoundRect(heroRect, 22f, 22f, paint)

        val white = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = 15f }
        val big = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = 56f; isFakeBoldText = true }
        val sub = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(217, 255, 255, 255); textSize = 13f }

        c.drawText(dataDateText, M + 20, M + 30, sub)
        c.drawText(dramaCount.toString(), M + 20, M + 90, big)
        c.drawText("部短剧", M + 20, M + 110, white)
        c.drawText("涉及平台 " + sums.size + " 个", W - M - 130, M + 40, sub)

        val pillW = (W - M * 2 - 60) / 2
        val pillY = M + heroH - 60
        val pill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(64, 255, 255, 255) }
        c.drawRoundRect(RectF(M + 20, pillY, M + 20 + pillW, pillY + 44), 12f, 12f, pill)
        c.drawRoundRect(RectF(M + 20 + pillW + 16, pillY, M + 20 + pillW * 2 + 16, pillY + 44), 12f, 12f, pill)

        val pillTitle = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(217, 255, 255, 255); textSize = 11f }
        val pillValue = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = 18f; isFakeBoldText = true }
        val avg = if (dramaCount > 0) total.toDouble() / dramaCount else 0.0
        c.drawText("广告", M + 32, pillY + 18, pillTitle)
        c.drawText(total.toString() + " 条", M + 32, pillY + 38, pillValue)
        c.drawText("平均每剧", M + 32 + pillW + 16, pillY + 18, pillTitle)
        c.drawText(String.format(Locale.US, "%.1f 条", avg), M + 32 + pillW + 16, pillY + 38, pillValue)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(26, 26, 26); textSize = 15f; isFakeBoldText = true }
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(90, 90, 90); textSize = 13f }
        val cntPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(92, 166, 245); textSize = 20f; isFakeBoldText = true }
        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }

        var y = rowsTop
        for (drama in valid) {
            val recs = day.records.filter { r -> r.title == drama.title && r.isFast == drama.isFast }
            c.drawRoundRect(RectF(M, y, W - M, y + rowH), 16f, 16f, cardPaint)
            val disp = if (drama.isFast) drama.title + AppConstants.fastSuffix else drama.title
            c.drawText("【" + disp + "】", M + 18, y + 24, titlePaint)
            val parts = recs.joinToString("  ") { r -> r.platform.take(1) + ":" + r.count }
            c.drawText(parts, M + 18, y + 44, subPaint)
            val tot = recs.sumOf { r -> r.count }
            c.drawText(tot.toString(), W - M - 48, y + 38, cntPaint)
            y += rowH + 8
        }

        c.drawRoundRect(RectF(M, sumTop, W - M, sumTop + sumH), 18f, 18f, cardPaint)
        val sumTitle = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(26, 26, 26); textSize = 16f; isFakeBoldText = true }
        val sumValue = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(90, 90, 90); textSize = 13f }
        c.drawText("平台汇总", M + 18, sumTop + 28, sumTitle)
        val sorted = sums.entries.sortedByDescending { it.value }
        for ((idx, e) in sorted.withIndex()) {
            if (idx >= 4) break
            val col = idx % 2; val row = idx / 2
            val x = M + 18 + col * ((W - M * 2) / 2)
            val py = sumTop + 60 + row * 26
            c.drawText(e.key.take(1) + "：" + e.value + " 次", x, py, sumValue)
        }
        c.drawText("合计 " + total + " 条", M + 18, sumTop + sumH - 16, sumTitle)

        val footer = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(153, 153, 153); textSize = 11f }
        c.drawText("导出时间 " + AppConstants.shortTime(), M, sumTop + sumH + 24, footer)

        return bmp
    }
}
