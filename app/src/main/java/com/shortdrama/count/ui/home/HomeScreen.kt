@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.shortdrama.count.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shortdrama.count.model.ActiveSheet
import com.shortdrama.count.model.Drama
import com.shortdrama.count.ui.components.BounceNumber
import com.shortdrama.count.ui.components.PressableCard
import com.shortdrama.count.ui.theme.AppColorsHolder
import com.shortdrama.count.ui.theme.Palette
import com.shortdrama.count.util.AppConstants
import com.shortdrama.count.util.Haptics
import com.shortdrama.count.viewmodel.AppViewModel
import com.shortdrama.count.viewmodel.ToastStyle
import java.util.Date

@Composable
fun HomeScreen(vm: AppViewModel) {
    val date by vm.currentDate.collectAsState()
    val settings by vm.settings.collectAsState()
    val dateStr = AppConstants.dateString(date)
    Column(Modifier.fillMaxSize().background(AppColorsHolder.bg).statusBarsPadding()) {
        TopInfoBar(vm, dateStr)
        TitleInputBar(vm, dateStr)
        if (settings.showQuickTools) ToolBar(vm)
        DramaList(vm, dateStr)
    }
}

@Composable
private fun TopInfoBar(vm: AppViewModel, dateStr: String) {
    val summary = vm.summary(dateStr)
    val date by vm.currentDate.collectAsState()
    val c = AppColorsHolder
    Column(Modifier.padding(horizontal = 18.dp).padding(top = 12.dp, bottom = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("开饭了", color = c.text, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            DateSelector(vm, date)
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.DateRange, null, tint = c.textSub, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(5.dp))
            Text(dateLabel(date), color = c.textSub, fontSize = 12.sp)
            Spacer(Modifier.weight(1f))
            Text("共 ", color = c.textSub, fontSize = 12.sp)
            BounceNumber(summary.dramaCount, Palette.indigo, 13, FontWeight.Bold)
            Text(" 部 · ", color = c.textSub, fontSize = 12.sp)
            BounceNumber(summary.total, Palette.orange, 13, FontWeight.Bold)
            Text(" 条广告", color = c.textSub, fontSize = 12.sp)
        }
    }
}

private fun dateLabel(date: Date): String = when {
    AppConstants.isToday(date) -> "今日 · " + AppConstants.chineseDate(date)
    AppConstants.isYesterday(date) -> "昨天 · " + AppConstants.chineseDate(date)
    else -> AppConstants.chineseDate(date)
}

@Composable
private fun DateSelector(vm: AppViewModel, date: Date) {
    var showPicker by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        IconCircle(Icons.Filled.ChevronLeft, true,
            if (AppColorsHolder.isDark) Palette.blueLightDark else Palette.blueLight) {
            vm.shiftDate(-1); Haptics.tap()
        }
        PressableCard(onClick = { showPicker = true }) {
            Text(AppConstants.dateString(date), color = Palette.blue, fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (AppColorsHolder.isDark) Palette.blueLightDark else Palette.blueLight)
                    .padding(horizontal = 10.dp, vertical = 6.dp))
        }
        val isToday = AppConstants.isToday(date)
        IconCircle(Icons.Filled.ChevronRight, !isToday,
            if (isToday) AppColorsHolder.cardElev
            else (if (AppColorsHolder.isDark) Palette.blueLightDark else Palette.blueLight)) {
            vm.shiftDate(1); Haptics.tap()
        }
    }
    if (showPicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = date.time)
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { vm.setDate(Date(it)) }
                    showPicker = false
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text("取消") } },
        ) { DatePicker(state = state) }
    }
}

