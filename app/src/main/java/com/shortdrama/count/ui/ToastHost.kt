package com.shortdrama.count.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shortdrama.count.viewmodel.AppViewModel
import com.shortdrama.count.viewmodel.ToastStyle

@Composable
fun ToastHost(vm: AppViewModel) {
    val toast by vm.toast.collectAsState()
    Box(Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = toast != null,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 40.dp),
        ) {
            val data = toast ?: return@AnimatedVisibility
            val bg = when (data.style) {
                ToastStyle.INFO -> Color(0xD1000000)
                ToastStyle.SUCCESS -> Color(0xFF34C759)
                ToastStyle.ERROR -> Color(0xFFFF3B30)
            }
            val icon = when (data.style) {
                ToastStyle.INFO -> Icons.Filled.Info
                ToastStyle.SUCCESS -> Icons.Filled.CheckCircle
                ToastStyle.ERROR -> Icons.Filled.Warning
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .shadow(8.dp, RoundedCornerShape(20.dp))
                    .background(bg, RoundedCornerShape(20.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            ) {
                Icon(icon, contentDescription = null, tint = Color.White)
                Text(data.message, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}
