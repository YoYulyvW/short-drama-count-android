package com.shortdrama.count.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shortdrama.count.model.Drama
import com.shortdrama.count.ui.components.PressableCard
import com.shortdrama.count.ui.theme.AppColorsHolder
import com.shortdrama.count.ui.theme.Palette
import com.shortdrama.count.ui.theme.parseHex
import com.shortdrama.count.util.AppConstants
import com.shortdrama.count.util.Haptics
import com.shortdrama.count.viewmodel.AppViewModel

@Composable
fun DramaCard(vm: AppViewModel, date: String, drama: Drama, isLatest: Boolean) {
    val settings by vm.settings.collectAsState()
    val days by vm.days.collectAsState()
    val c = AppColorsHolder
    val day = days[date] ?: com.shortdrama.count.model.DayData(date)
    val records = day.records.filter { it.title == drama.title && it.isFast == drama.isFast }
    val total = records.sumOf { it.count }

    var userExpanded by remember { mutableStateOf<Boolean?>(null) }
    var showRename by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    var showAddPlatform by remember { mutableStateOf(false) }
    val expanded = userExpanded ?: if (settings.autoCollapse) isLatest else true

    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.card)
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(drama.title, color = c.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    if (drama.isFast) {
                        Spacer(Modifier.width(6.dp))
                        Text("极速", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Palette.blue)
                                .padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    if (records.isEmpty()) "暂无计数 · 点开添加" else records.joinToString("  ") { it.platform.take(1) + ":" + it.count },
                    color = c.textSub, fontSize = 12.sp
                )
            }
            Text(total.toString(), color = Palette.indigo, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(6.dp))
            PressableCard(onClick = { userExpanded = !expanded }) {
                Icon(if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, null,
                    tint = c.textSub, modifier = Modifier.size(22.dp))
            }
        }

        if (expanded) {
            Spacer(Modifier.height(10.dp))
            Divider(color = c.divider)
            Spacer(Modifier.height(8.dp))
            records.forEach { rec ->
                val cfg = vm.platformConfig(rec.platform)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                    Box(Modifier.size(10.dp).clip(RoundedCornerShape(5.dp)).background(parseHex(cfg.colorHex)))
                    Spacer(Modifier.width(8.dp))
                    Text(rec.platform, color = c.text, fontSize = 14.sp, modifier = Modifier.weight(1f))
                    StepButton(Icons.Filled.Remove, Palette.grayBtn) {
                        vm.incrementPlatform(date, drama.title, rec.platform, drama.isFast, -1); Haptics.tap()
                    }
                    Text(rec.count.toString(), color = c.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.width(40.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    StepButton(Icons.Filled.Add, Palette.blue) {
                        vm.incrementPlatform(date, drama.title, rec.platform, drama.isFast, 1); Haptics.tap()
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlineChip("添加平台", Icons.Filled.Add) { showAddPlatform = true }
                OutlineChip("重命名", Icons.Filled.Edit) { showRename = true }
                OutlineChip("删除", Icons.Filled.Delete, tint = Palette.red) { showDelete = true }
            }
        }
    }

    if (showRename) {
        var text by remember { mutableStateOf(drama.title) }
        AlertDialog(
            onDismissRequest = { showRename = false },
            confirmButton = { TextButton(onClick = { vm.renameDrama(date, drama.title, text, drama.isFast); showRename = false }) { Text("确定") } },
            dismissButton = { TextButton(onClick = { showRename = false }) { Text("取消") } },
            title = { Text("重命名") },
            text = { OutlinedTextField(value = text, onValueChange = { text = it }, singleLine = true) },
        )
    }
    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            confirmButton = { TextButton(onClick = { vm.deleteDrama(date, drama.title, drama.isFast); showDelete = false; Haptics.warning() }) { Text("删除", color = Palette.red) } },
            dismissButton = { TextButton(onClick = { showDelete = false }) { Text("取消") } },
            title = { Text("删除短剧") },
            text = { Text("确定删除「" + drama.title + "」及其所有计数？") },
        )
    }
    if (showAddPlatform) {
        AddPlatformDialog(vm, onDismiss = { showAddPlatform = false }) { platform ->
            vm.incrementPlatform(date, drama.title, platform, drama.isFast, 1)
            showAddPlatform = false
        }
    }
}

@Composable
private fun StepButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    bg: Color,
    onClick: () -> Unit,
) {
    PressableCard(onClick = onClick) {
        Box(
            Modifier.size(30.dp).clip(RoundedCornerShape(8.dp)).background(bg),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = Color.White, modifier = Modifier.size(16.dp)) }
    }
}

@Composable
private fun OutlineChip(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color = AppColorsHolder.textSub,
    onClick: () -> Unit,
) {
    PressableCard(onClick = onClick) {
        Row(
            Modifier.clip(RoundedCornerShape(10.dp)).background(AppColorsHolder.cardElev)
                .padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text(text, color = tint, fontSize = 12.sp)
        }
    }
}

@Composable
private fun AddPlatformDialog(vm: AppViewModel, onDismiss: () -> Unit, onPick: (String) -> Unit) {
    val settings by vm.settings.collectAsState()
    var custom by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
        title = { Text("选择平台") },
        text = {
            Column {
                settings.platforms.forEach { cfg ->
                    PressableCard(onClick = { onPick(cfg.name) }, modifier = Modifier.fillMaxWidth()) {
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(Modifier.size(10.dp).clip(RoundedCornerShape(5.dp)).background(parseHex(cfg.colorHex)))
                            Spacer(Modifier.width(8.dp))
                            Text(cfg.name, color = AppColorsHolder.text, fontSize = 14.sp)
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(value = custom, onValueChange = { custom = it },
                    label = { Text("自定义平台") }, singleLine = true)
                Spacer(Modifier.height(8.dp))
                Button(onClick = { if (custom.isNotBlank()) onPick(custom.trim()) }) { Text("添加自定义") }
            }
        },
    )
}
