package com.shortdrama.count.ui.theme

import androidx.compose.ui.graphics.Color

fun parseHex(hex: String): Color {
    val h = hex.trim().removePrefix("#")
    return try {
        when (h.length) {
            6 -> Color(0xFF000000 or h.toLong(16))
            8 -> Color(h.toLong(16))
            else -> Color.Gray
        }
    } catch (_: Exception) { Color.Gray }
}

object Palette {
    val bg = Color(0xFFF2F2F7)
    val bgDark = Color(0xFF000000)
    val card = Color(0xFFFFFFFF)
    val cardDark = Color(0xFF1C1C1E)
    val cardElev = Color(0xFFF9F9FB)
    val cardElevDark = Color(0xFF2C2C2E)
    val divider = Color(0x33000000)
    val dividerDark = Color(0x33FFFFFF)

    val text = Color(0xFF000000)
    val textDark = Color(0xFFFFFFFF)
    val textSub = Color(0xFF3C3C43)
    val textSubDark = Color(0xFFEBEBF5)
    val textTertiary = Color(0xFF8E8E93)

    val blue = Color(0xFF007AFF)
    val blueDark = Color(0xFF0A84FF)
    val indigo = Color(0xFF5E5CE6)
    val purple = Color(0xFFAF52DE)
    val purpleDark = Color(0xFFBF5AF2)
    val green = Color(0xFF34C759)
    val greenDark = Color(0xFF30D158)
    val orange = Color(0xFFFF9500)
    val orangeDark = Color(0xFFFF9F0A)
    val red = Color(0xFFFF3B30)
    val redDark = Color(0xFFFF453A)

    val blueLight = Color(0xFFE5F0FF)
    val blueLightDark = Color(0xFF152A40)
    val greenLight = Color(0xFFD1F5DB)
    val greenLightDark = Color(0xFF15301D)
    val orangeLight = Color(0xFFFFE8CC)
    val orangeLightDark = Color(0xFF3A2A14)
    val redLight = Color(0xFFFFE5E5)
    val redLightDark = Color(0xFF3A1A1A)
    val grayBtn = Color(0xFFE5E5EA)
    val grayBtnDark = Color(0xFF2C2C2E)

    val heroGradient = listOf(Color(0xFFA78BFA), Color(0xFF7C7CF0), Color(0xFF5FA8F5))
    val monthGradient = listOf(Color(0xFF7C83E8), Color(0xFF9B6BDB))
    val buttonGradient = listOf(Color(0xFF60A5FA), Color(0xFF6366F1))
}
