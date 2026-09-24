package com.shortdrama.count.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shortdrama.count.ui.theme.AppColorsHolder
import com.shortdrama.count.util.Haptics

/** 可折叠的分类卡片：点标题栏展开/收起内容 */
@Composable
fun CollapsibleCard(
    title: String,
    subtitle: String? = null,
    defaultExpanded: Boolean = false,
    content: @Composable () -> Unit,
) {
    val c = AppColorsHolder
    var expanded by rememberSaveable(title) { mutableStateOf(defaultExpanded) }
    val arrow by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
        label = "arrow",
    )
    val interaction = remember { MutableInteractionSource() }

    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.card),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
                .clickable(interactionSource = interaction, indication = null) {
                    expanded = !expanded; Haptics.tap()
                }
                .padding(horizontal = 14.dp, vertical = 14.dp),
        ) {
            Text(title, color = c.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            androidx.compose.foundation.layout.Spacer(Modifier.width(10.dp))
            if (subtitle != null) {
                Text(subtitle, color = c.textSub, fontSize = 12.sp, modifier = Modifier.weight(1f))
            } else {
                Box(Modifier.weight(1f))
            }
            Icon(
                Icons.Filled.ExpandMore, null,
                tint = c.textSub,
                modifier = Modifier.size(22.dp).rotate(arrow),
            )
        }
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
            exit = shrinkVertically(spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow)) + fadeOut(),
        ) {
            Column(Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp)) {
                content()
            }
        }
    }
}

