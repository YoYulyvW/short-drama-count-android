package com.shortdrama.count.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shortdrama.count.model.RemoteDatabaseType
import com.shortdrama.count.ui.components.SectionCard
import com.shortdrama.count.ui.theme.AppColorsHolder
import com.shortdrama.count.ui.theme.Palette
import com.shortdrama.count.ui.theme.parseHex
import com.shortdrama.count.util.Haptics
import com.shortdrama.count.viewmodel.AppViewModel

@Composable
fun SettingsScreen(vm: AppViewModel) {
    val settings by vm.settings.collectAsState()
    val lastMessage by vm.lastMessage.collectAsState()
    val syncStatus by vm.lastSyncStatus.collectAsState()
    val c = AppColorsHolder

    LazyColumn(
        Modifier.fillMaxSize().background(c.bg),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { Text("设置", color = c.text, fontSize = 26.sp, fontWeight = FontWeight.Bold) }

        item {
            SectionCard(title = "反馈") {
                Column {
                    SwitchRow("震动反馈", settings.hapticFeedback) {
                        vm.updateSettings(settings.copy(hapticFeedback = it)); vm.saveSettings()
                    }
                    SwitchRow("音效反馈", settings.soundFeedback) {
                        vm.updateSettings(settings.copy(soundFeedback = it)); vm.saveSettings()
                    }
                }
            }
        }

        item {
            SectionCard(title = "显示") {
                Column {
                    SwitchRow("自动折叠", settings.autoCollapse) {
                        vm.updateSettings(settings.copy(autoCollapse = it)); vm.saveSettings()
                    }
                    SwitchRow("显示快捷工具", settings.showQuickTools) {
                        vm.updateSettings(settings.copy(showQuickTools = it)); vm.saveSettings()
                    }
                    SwitchRow("按广告数排序", settings.sortByAds) {
                        vm.updateSettings(settings.copy(sortByAds = it)); vm.saveSettings()
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("工具按钮大小：" + settings.toolButtonSize, color = c.textSub, fontSize = 13.sp)
                    Slider(
                        value = settings.toolButtonSize.toFloat(),
                        onValueChange = { vm.updateSettings(settings.copy(toolButtonSize = it.toInt())) },
                        onValueChangeFinished = { vm.saveSettings() },
                        valueRange = 40f..80f,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("每日撤销上限：" + settings.maxUndoPerDay, color = c.textSub, fontSize = 13.sp)
                    Slider(
                        value = settings.maxUndoPerDay.toFloat(),
                        onValueChange = { vm.updateSettings(settings.copy(maxUndoPerDay = it.toInt())) },
                        onValueChangeFinished = { vm.saveSettings() },
                        valueRange = 1f..30f,
                    )
                }
            }
        }

        item {
            SectionCard(title = "平台") {
                Column {
                    settings.platforms.forEach { cfg ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
                            Box(Modifier.size(14.dp).clip(RoundedCornerShape(4.dp)).background(parseHex(cfg.colorHex)))
                            Spacer(Modifier.width(10.dp))
                            Text(cfg.name, color = c.text, fontSize = 14.sp, modifier = Modifier.weight(1f))
                            Text(cfg.short, color = c.textSub, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        item {
            SectionCard(title = "局域网推送") {
                Column {
                    SwitchRow("启用局域网服务", settings.lanEnabled) {
                        vm.updateSettings(settings.copy(lanEnabled = it)); vm.saveSettings()
                    }
                    Spacer(Modifier.height(6.dp))
                    Button(onClick = { vm.scanDevicesForPush() }, modifier = Modifier.fillMaxWidth()) {
                        Text("扫描设备并推送")
                    }
                }
            }
        }

        item {
            SectionCard(title = "远程同步") {
                Column {
                    SwitchRow("启用远程同步", settings.remoteEnabled) {
                        vm.updateSettings(settings.copy(remoteEnabled = it)); vm.saveSettings()
                    }
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = settings.remoteHost, onValueChange = { vm.updateSettings(settings.copy(remoteHost = it)) },
                        label = { Text("服务器地址") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = settings.remoteToken, onValueChange = { vm.updateSettings(settings.copy(remoteToken = it)) },
                        label = { Text("Token") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(6.dp))
                    Row {
                        RemoteDatabaseType.entries.forEach { t ->
                            FilterChip(
                                selected = settings.remoteDBType == t,
                                onClick = { vm.updateSettings(settings.copy(remoteDBType = t)) },
                                label = { Text(t.display) },
                                modifier = Modifier.padding(end = 6.dp),
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { vm.syncToRemote() }, modifier = Modifier.fillMaxWidth()) { Text("立即同步") }
                    if (syncStatus.isNotEmpty()) {
                        Spacer(Modifier.height(6.dp))
                        Text(syncStatus, color = c.textSub, fontSize = 12.sp)
                    }
                }
            }
        }

        item {
            SectionCard(title = "更新") {
                Column {
                    SwitchRow("自动检查更新", settings.autoUpdateCheck) {
                        vm.updateSettings(settings.copy(autoUpdateCheck = it)); vm.saveSettings()
                    }
                    SwitchRow("静默下载", settings.silentDownload) {
                        vm.updateSettings(settings.copy(silentDownload = it)); vm.saveSettings()
                    }
                    SwitchRow("自动提示安装", settings.autoPromptInstall) {
                        vm.updateSettings(settings.copy(autoPromptInstall = it)); vm.saveSettings()
                    }
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = settings.customUpdateProxy, onValueChange = { vm.updateSettings(settings.copy(customUpdateProxy = it)) },
                        label = { Text("自定义代理（可选）") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { vm.checkForUpdate(silent = false, force = true) }, modifier = Modifier.weight(1f)) {
                            Text("检查更新")
                        }
                        OutlinedButton(onClick = { vm.testCustomProxy(settings.customUpdateProxy) }, modifier = Modifier.weight(1f)) {
                            Text("测试代理")
                        }
                    }
                    lastMessage?.let {
                        Spacer(Modifier.height(6.dp))
                        Text(it, color = c.textSub, fontSize = 12.sp)
                    }
                }
            }
        }

        item {
            SectionCard(title = "关于") {
                Column {
                    Text("开饭了 · 短剧计数（Android）", color = c.text, fontSize = 14.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("版本 " + com.shortdrama.count.model.AppVersion.full, color = c.textSub, fontSize = 12.sp)
                }
            }
        }
        item { Spacer(Modifier.height(40.dp)) }
    }
}

@Composable
private fun SwitchRow(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
        Text(title, color = AppColorsHolder.text, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = { onCheckedChange(it); Haptics.tap() })
    }
}
