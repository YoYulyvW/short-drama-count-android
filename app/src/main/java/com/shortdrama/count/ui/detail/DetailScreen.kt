package com.shortdrama.count.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shortdrama.count.ui.components.HeroCard
import com.shortdrama.count.ui.components.SectionCard
import com.shortdrama.count.ui.theme.AppColorsHolder
import com.shortdrama.count.ui.theme.Palette
import com.shortdrama.count.ui.theme.parseHex
import com.shortdrama.count.util.AppConstants
import com.shortdrama.count.viewmodel.AppViewModel
import java.util.Date

@Composable
fun DetailScreen(vm: AppViewModel) {
    val date by vm.currentDate.collectAsState()
    val days by vm.days.collectAsState()
    val dateStr = AppConstants.dateString(date)
    val day = days[dateStr]
    val summary = vm.summary(dateStr)
    var showPicker by remember { mutableStateOf(false) }
    val c = AppColorsHolder

    LazyColumn(
        Modifier.fillMaxSize().background(c.bg),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("每日详情", color = c.text, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    Text(AppConstants.chineseDate(date), color = c.textSub, fontSize = 13.sp)
                }
                PressableChip(dateStr) { showPicker = true }
            }
        }
        item {
            HeroCard(Palette.monthGradient) {
                Column(Modifier.padding(20.dp)) {
                    Text("今日概览", color = Color.White.copy(alpha = 0.9f), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Row {
                        Column(Modifier.weight(1f)) {
                            Text("短剧 " + summary.dramaCount + " 部", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text("广告 " + summary.total + " 条", color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("涉及平台 " + summary.sums.size + " 个", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            val avg = if (summary.dramaCount > 0) summary.total.toDouble() / summary.dramaCount else 0.0
                            Text("平均 " + String.format(java.util.Locale.US, "%.1f", avg) + " 条/部",
                                color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp)
                        }
                    }
                }
            }
        }
        item {
            SectionCard(title = "平台累计次数") {
                val sorted = summary.sums.entries.sortedByDescending { it.value }
                if (sorted.isEmpty()) {
                    Text("暂无数据", color = c.textSub, fontSize = 13.sp, modifier = Modifier.fillMaxWidth())
                } else {
                    Column {
                        sorted.forEachIndexed { idx, e ->
                            val cfg = vm.platformConfig(e.key)
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 10.dp)) {
                                Box(Modifier.size(10.dp).clip(RoundedCornerShape(5.dp)).background(parseHex(cfg.colorHex)))
                                Spacer(Modifier.width(10.dp))
                                Text(e.key, color = c.text, fontSize = 14.sp)
                                Spacer(Modifier.weight(1f))
                                Text(e.value.toString() + " 次", color = Palette.indigo, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            }
                            if (idx < sorted.size - 1) Divider(color = c.divider)
                        }
                    }
                }
            }
        }
        item { Text("短剧明细", color = c.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold) }
        val dramas = day?.dramas?.reversed() ?: emptyList()
        val detailed = dramas.filter { d -> day!!.records.any { it.title == d.title && it.isFast == d.isFast } }
        if (detailed.isEmpty()) {
            item {
                Text("还没有明细", color = c.textSub, fontSize = 13.sp,
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.card).padding(30.dp))
            }
        } else {
            items(detailed, key = { it.id }) { drama ->
                val recs = day!!.records.filter { it.title == drama.title && it.isFast == drama.isFast }
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.card).padding(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(drama.title, color = c.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        if (drama.isFast) {
                            Spacer(Modifier.width(6.dp))
                            Text("极速", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold,
                                modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Palette.blue)
                                    .padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                        Spacer(Modifier.weight(1f))
                        Text(recs.sumOf { it.count }.toString() + " 条", color = Palette.indigo, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        recs.forEach { r ->
                            val cfg = vm.platformConfig(r.platform)
                            Text(cfg.short + " " + r.count, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(parseHex(cfg.colorHex))
                                    .padding(horizontal = 8.dp, vertical = 3.dp))
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(40.dp)) }
    }

    if (showPicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = date.time)
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = { TextButton(onClick = { state.selectedDateMillis?.let { vm.setDate(Date(it)) }; showPicker = false }) { Text("确定") } },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text("取消") } },
        ) { DatePicker(state = state) }
    }
}

@Composable
private fun PressableChip(text: String, onClick: () -> Unit) {
    val c = AppColorsHolder
    com.shortdrama.count.ui.components.PressableCard(onClick = onClick) {
        Row(
            Modifier.clip(RoundedCornerShape(10.dp))
                .background(if (c.isDark) Palette.blueLightDark else Palette.blueLight)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.CalendarMonth, null, tint = Palette.blue, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text(text, color = Palette.blue, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
