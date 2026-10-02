package com.shortdrama.count.ui.month

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
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
import com.shortdrama.count.ui.components.HeroPills
import com.shortdrama.count.ui.components.KPIRow
import com.shortdrama.count.ui.components.KpiItem
import com.shortdrama.count.ui.components.LineChartView
import com.shortdrama.count.ui.components.RankBarListImpl
import com.shortdrama.count.ui.components.RankItem
import com.shortdrama.count.ui.theme.AppColorsHolder
import com.shortdrama.count.ui.theme.Palette
import com.shortdrama.count.util.AppConstants
import com.shortdrama.count.util.Haptics
import com.shortdrama.count.viewmodel.AppViewModel
import java.util.Calendar
import java.util.Date

@Composable
fun MonthStatsScreen(vm: AppViewModel) {
    var month by remember { mutableStateOf(Date()) }
    val days by vm.days.collectAsState()
    var dayDetail by remember { mutableStateOf<com.shortdrama.count.model.DayData?>(null) }
    val c = AppColorsHolder

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
    var activeDays = 0
    val platformSums = mutableMapOf<String, Int>()
    val points = mutableListOf<Int>()
    val countsByDay = mutableMapOf<String, Int>()
    for (d in dayList) {
        val ds = AppConstants.dateString(d)
        val day = days[ds]
        val ads = day?.records?.sumOf { it.count } ?: 0
        totalAds += ads
        totalDramas += day?.dramas?.size ?: 0
        if (ads > 0) activeDays++
        day?.records?.forEach { r -> platformSums[r.platform] = (platformSums[r.platform] ?: 0) + r.count }
        points.add(ads)
        countsByDay[ds] = ads
    }
    val avg = if (activeDays > 0) totalAds.toDouble() / activeDays else 0.0
    val perDrama = if (totalDramas > 0) totalAds.toDouble() / totalDramas else 0.0
    val isCurrentMonth = run {
        val c1 = Calendar.getInstance().apply { time = month }
        val c2 = Calendar.getInstance()
        c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) && c1.get(Calendar.MONTH) == c2.get(Calendar.MONTH)
    }
    val rankItems = platformSums.entries.sortedByDescending { it.value }.map { e ->
        val cfg = vm.platformConfig(e.key)
        RankItem(e.key, cfg.short, cfg.colorHex, e.value)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(c.bg),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
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
                    Text("本月共记录", color = Color.White.copy(alpha = 0.88f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(totalDramas.toString(), color = Color.White, fontSize = 56.sp, fontWeight = FontWeight.Black)
                        Spacer(Modifier.width(6.dp))
                        Text("部短剧", color = Color.White.copy(alpha = 0.92f), fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 8.dp))
                    }
                    Spacer(Modifier.height(10.dp))
                    HeroPills(listOf(
                        "广告总数" to totalAds.toString(),
                        "日均" to String.format(java.util.Locale.US, "%.1f", avg),
                        "活跃天" to activeDays.toString(),
                    ))
                }
            }
        }
        item {
            KPIRow(listOf(
                KpiItem(totalDramas.toString(), "短剧", Palette.indigo),
                KpiItem(totalAds.toString(), "广告", Palette.orange),
                KpiItem(String.format(java.util.Locale.US, "%.1f", perDrama), "条/部", Palette.green),
                KpiItem(platformSums.size.toString(), "平台", Palette.blue),
            ))
        }
        item {
            Column {
                Row(Modifier.padding(horizontal = 4.dp, vertical = 4.dp)) {
                    Text("广告趋势", color = c.textSub, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.weight(1f))
                    Text("近 " + points.size + " 天", color = c.textSub.copy(alpha = 0.8f), fontSize = 11.sp)
                }
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.card).padding(14.dp)) {
                    if (points.all { it == 0 }) {
                        Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                            Text("本月暂无数据", color = c.textSub, fontSize = 13.sp)
                        }
                    } else {
                        Column {
                            LineChartView(points)
                            Spacer(Modifier.height(6.dp))
                            Row {
                                Text("1", color = c.textSub, fontSize = 10.sp)
                                Spacer(Modifier.weight(1f))
                                val peak = points.maxOrNull() ?: 0
                                if (peak > 0) Text("峰值 " + peak, color = Palette.indigo, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.weight(1f))
                                Text(points.size.toString(), color = c.textSub, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }
        item {
            Column {
                Row(Modifier.padding(horizontal = 4.dp, vertical = 4.dp)) {
                    Text("平台分布", color = c.textSub, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.weight(1f))
                    Text("共 " + totalAds + " 条", color = c.textSub.copy(alpha = 0.8f), fontSize = 11.sp)
                }
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.card).padding(14.dp)) {
                    if (rankItems.isEmpty()) {
                        Text("本月暂无数据", color = c.textSub, fontSize = 13.sp,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), textAlign = TextAlign.Center)
                    } else {
                        RankBarListImpl(rankItems)
                    }
                }
            }
        }
        item {
            Column {
                Row(Modifier.padding(horizontal = 4.dp, vertical = 4.dp)) {
                    Text("看剧日历", color = c.textSub, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.weight(1f))
                    Text("点击查看当天", color = c.textSub.copy(alpha = 0.8f), fontSize = 11.sp)
                }
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.card).padding(14.dp)) {
                    HeatmapGrid(month, countsByDay) { ds ->
                        days[ds]?.let { dayDetail = it }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(70.dp)) }
    }

    dayDetail?.let { data ->
        DayDetailDialog(data = data, vm = vm, onDismiss = { dayDetail = null })
    }
}

