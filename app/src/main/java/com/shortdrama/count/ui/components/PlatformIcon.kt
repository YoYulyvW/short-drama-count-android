package com.shortdrama.count.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shortdrama.count.R
import com.shortdrama.count.ui.theme.parseHex

/**
 * 平台图标：优先用内置 Logo 资源，未知平台回退为品牌色方块 + 首字。
 */
@Composable
fun PlatformIcon(
    name: String,
    colorHex: String,
    size: Dp = 20.dp,
    modifier: Modifier = Modifier,
) {
    val resId = assetRes(name)
    if (resId != null) {
        Image(
            painter = painterResource(resId),
            contentDescription = name,
            contentScale = ContentScale.Fit,
            modifier = modifier.size(size).clip(RoundedCornerShape(size * 0.28f)),
        )
    } else {
        Box(
            modifier = modifier
                .size(size)
                .clip(RoundedCornerShape(size * 0.28f))
                .background(parseHex(colorHex)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                name.take(1),
                color = Color.White,
                fontSize = (size.value * 0.5f).sp,
                fontWeight = FontWeight.Black,
            )
        }
    }
}

private fun assetRes(name: String): Int? = when (name) {
    "橙子建站" -> R.mipmap.platform_orange
    "懂车帝" -> R.mipmap.platform_dongchedi
    "易车" -> R.mipmap.platform_yiche
    "车主之家", "汽车之家" -> R.mipmap.platform_chezhu
    else -> null
}
