package com.shortdrama.count.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import com.shortdrama.count.ui.theme.AppColorsHolder
import com.shortdrama.count.ui.theme.parseHex

@Composable
fun PressableCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
        label = "pressScale",
    )
    Box(
        modifier = modifier
            .scale(scale)
            .then(
                if (onClick != null) Modifier.clickable(
                    interactionSource = interaction,
                    indication = null, onClick = onClick
                ) else Modifier
            )
    ) { content() }
}

/** 带弹性缩放的数字（值变化时先放大再回弹） */
@Composable
fun BounceNumber(
    value: Int,
    color: Color,
    size: Int = 16,
    fontWeight: FontWeight = FontWeight.SemiBold,
) {
    val scale = remember { Animatable(1f) }
    LaunchedEffect(value) {
        scale.snapTo(1.35f)
        scale.animateTo(1f, animationSpec = spring(dampingRatio = 0.42f, stiffness = Spring.StiffnessMedium))
    }
    Text(
        value.toString(),
        color = color,
        fontSize = size.sp,
        fontWeight = fontWeight,
        modifier = Modifier.scale(scale.value),
    )
}

@Composable
fun AnimatedNumber(value: Int, color: Color = AppColorsHolder.text, size: Int = 16) {
    BounceNumber(value, color, size)
}

/**
 * 渐变 Hero 卡片。
 * 关键：shadow 必须在 clip 之前，否则阴影会被裁剪成深色边圈。
 */
@Composable
fun HeroCard(
    gradient: List<Color>,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val colors = AppColorsHolder
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (colors.isDark) 0.dp else 6.dp,
                shape = RoundedCornerShape(22.dp),
                clip = false,
                ambientColor = Color(0x33000000),
                spotColor = Color(0x33000000),
            )
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.linearGradient(gradient)),
    ) {
        if (colors.isDark) {
            Box(Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.12f)))
        }
        content()
    }
}

@Composable
fun SectionCard(title: String? = null, content: @Composable () -> Unit) {
    val c = AppColorsHolder
    Column(Modifier.fillMaxWidth()) {
        if (title != null) {
            Text(title, color = c.textSub, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp))
        }
        Box(
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(c.card)
                .padding(14.dp)
        ) { content() }
    }
}

@Composable
fun EmptyStateView(icon: String, title: String, subtitle: String, modifier: Modifier = Modifier) {
    val c = AppColorsHolder
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(icon, fontSize = 54.sp)
        Text(title, color = c.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        Text(subtitle, color = c.textSub, fontSize = 13.sp, textAlign = TextAlign.Center)
    }
}

@Composable
fun StatPill(title: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.22f))
            .padding(vertical = 10.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp)
        Text(value, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}



/** 平台排行条 */
@Composable
fun RankBarRow(
    name: String,
    short: String,
    colorHex: String,
    count: Int,
    maxCount: Int,
    total: Int,
) {
    val color = parseHex(colorHex)
    val ratio = if (maxCount <= 0) 0f else count.toFloat() / maxCount
    val percent = if (total <= 0) 0 else ((count.toDouble() / total) * 100).roundToInt()
    Row(
        Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PlatformIcon(name = name, colorHex = colorHex, size = 34.dp)
        Column(Modifier.weight(1f)) {
            Text(name, color = AppColorsHolder.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Box(
                Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
                    .background(AppColorsHolder.cardElev)
            ) {
                Box(
                    Modifier.fillMaxHeight().fillMaxWidth(ratio.coerceIn(0f, 1f))
                        .clip(RoundedCornerShape(3.dp)).background(color)
                )
            }
        }
        Text(count.toString(), color = AppColorsHolder.text, fontSize = 17.sp,
            fontWeight = FontWeight.Black, modifier = Modifier.width(38.dp),
            textAlign = TextAlign.End)
        Text(percent.toString() + "%", color = AppColorsHolder.textSub, fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold, modifier = Modifier.width(34.dp),
            textAlign = TextAlign.End)
    }
}

data class RankItem(val name: String, val short: String, val colorHex: String, val count: Int)

@Composable
fun RankBarListImpl(items: List<RankItem>) {
    val maxCount = items.maxOfOrNull { it.count } ?: 1
    val total = items.sumOf { it.count }
    Column {
        items.forEachIndexed { idx, it ->
            RankBarRow(it.name, it.short, it.colorHex, it.count, maxCount, total)
            if (idx < items.size - 1) {
                Box(Modifier.fillMaxWidth().height(1.dp)
                    .background(if (AppColorsHolder.isDark) Color(0x33FFFFFF) else Color(0x22000000)))
            }
        }
    }
}

/** 4 格 KPI */
data class KpiItem(val value: String, val label: String, val color: Color)

@Composable
fun KPIRow(items: List<KpiItem>) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { it ->
            Column(
                Modifier.weight(1f).clip(RoundedCornerShape(14.dp))
                    .background(AppColorsHolder.card).padding(vertical = 12.dp, horizontal = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(it.value, color = it.color, fontSize = 19.sp, fontWeight = FontWeight.Black,
                    maxLines = 1)
                Spacer(Modifier.height(3.dp))
                Text(it.label, color = AppColorsHolder.textSub, fontSize = 10.5.sp)
            }
        }
    }
}

/** Hero 内三宫格 pills */
@Composable
fun HeroPills(items: List<Pair<String, String>>) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items.forEach { (k, v) ->
            Column(
                Modifier.weight(1f).clip(RoundedCornerShape(13.dp))
                    .background(Color.White.copy(alpha = 0.20f))
                    .padding(vertical = 10.dp, horizontal = 12.dp),
            ) {
                Text(k, color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp)
                Spacer(Modifier.height(2.dp))
                Text(v, color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Black, maxLines = 1)
            }
        }
    }
}

@Composable
fun ColorDot(hex: String, size: Int = 10) {
    Box(Modifier.size(size.dp).clip(CircleShape).background(parseHex(hex)))
}
