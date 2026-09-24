package com.shortdrama.count.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shortdrama.count.ui.theme.AppColorsHolder

@Composable
fun BarChartView(values: List<Triple<String, Int, Color>>, modifier: Modifier = Modifier) {
    val c = AppColorsHolder
    val maxV = (values.maxOfOrNull { it.second } ?: 1).coerceAtLeast(1)
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        values.forEach { (label, value, color) ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f),
            ) {
                Text("$value", color = c.text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Box(
                    Modifier.height(120.dp).width(28.dp),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    Box(
                        Modifier.fillMaxSize().clip(RoundedCornerShape(6.dp)).background(c.cardElev)
                    )
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(((value.toFloat() / maxV) * 120f).coerceAtLeast(6f).dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Brush.verticalGradient(listOf(color.copy(alpha = 0.7f), color)))
                    )
                }
                Text(label, color = c.textSub, fontSize = 11.sp, maxLines = 1)
            }
        }
    }
}

@Composable
fun LineChartView(points: List<Int>, modifier: Modifier = Modifier) {
    val c = AppColorsHolder
    val maxV = (points.maxOrNull() ?: 1).coerceAtLeast(1)
    Canvas(modifier.fillMaxWidth().height(140.dp)) {
        val w = size.width
        val h = size.height
        val n = (points.size - 1).coerceAtLeast(1)
        val step = w / n
        // grid
        for (i in 0..4) {
            val y = h * i / 4
            drawLine(c.divider, Offset(0f, y), Offset(w, y), strokeWidth = 1f)
        }
        if (points.size >= 2) {
            val fillPath = Path().apply {
                moveTo(0f, h)
                points.forEachIndexed { i, v ->
                    val x = i * step
                    val y = h - (v.toFloat() / maxV) * (h - 20f) - 6f
                    lineTo(x, y)
                }
                lineTo((points.size - 1) * step, h)
                close()
            }
            drawPath(fillPath, Brush.verticalGradient(
                listOf(
                    com.shortdrama.count.ui.theme.Palette.blue.copy(alpha = 0.25f),
                    com.shortdrama.count.ui.theme.Palette.blue.copy(alpha = 0.02f),
                )
            ))
            val linePath = Path()
            points.forEachIndexed { i, v ->
                val x = i * step
                val y = h - (v.toFloat() / maxV) * (h - 20f) - 6f
                if (i == 0) linePath.moveTo(x, y) else linePath.lineTo(x, y)
            }
            drawPath(linePath, com.shortdrama.count.ui.theme.Palette.blue,
                style = Stroke(width = 5f, cap = StrokeCap.Round))
        }
    }
}
