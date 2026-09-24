package com.shortdrama.count.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

data class AppColors(
    val bg: androidx.compose.ui.graphics.Color,
    val card: androidx.compose.ui.graphics.Color,
    val cardElev: androidx.compose.ui.graphics.Color,
    val divider: androidx.compose.ui.graphics.Color,
    val text: androidx.compose.ui.graphics.Color,
    val textSub: androidx.compose.ui.graphics.Color,
    val textTertiary: androidx.compose.ui.graphics.Color,
    val isDark: Boolean,
)

val LocalAppColors = staticCompositionLocalOf { Palette.let { AppColors(it.bg, it.card, it.cardElev, it.divider, it.text, it.textSub, it.textTertiary, false) } }

val AppColorsHolder: AppColors
    @Composable @ReadOnlyComposable get() = LocalAppColors.current

private val LightScheme = lightColorScheme(
    primary = Palette.blue,
    secondary = Palette.indigo,
    background = Palette.bg,
    surface = Palette.card,
    error = Palette.red,
)

private val DarkScheme = darkColorScheme(
    primary = Palette.blueDark,
    secondary = Palette.indigo,
    background = Palette.bgDark,
    surface = Palette.cardDark,
    error = Palette.redDark,
)

@Composable
fun ShortDramaCountTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val scheme = if (darkTheme) DarkScheme else LightScheme
    val colors = if (darkTheme) AppColors(
        bg = Palette.bgDark, card = Palette.cardDark, cardElev = Palette.cardElevDark,
        divider = Palette.dividerDark, text = Palette.textDark, textSub = Palette.textSubDark,
        textTertiary = Palette.textTertiary, isDark = true,
    ) else AppColors(
        bg = Palette.bg, card = Palette.card, cardElev = Palette.cardElev,
        divider = Palette.divider, text = Palette.text, textSub = Palette.textSub,
        textTertiary = Palette.textTertiary, isDark = false,
    )
    CompositionLocalProvider(LocalAppColors provides colors) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