@Composable
private fun IconCircle(icon: ImageVector, enabled: Boolean, bg: Color, onClick: () -> Unit) {
    PressableCard(onClick = if (enabled) onClick else null) {
        Box(Modifier.size(30.dp).clip(CircleShape).background(bg), contentAlignment = Alignment.Center) {
            Icon(icon, null,
                tint = if (enabled) Palette.blue else AppColorsHolder.textSub.copy(alpha = 0.4f),
                modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun ToolBar(vm: AppViewModel) {
    val settings by vm.settings.collectAsState()
    val scanning by vm.scanningDevices.collectAsState()
    val size: Dp = settings.toolButtonSize.dp
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(top = 8.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ToolButton(Icons.Filled.Download, "导入", Palette.green, size) {
            vm.setActiveSheet(ActiveSheet.IMPORT_DATA); Haptics.tap()
        }
        ToolButton(Icons.Filled.Description, "明文", Palette.blue, size) {
            vm.setActiveSheet(ActiveSheet.EXPORT_TEXT); Haptics.tap()
        }
        ToolButton(Icons.Filled.Lock, "密文", Palette.indigo, size) {
            vm.setActiveSheet(ActiveSheet.EXPORT_CODE); Haptics.tap()
        }
        ToolButton(if (scanning) Icons.Filled.HourglassEmpty else Icons.Filled.Send,
            if (scanning) "扫描中" else "推送", Color(0xFFFF6B9D), size) {
            if (!scanning) vm.scanDevicesForPush(); Haptics.tap()
        }
        ToolButton(Icons.Filled.Photo, "图片", Color(0xFF7B4BC4), size) {
            vm.setActiveSheet(ActiveSheet.EXPORT_IMAGE); Haptics.tap()
        }
        ToolButton(Icons.Filled.Undo, "回档", Palette.orange, size) {
            vm.setActiveSheet(ActiveSheet.UNDO); Haptics.tap()
        }
    }
}

@Composable
private fun ToolButton(icon: ImageVector, title: String, color: Color, size: Dp, action: () -> Unit) {
    PressableCard(onClick = action) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(size)) {
            Box(
                Modifier.size(size * 0.62f).clip(RoundedCornerShape(size * 0.2f)).background(color),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, null, tint = Color.White, modifier = Modifier.size(size * 0.34f)) }
            Spacer(Modifier.height(6.dp))
            Text(title, color = AppColorsHolder.text, fontSize = 10.sp,
                fontWeight = FontWeight.Medium, maxLines = 1)
        }
    }
}

@Composable
private fun TitleInputBar(vm: AppViewModel, dateStr: String) {
    var title by rememberSaveable { mutableStateOf("") }
    var fastInput by rememberSaveable { mutableStateOf(false) }
    var showOcr by remember { mutableStateOf(false) }
    val c = AppColorsHolder
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 18.dp).padding(bottom = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            Modifier.weight(1f).height(46.dp).clip(RoundedCornerShape(13.dp)).background(c.card)
                .padding(start = 14.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            androidx.compose.foundation.text.BasicTextField(
                value = title, onValueChange = { title = it },
                modifier = Modifier.weight(1f),
                textStyle = androidx.compose.ui.text.TextStyle(color = c.text, fontSize = 15.sp),
                singleLine = true,
                decorationBox = { inner ->
                    Box { if (title.isEmpty()) Text("输入剧名", color = c.textSub, fontSize = 15.sp); inner() }
                },
            )
            if (title.isNotEmpty()) {
                IconButton(onClick = { title = "" }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Filled.Clear, null, tint = c.textSub, modifier = Modifier.size(16.dp))
                }
            }
            IconButton(onClick = { showOcr = true }, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Filled.PhotoCamera, null, tint = Palette.blue, modifier = Modifier.size(20.dp))
            }
        }
        PressableCard(onClick = { fastInput = !fastInput; Haptics.tap() }) {
            Box(
                Modifier.size(54.dp, 46.dp).clip(RoundedCornerShape(13.dp))
                    .background(if (fastInput) Palette.blue else c.cardElev),
                contentAlignment = Alignment.Center,
            ) { Text("极速", color = if (fastInput) Color.White else c.textSub, fontSize = 14.sp, fontWeight = FontWeight.SemiBold) }
        }
        PressableCard(onClick = {
            val raw = title.trim()
            if (raw.isEmpty()) {
                Haptics.warning(); vm.showToast("请输入剧名", ToastStyle.ERROR)
            } else {
                val (t, sf) = AppConstants.splitFast(raw)
                vm.addDrama(dateStr, t, fastInput || sf)
                title = ""; fastInput = false; Haptics.success()
            }
        }) {
            Box(
                Modifier.size(64.dp, 46.dp).clip(RoundedCornerShape(13.dp)).background(Palette.green),
                contentAlignment = Alignment.Center,
            ) { Text("新增", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold) }
        }
    }
    if (showOcr) {
        OcrDialog(vm) { result ->
            showOcr = false
            if (!result.isNullOrBlank()) title = result
        }
    }
}

@Composable
private fun DramaList(vm: AppViewModel, dateStr: String) {
    val days by vm.days.collectAsState()
    val day = days[dateStr]
    val dramas = day?.dramas?.reversed() ?: emptyList()
    if (dramas.isEmpty()) {
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(Icons.Filled.MovieFilter, null, tint = AppColorsHolder.textSub, modifier = Modifier.size(54.dp))
            Spacer(Modifier.height(12.dp))
            Text("还没有短剧", color = AppColorsHolder.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            Text("在上方输入剧名开始记录", color = AppColorsHolder.textSub, fontSize = 13.sp)
        }
        return
    }
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(vertical = 8.dp),
    ) {
        items(dramas, key = { it.id }) { drama ->
            DramaCard(vm, dateStr, drama, isLatest = drama.id == dramas.first().id)
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}