@Composable
private fun DayDetailDialog(
    data: com.shortdrama.count.model.DayData,
    vm: AppViewModel,
    onDismiss: () -> Unit,
) {
    val c = AppColorsHolder
    val days by vm.days.collectAsState()
    var currentDate by remember { mutableStateOf(parseDate(data.date) ?: Date()) }
    val currentStr = AppConstants.dateString(currentDate)
    val current = days[currentStr] ?: com.shortdrama.count.model.DayData(currentStr)

    val valid = current.dramas.reversed().filter { drama ->
        current.records.any { it.title == drama.title && it.isFast == drama.isFast }
    }
    val platformSums = mutableMapOf<String, Int>()
    valid.forEach { drama ->
        current.records.filter { it.title == drama.title && it.isFast == drama.isFast }
            .forEach { r -> platformSums[r.platform] = (platformSums[r.platform] ?: 0) + r.count }
    }
    val ads = platformSums.values.sum()
    val rank = platformSums.entries.sortedByDescending { it.value }.map { e ->
        val cfg = vm.platformConfig(e.key)
        RankItem(e.key, cfg.short, cfg.colorHex, e.value)
    }
    val perDrama = if (valid.size > 0) ads.toDouble() / valid.size else 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("完成") } },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { currentDate = shiftDay(currentDate, -1) }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.ChevronLeft, null, tint = Palette.blue)
                }
                Text(AppConstants.chineseDate(currentDate), modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                val isToday = AppConstants.isToday(currentDate)
                IconButton(
                    onClick = { if (!isToday) currentDate = shiftDay(currentDate, 1) },
                    enabled = !isToday, modifier = Modifier.size(32.dp),
                ) { Icon(Icons.Filled.ChevronRight, null, tint = if (isToday) c.textSub.copy(alpha=0.4f) else Palette.blue) }
            }
        },
        text = {
            Column(Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                HeroCard(Palette.monthGradient) {
                    Column(Modifier.padding(16.dp)) {
                        Text("当天共记录", color = Color.White.copy(alpha = 0.88f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(ads.toString(), color = Color.White, fontSize = 44.sp, fontWeight = FontWeight.Black)
                            Spacer(Modifier.width(6.dp))
                            Text("条广告", color = Color.White.copy(alpha = 0.92f), fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 6.dp))
                        }
                        Spacer(Modifier.height(10.dp))
                        HeroPills(listOf(
                            "短剧" to (valid.size.toString() + " 部"),
                            "平台" to (platformSums.size.toString() + " 个"),
                            "均/部" to String.format(java.util.Locale.US, "%.1f", perDrama),
                        ))
                    }
                }

                if (rank.isNotEmpty()) {
                    Text("平台累计", color = c.textSub, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.card).padding(12.dp)) {
                        RankBarListImpl(rank)
                    }
                }

                if (valid.isEmpty()) {
                    Text("当天没有明细", color = c.textSub, fontSize = 13.sp,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp), textAlign = TextAlign.Center)
                } else {
                    Text("剧集明细", color = c.textSub, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    valid.forEach { drama ->
                        val recs = current.records.filter { it.title == drama.title && it.isFast == drama.isFast }
                        val total = recs.sumOf { it.count }
                        Column(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                                .background(c.card).padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(drama.title, color = c.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                if (drama.isFast) {
                                    Spacer(Modifier.width(6.dp))
                                    Text("极速", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                                        modifier = Modifier.clip(RoundedCornerShape(5.dp)).background(Palette.orange)
                                            .padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                                Spacer(Modifier.weight(1f))
                                Text(total.toString(), color = Palette.indigo, fontSize = 17.sp, fontWeight = FontWeight.Black)
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                recs.forEach { r ->
                                    val cfg = vm.platformConfig(r.platform)
                                    Row(
                                        Modifier.clip(RoundedCornerShape(8.dp)).background(c.cardElev)
                                            .padding(horizontal = 9.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                                    ) {
                                        Box(Modifier.size(8.dp).clip(CircleShape).background(com.shortdrama.count.ui.theme.parseHex(cfg.colorHex)))
                                        Text(cfg.name, color = c.textSub, fontSize = 12.sp)
                                        Text(r.count.toString(), color = c.text, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
    )
}

private fun parseDate(s: String): Date? {
    return try {
        val f = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        f.parse(s)
    } catch (e: Exception) { null }
}

private fun shiftDay(date: Date, delta: Int): Date {
    val c = Calendar.getInstance().apply { time = date; add(Calendar.DAY_OF_MONTH, delta) }
    return c.time
}

private fun shiftMonth(date: Date, delta: Int): Date {
    val c = Calendar.getInstance().apply { time = date; add(Calendar.MONTH, delta) }
    return c.time
}

@Composable
private fun HeatmapGrid(month: Date, counts: Map<String, Int>, onSelect: (String) -> Unit) {
    val c = AppColorsHolder
    val weekNames = listOf("日", "一", "二", "三", "四", "五", "六")
    val cal = Calendar.getInstance().apply { time = month; set(Calendar.DAY_OF_MONTH, 1) }
    val firstWeekday = cal.get(Calendar.DAY_OF_WEEK)
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
                                .then(if (cnt > 0) Modifier.clickable { onSelect(ds); Haptics.tap() } else Modifier)
                                .then(if (isToday) Modifier.background(Color.Transparent) else Modifier),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(Calendar.getInstance().apply { time = date }.get(Calendar.DAY_OF_MONTH).toString(),
                                    color = if (cnt >= 4) Color.White else c.text, fontSize = 12.sp,
                                    fontWeight = if (cnt > 0) FontWeight.SemiBold else FontWeight.Normal)
                                if (cnt > 0) {
                                    Text(cnt.toString(), color = if (cnt >= 4) Color.White.copy(alpha=0.85f) else c.textSub,
                                        fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                                }
                            }
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
