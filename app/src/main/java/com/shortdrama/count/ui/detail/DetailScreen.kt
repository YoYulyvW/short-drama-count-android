package com.shortdrama.count.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
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
import com.shortdrama.count.ui.components.HeroPills
import com.shortdrama.count.ui.components.PlatformIcon
import com.shortdrama.count.ui.components.RankBarListImpl
import com.shortdrama.count.ui.components.RankItem
import com.shortdrama.count.ui.theme.AppColorsHolder
import com.shortdrama.count.ui.theme.Palette
import com.shortdrama.count.ui.theme.parseHex
import com.shortdrama.count.util.AppConstants
import com.shortdrama.count.viewmodel.AppViewModel

@Composable
fun DetailScreen(vm: AppViewModel) {
    val date by vm.currentDate.collectAsState()
    val days by vm.days.collectAsState()
    val dateStr = AppConstants.dateString(date)
    val c = AppColorsHolder
    val summary = vm.summary(dateStr)
    val day = days[dateStr] ?: com.shortdrama.count.model.DayData(dateStr)
    val perDrama = if (summary.dramaCount > 0) summary.total.toDouble() / summary.dramaCount else 0.0
    val rank = summary.sums.entries.sortedByDescending { it.value }.map { e ->
        val cfg = vm.platformConfig(e.key)
        RankItem(e.key, cfg.short, cfg.colorHex, e.value)
    }

    LazyColumn(
        Modifier.fillMaxSize().background(c.bg),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("每日详情", color = c.text, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    Text(AppConstants.chineseDate(date), color = c.textSub, fontSize = 13.sp)
                }
                Row(
                    Modifier.clip(RoundedCornerShape(10.dp)).background(Palette.blueLight)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(Icons.Filled.CalendarMonth, null, tint = Palette.blue, modifier = Modifier.size(14.dp))
                    Text(dateStr, color = Palette.blue, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        item {
            HeroCard(Palette.monthGradient) {
                Column(Modifier.padding(20.dp)) {
                    Text("今日共记录", color = Color.White.copy(alpha = 0.88f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(summary.total.toString(), color = Color.White, fontSize = 56.sp, fontWeight = FontWeight.Black)
                        Spacer(Modifier.width(6.dp))
                        Text("条广告", color = Color.White.copy(alpha = 0.92f), fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 8.dp))
                    }
                    Spacer(Modifier.height(10.dp))
                    HeroPills(listOf(
                        "短剧" to (summary.dramaCount.toString() + " 部"),
                        "平台" to (summary.sums.size.toString() + " 个"),
                        "均/部" to String.format(java.util.Locale.US, "%.1f", perDrama),
                    ))
                }
            }
        }

        item {
            Column {
                Row(Modifier.padding(horizontal = 4.dp, vertical = 2.dp)) {
                    Text("平台累计", color = c.textSub, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.weight(1f))
                    Text("共 " + summary.total + " 条", color = c.textSub.copy(alpha = 0.8f), fontSize = 11.sp)
                }
                Spacer(Modifier.height(8.dp))
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.card).padding(14.dp)) {
                    if (rank.isEmpty()) {
                        Text("暂无数据", color = c.textSub, fontSize = 13.sp,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp))
                    } else {
                        RankBarListImpl(rank)
                    }
                }
            }
        }

        item {
            val valid = day.dramas.reversed().filter { d ->
                day.records.any { it.title == d.title && it.isFast == d.isFast }
            }
            Column {
                Row(Modifier.padding(horizontal = 4.dp, vertical = 2.dp)) {
                    Text("剧集明细", color = c.textSub, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.weight(1f))
                    Text(valid.size.toString() + " 部", color = c.textSub.copy(alpha = 0.8f), fontSize = 11.sp)
                }
                Spacer(Modifier.height(8.dp))
                if (valid.isEmpty()) {
                    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.card).padding(30.dp),
                        contentAlignment = Alignment.Center) {
                        Text("还没有明细", color = c.textSub, fontSize = 13.sp)
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        valid.forEach { drama ->
                            val recs = day.records.filter { it.title == drama.title && it.isFast == drama.isFast }
                            val total = recs.sumOf { it.count }
                            Column(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                                    .background(c.card).padding(14.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(drama.title, color = c.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.weight(1f))
                                    if (drama.isFast) {
                                        Text("极速", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                                            modifier = Modifier.clip(RoundedCornerShape(5.dp)).background(Palette.orange)
                                                .padding(horizontal = 6.dp, vertical = 2.dp))
                                        Spacer(Modifier.width(8.dp))
                                    }
                                    Text(total.toString(), color = Palette.indigo, fontSize = 17.sp, fontWeight = FontWeight.Black)
                                }
                                Spacer(Modifier.height(10.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    recs.forEach { r ->
                                        val cfg = vm.platformConfig(r.platform)
                                        Row(
                                            Modifier.clip(RoundedCornerShape(8.dp)).background(c.cardElev)
                                                .padding(horizontal = 9.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                                        ) {
                                            PlatformIcon(name = cfg.name, colorHex = cfg.colorHex, size = 16.dp)
                                            Text(cfg.name, color = c.textSub, fontSize = 12.sp)
                                            Text(r.count.toString(), color = c.text, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(70.dp)) }
    }
}
