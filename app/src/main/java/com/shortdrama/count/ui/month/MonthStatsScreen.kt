package com.shortdrama.count.ui.month

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shortdrama.count.ui.components.HeroCard
import com.shortdrama.count.ui.components.LineChartView
import com.shortdrama.count.ui.components.SectionCard
import com.shortdrama.count.ui.components.StatPill
import com.shortdrama.count.ui.theme.AppColorsHolder
import com.shortdrama.count.ui.theme.Palette
import com.shortdrama.count.util.AppConstants
import com.shortdrama.count.viewmodel.AppViewModel
import java.util.Calendar
import java.util.Date

@Composable
fun MonthStatsScreen(vm: AppViewModel) {
    var month by remember { mutableStateOf(Date()) }
    val days by vm.days.collectAsState()
    val c = AppColorsHolder

    val cal = Calendar.getInstance()
    fun monthRange(): Pair<Date, Date> {
        val cc = Calendar.getInstance().apply { time = month; set(Calendar.DAY_OF_MONTH, 1) }
        val start = cc.time
        cc.add(Calendar.MONTH, 1)
        return start to cc.time
    }
    val (start, end) = monthRange()
    val dayList = mutableListOf<Date>()
    run {
        val cc = Calendar.getInstance().apply { time = start }
        while (cc.time.before(end)) { dayList.add(cc.time); cc.add(Calendar.DAY_OF_MONTH, 1) }
    }
    var totalDramas = 0; var totalAds = 0
    val points = mutableListOf<Int>()
    val countsByDay = mutableMapOf<String, Int>()
    for (d in dayList) {
        val ds = AppConstants.dateString(d)
        val day = days[ds]
        val ads = day?.records?.sumOf { it.count } ?: 0
        totalAds += ads
        totalDramas += day?.dramas?.size ?: 0
        points.add(ads)
        countsByDay[ds] = ads
    }
    val avg = if (dayList.isNotEmpty()) totalAds.toDouble() / dayList.size else 0.0
    val isCurrentMonth = run {
        val c1 = Calendar.getInstance().apply { time = month }
        val c2 = Calendar.getInstance()
        c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) && c1.get(Calendar.MONTH) == c2.get(Calendar.MONTH)
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(1),
        modifier = Modifier.fillMaxSize().background(c.bg),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("月统计", color = c.text, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { month = shiftMonth(month, -1) }) {
                    Icon(Icons.Filled.ChevronLeft, null, tint = Palette.blue)
                }
                Text(AppConstants.monthTitle(month), color = c.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                IconButton(onClick = { if (!isCurrentMonth) month = shiftMonth(month, 1) }, enabled = !isCurrentMonth) {
                    Icon(Icons.Filled.ChevronRight, null, tint = if (isCurrentMonth) c.textSub.copy(alpha = 0.4f) else Palette.blue)
                }
            }
        }
        item {
            HeroCard(Palette.monthGradient) {
                Column(Modifier.padding(20.dp)) {
                    Text("本月", color = Color.White.copy(alpha = 0.9f), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(totalDramas.toString(), color = Color.White, fontSize = 52.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(6.dp))
                        Text("部短剧", color = Color.White.copy(alpha = 0.95f), fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 8.dp))
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatPill("广告", totalAds.toString() + " 条", Modifier.weight(1f))
                        StatPill("日均", String.format(java.util.Locale.US, "%.1f 条", avg), Modifier.weight(1f))
                    }
                }
            }
        }
        item {
            SectionCard(title = "月度广告趋势") {
                if (points.all { it == 0 }) {
                    Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                        Text("本月暂无数据", color = c.textSub, fontSize = 13.sp)
                    }
                } else LineChartView(points)
            }
        }
        item {
            SectionCard(title = "每日热力") {
                HeatmapGrid(month, countsByDay)
            }
        }
    }
}

private fun shiftMonth(date: Date, delta: Int): Date {
    val c = Calendar.getInstance().apply { time = date; add(Calendar.MONTH, delta) }
    return c.time
}

@Composable
private fun HeatmapGrid(month: Date, counts: Map<String, Int>) {
    val c = AppColorsHolder
    val weekNames = listOf("日", "一", "二", "三", "四", "五", "六")
    val cal = Calendar.getInstance().apply { time = month; set(Calendar.DAY_OF_MONTH, 1) }
    val firstWeekday = cal.get(Calendar.DAY_OF_WEEK) // 1=Sun
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val cells = mutableListOf<Date?>()
    for (i in 1 until firstWeekday) cells.add(null)
    for (d in 1..daysInMonth) {
        val cc = Calendar.getInstance().apply { time = month; set(Calendar.DAY_OF_MONTH, d) }
        cells.add(cc.time)
    }

    Column {
        Row(Modifier.fillMaxWidth()) {
            weekNames.forEach { n ->
                Text(n, color = c.textSub, fontSize = 11.sp, textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(6.dp))
        val rows = (cells.size + 6) / 7
        for (r in 0 until rows) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (col in 0 until 7) {
                    val idx = r * 7 + col
                    val date = cells.getOrNull(idx)
                    if (date == null) {
                        Box(Modifier.weight(1f).height(36.dp))
                    } else {
                        val ds = AppConstants.dateString(date)
                        val cnt = counts[ds] ?: 0
                        val isToday = AppConstants.isToday(date)
                        Box(
                            Modifier.weight(1f).height(36.dp).clip(RoundedCornerShape(8.dp))
                                .background(heatBg(cnt, c.isDark, c.cardElev))
                                .then(if (isToday) Modifier else Modifier),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(Calendar.getInstance().apply { time = date }.get(Calendar.DAY_OF_MONTH).toString(),
                                color = if (cnt >= 4) Color.White else c.text, fontSize = 12.sp,
                                fontWeight = if (cnt > 0) FontWeight.SemiBold else FontWeight.Normal)
                        }
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
        }
    }
}

private fun heatBg(count: Int, isDark: Boolean, emptyColor: Color): Color {
    if (count == 0) return emptyColor
    return when {
        count <= 1 -> if (isDark) Palette.blueLightDark else Palette.blueLight
        count <= 3 -> Palette.blue.copy(alpha = 0.35f)
        count <= 6 -> Palette.blue.copy(alpha = 0.55f)
        else -> Palette.blue.copy(alpha = 0.85f)
    }
}
