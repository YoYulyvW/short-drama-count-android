package com.shortdrama.count.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
    val scale by animateFloatAsState(if (pressed) 0.95f else 1f, label = "pressScale")
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

@Composable
fun AnimatedNumber(value: Int, color: Color = AppColorsHolder.text, size: Int = 16) {
    val animated by animateIntAsState(targetValue = value, animationSpec = spring(dampingRatio = 0.6f), label = "num")
    Text("$animated", color = color, fontSize = size.sp, fontWeight = FontWeight.SemiBold)
}

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
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.linearGradient(gradient))
            .then(if (colors.isDark) Modifier.background(Color.Black.copy(alpha = 0.15f)) else Modifier)
            .shadow(if (colors.isDark) 0.dp else 8.dp, RoundedCornerShape(22.dp))
    ) { content() }
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

@Composable
fun ColorDot(hex: String, size: Int = 10) {
    Box(Modifier.size(size.dp).clip(CircleShape).background(parseHex(hex)))
}
