@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.shortdrama.count.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shortdrama.count.model.AppVersion
import com.shortdrama.count.service.RelayClient
import com.shortdrama.count.ui.theme.AppColorsHolder
import com.shortdrama.count.ui.theme.Palette
import com.shortdrama.count.ui.theme.parseHex
import com.shortdrama.count.util.AppConstants
import com.shortdrama.count.util.Haptics
import com.shortdrama.count.viewmodel.AppViewModel
import com.shortdrama.count.viewmodel.ToastStyle

private enum class SettingsPage { HOME, GENERAL, CONNECTION, DATA, UPDATE }

@Composable
fun SettingsScreen(vm: AppViewModel) {
    var page by remember { mutableStateOf(SettingsPage.HOME) }
    BackHandler(enabled = page != SettingsPage.HOME) { page = SettingsPage.HOME }

    when (page) {
        SettingsPage.HOME -> SettingsHomePage(vm) { page = it }
        SettingsPage.GENERAL -> GeneralSettingsPage(vm) { page = SettingsPage.HOME }
        SettingsPage.CONNECTION -> ConnectionSettingsPage(vm) { page = SettingsPage.HOME }
        SettingsPage.DATA -> DataSettingsPage(vm) { page = SettingsPage.HOME }
        SettingsPage.UPDATE -> UpdateSettingsPage(vm) { page = SettingsPage.HOME }
    }
}

// ==================== 主页：分类入口 ====================
@Composable
private fun SettingsHomePage(vm: AppViewModel, onOpen: (SettingsPage) -> Unit) {
    val settings by vm.settings.collectAsState()
    val days by vm.days.collectAsState()
    val lanRunning by vm.lanRunning.collectAsState()
    val relayState by vm.relayState.collectAsState()
    val c = AppColorsHolder

    val connBadge: Pair<String, Color> = when {
        settings.relayEnabled -> when (relayState) {
            RelayClient.State.CONNECTED -> "已连接" to Palette.green
            RelayClient.State.CONNECTING -> "连接中" to Palette.orange
            RelayClient.State.ERROR -> "异常" to Palette.red
            else -> "未启用" to c.textSub
        }
        settings.lanEnabled -> (if (lanRunning) "局域网" to Palette.green else "局域网" to Palette.orange)
        else -> "未启用" to c.textSub
    }

    LazyColumn(
        Modifier.fillMaxSize().background(c.bg),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { Text("设置", color = c.text, fontSize = 26.sp, fontWeight = FontWeight.Bold) }

        item {
            SettingsGroupCard {
                SettingsRow(
                    iconEmoji = "\u2699\uFE0F", iconBg = "#8E8E93",
                    title = "通用", subtitle = "显示 · 反馈 · 统计",
                    onClick = { onOpen(SettingsPage.GENERAL) },
                )
            }
        }
        item {
            SettingsGroupCard {
                SettingsRow(
                    iconEmoji = "\uD83D\uDCE1", iconBg = "#34C759",
                    title = "推送与连接", subtitle = null,
                    badge = connBadge.first, badgeColor = connBadge.second,
                    onClick = { onOpen(SettingsPage.CONNECTION) },
                )
            }
        }
        item {
            SettingsGroupCard {
                SettingsRow(
                    iconEmoji = "\uD83D\uDDC4", iconBg = "#5E5CE6",
                    title = "数据",
                    subtitle = days.size.toString() + " 天 · " + settings.platforms.size + " 平台",
                    onClick = { onOpen(SettingsPage.DATA) },
                )
            }
        }
        item {
            SettingsGroupCard {
                SettingsRow(
                    iconEmoji = "\uD83D\uDD04", iconBg = "#007AFF",
                    title = "软件更新",
                    subtitle = "v" + AppVersion.name,
                    onClick = { onOpen(SettingsPage.UPDATE) },
                )
            }
        }
        item { Spacer(Modifier.height(70.dp)) }
    }
}

@Composable
private fun SettingsGroupCard(content: @Composable () -> Unit) {
    val c = AppColorsHolder
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.card)
    ) { content() }
}

@Composable
private fun SettingsRow(
    iconEmoji: String, iconBg: String,
    title: String, subtitle: String?,
    badge: String? = null, badgeColor: Color = Palette.textSub,
    onClick: (() -> Unit)? = null,
) {
    val c = AppColorsHolder
    Row(
        Modifier.fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick(); Haptics.tap() } else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier.size(30.dp).clip(RoundedCornerShape(7.dp)).background(parseHex(iconBg)),
            contentAlignment = Alignment.Center,
        ) { Text(iconEmoji, fontSize = 15.sp) }
        Text(title, color = c.text, fontSize = 16.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        if (badge != null) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Box(Modifier.size(7.dp).clip(CircleShape).background(badgeColor))
                Text(badge, color = badgeColor, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
            }
        } else if (subtitle != null) {
            Text(subtitle, color = c.textSub, fontSize = 14.sp)
        }
        if (onClick != null) {
            Icon(Icons.Filled.ChevronRight, null, tint = c.textSub.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
        }
    }
}

// ==================== 通用 ====================
@Composable
private fun GeneralSettingsPage(vm: AppViewModel, onBack: () -> Unit) {
    val settings by vm.settings.collectAsState()
    val c = AppColorsHolder

    SubPageScaffold(title = "通用", onBack = onBack) {
        item {
            GroupTitle("显示偏好")
            SettingsGroupCard {
                Column {
                    SwitchRow("首页显示工具行", settings.showQuickTools) {
                        vm.updateSettings(settings.copy(showQuickTools = it)); vm.saveSettings()
                    }
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Row {
                            Text("工具按钮大小", color = c.text, fontSize = 14.sp, modifier = Modifier.weight(1f))
                            Text(settings.toolButtonSize.toString(), color = c.textSub, fontSize = 14.sp)
                        }
                        Slider(
                            value = settings.toolButtonSize.toFloat(),
                            onValueChange = { vm.updateSettings(settings.copy(toolButtonSize = it.toInt())) },
                            onValueChangeFinished = { vm.saveSettings() },
                            valueRange = 40f..80f,
                        )
                    }
                    SwitchRow("自动折叠剧集", settings.autoCollapse) {
                        vm.updateSettings(settings.copy(autoCollapse = it)); vm.saveSettings()
                    }
                    SwitchRow("时间与平台名同行", settings.timeInline) {
                        vm.updateSettings(settings.copy(timeInline = it)); vm.saveSettings()
                    }
                }
            }
        }
        item {
            GroupTitle("交互反馈")
            SettingsGroupCard {
                Column {
                    SwitchRow("震动反馈", settings.hapticFeedback) {
                        vm.updateSettings(settings.copy(hapticFeedback = it)); vm.saveSettings()
                    }
                    SwitchRow("按钮音效", settings.soundFeedback) {
                        vm.updateSettings(settings.copy(soundFeedback = it)); vm.saveSettings()
                    }
                }
            }
        }
        item {
            GroupTitle("统计偏好")
            SettingsGroupCard {
                Column {
                    SwitchRow("按广告数排序", settings.sortByAds) {
                        vm.updateSettings(settings.copy(sortByAds = it)); vm.saveSettings()
                    }
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Row {
                            Text("每日存档上限", color = c.text, fontSize = 14.sp, modifier = Modifier.weight(1f))
                            Text(settings.maxUndoPerDay.toString(), color = c.textSub, fontSize = 14.sp)
                        }
                        Slider(
                            value = settings.maxUndoPerDay.toFloat(),
                            onValueChange = { vm.updateSettings(settings.copy(maxUndoPerDay = it.toInt())) },
                            onValueChangeFinished = { vm.saveSettings() },
                            valueRange = 3f..30f,
                        )
                    }
                }
            }
        }
    }
}

// ==================== 推送与连接 ====================
@Composable
private fun ConnectionSettingsPage(vm: AppViewModel, onBack: () -> Unit) {
    val settings by vm.settings.collectAsState()
    val lanRunning by vm.lanRunning.collectAsState()
    val lanPort by vm.lanPort.collectAsState()
    val lanIp by vm.lanIp.collectAsState()
    val relayState by vm.relayState.collectAsState()
    val relayErr by vm.relayLastError.collectAsState()
    val clipboard = LocalClipboardManager.current
    val c = AppColorsHolder

    SubPageScaffold(title = "推送与连接", onBack = onBack) {
        item {
            GroupTitle("局域网服务")
            SettingsGroupCard {
                Column {
                    SwitchRow("启用局域网输入", settings.lanEnabled) {
                        vm.updateSettings(settings.copy(lanEnabled = it)); vm.saveSettings(); vm.refreshLanIp()
                    }
                    if (settings.lanEnabled) {
                        val ip = lanIp
                        Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("服务状态", color = c.text, fontSize = 14.sp, modifier = Modifier.weight(1f))
                            Box(Modifier.size(8.dp).clip(CircleShape).background(if (lanRunning) Palette.green else Palette.orange))
                            Spacer(Modifier.width(8.dp))
                            Text(if (lanRunning) "运行中" else "未运行",
                                color = if (lanRunning) Palette.green else Palette.orange,
                                fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                        if (ip != null && lanRunning) {
                            val url = "http://" + ip + ":" + lanPort
                            Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("访问地址", color = c.text, fontSize = 14.sp, modifier = Modifier.weight(1f))
                                Text(url, color = Palette.blue, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Row(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                                TextButton(onClick = {
                                    clipboard.setText(AnnotatedString(url))
                                    vm.showToast("已复制地址", ToastStyle.SUCCESS)
                                }) { Text("复制地址") }
                                Spacer(Modifier.weight(1f))
                                TextButton(onClick = { vm.refreshLanIp() }) { Text("刷新") }
                            }
                        }
                        SwitchRow("显示握手提示", settings.showHandshakeToast) {
                            vm.updateSettings(settings.copy(showHandshakeToast = it)); vm.saveSettings()
                        }
                    }
                }
            }
        }
        item {
            GroupTitle("中继服务器")
            SettingsGroupCard {
                Column {
                    SwitchRow("启用中继服务器", settings.relayEnabled) {
                        vm.updateSettings(settings.copy(relayEnabled = it)); vm.saveSettings()
                    }
                    if (settings.relayEnabled) {
                        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                            OutlinedTextField(
                                value = settings.relayUrl,
                                onValueChange = { vm.updateSettings(settings.copy(relayUrl = it)) },
                                label = { Text("服务器地址（https://...）") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        val relayStatusText = when (relayState) {
                            RelayClient.State.CONNECTED -> "已连接"
                            RelayClient.State.CONNECTING -> "连接中…"
                            RelayClient.State.ERROR -> "异常"
                            else -> "未启用"
                        }
                        val relayStatusColor = when (relayState) {
                            RelayClient.State.CONNECTED -> Palette.green
                            RelayClient.State.CONNECTING -> Palette.orange
                            RelayClient.State.ERROR -> Palette.red
                            else -> c.textSub
                        }
                        Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("连接状态", color = c.text, fontSize = 14.sp, modifier = Modifier.weight(1f))
                            Box(Modifier.size(8.dp).clip(CircleShape).background(relayStatusColor))
                            Spacer(Modifier.width(8.dp))
                            Text(relayStatusText, color = relayStatusColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                        if (relayErr != null && relayState == RelayClient.State.ERROR) {
                            Text(relayErr ?: "", color = Palette.red, fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                        }
                        TextButton(
                            onClick = { vm.saveSettings(); Haptics.tap() },
                            modifier = Modifier.padding(horizontal = 8.dp),
                        ) { Text("立即重连") }
                    }
                }
            }
        }
        item {
            GroupTitle("通知")
            SettingsGroupCard {
                Column {
                    SwitchRow("收到推送时弹通知", settings.notifyOnPush) {
                        vm.updateSettings(settings.copy(notifyOnPush = it)); vm.saveSettings()
                    }
                    SwitchRow("点击后先询问再打开", settings.askBeforeOpenPush) {
                        vm.updateSettings(settings.copy(askBeforeOpenPush = it)); vm.saveSettings()
                    }
                }
            }
        }
    }
}

// ==================== 数据 ====================
@Composable
private fun DataSettingsPage(vm: AppViewModel, onBack: () -> Unit) {
    val settings by vm.settings.collectAsState()
    val days by vm.days.collectAsState()
    val c = AppColorsHolder
    val context = LocalContext.current
    var showDeleteDay by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }
    var showClearFinal by remember { mutableStateOf(false) }

    val backupImportLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                if (text != null && vm.importBackup(text)) {
                    vm.showToast("备份导入成功", ToastStyle.SUCCESS)
                } else {
                    vm.showToast("备份格式错误", ToastStyle.ERROR)
                }
            } catch (e: Exception) {
                vm.showToast("导入失败：" + e.message, ToastStyle.ERROR)
            }
        }
    }

    SubPageScaffold(title = "数据", onBack = onBack) {
        item {
            GroupTitle("字典与数据库")
            SettingsGroupCard {
                Column {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("平台字典", color = c.text, fontSize = 16.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                        Text(settings.platforms.size.toString() + " 个", color = c.textSub, fontSize = 14.sp)
                    }
                    Box(Modifier.fillMaxWidth().height(1.dp).background(dividerColor(c.isDark)))
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("记录天数", color = c.text, fontSize = 16.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                        Text(days.size.toString(), color = c.textSub, fontSize = 14.sp)
                    }
                }
            }
        }
        item {
            GroupTitle("数据管理")
            SettingsGroupCard {
                Column {
                    Row(
                        Modifier.fillMaxWidth().clickable { showDeleteDay = true; Haptics.tap() }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("删除指定日期数据", color = c.text, fontSize = 16.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                        Icon(Icons.Filled.ChevronRight, null, tint = c.textSub.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
                    }
                    Box(Modifier.fillMaxWidth().height(1.dp).background(dividerColor(c.isDark)))
                    Row(
                        Modifier.fillMaxWidth().clickable { showClearConfirm = true; Haptics.tap() }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("清空所有数据", color = Palette.red, fontSize = 16.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
        item {
            GroupTitle("数据备份")
            SettingsGroupCard {
                Column {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            val json = vm.exportBackup()
                            val file = java.io.File(context.cacheDir, "shortdrama_backup.json")
                            file.writeText(json)
                            com.shortdrama.count.util.BackupShare.shareFile(context, file, "application/json", "导出备份")
                        }, modifier = Modifier.weight(1f)) { Text("导出备份") }
                        OutlinedButton(onClick = {
                            backupImportLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
                        }, modifier = Modifier.weight(1f)) { Text("导入备份") }
                    }
                }
            }
        }
    }

    if (showDeleteDay) {
        DeleteDayDialog(vm) { showDeleteDay = false }
    }
    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            confirmButton = { TextButton(onClick = { showClearConfirm = false; showClearFinal = true }) { Text("继续", color = Palette.red) } },
            dismissButton = { TextButton(onClick = { showClearConfirm = false }) { Text("取消") } },
            title = { Text("确认清空？") },
            text = { Text("将删除全部短剧记录、平台记录、历史存档。此操作无法恢复。") },
        )
    }
    if (showClearFinal) {
        AlertDialog(
            onDismissRequest = { showClearFinal = false },
            confirmButton = {
                TextButton(onClick = {
                    vm.clearAllData()
                    vm.showToast("已清空所有数据", ToastStyle.SUCCESS)
                    showClearFinal = false
                }) { Text("彻底清空", color = Palette.red) }
            },
            dismissButton = { TextButton(onClick = { showClearFinal = false }) { Text("取消") } },
            title = { Text("最后确认") },
            text = { Text("真的要清空所有数据吗？删除后无法撤销。") },
        )
    }
}

// ==================== 软件更新 ====================
@Composable
private fun UpdateSettingsPage(vm: AppViewModel, onBack: () -> Unit) {
    val settings by vm.settings.collectAsState()
    val lastMessage by vm.lastMessage.collectAsState()
    val c = AppColorsHolder

    SubPageScaffold(title = "软件更新", onBack = onBack) {
        item {
            GroupTitle("软件更新")
            SettingsGroupCard {
                Column {
                    Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("当前版本", color = c.text, fontSize = 16.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                        Text("v" + AppVersion.full, color = c.textSub, fontSize = 13.sp)
                    }
                    SwitchRow("自动检查更新", settings.autoUpdateCheck) {
                        vm.updateSettings(settings.copy(autoUpdateCheck = it)); vm.saveSettings()
                    }
                    SwitchRow("静默下载", settings.silentDownload) {
                        vm.updateSettings(settings.copy(silentDownload = it)); vm.saveSettings()
                    }
                    SwitchRow("自动提示安装", settings.autoPromptInstall) {
                        vm.updateSettings(settings.copy(autoPromptInstall = it)); vm.saveSettings()
                    }
                    Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { vm.checkForUpdate(silent = false, force = true) }, modifier = Modifier.weight(1f)) {
                            Text("检查更新")
                        }
                        OutlinedButton(onClick = { vm.testCustomProxy(settings.customUpdateProxy) }, modifier = Modifier.weight(1f)) {
                            Text("测试代理")
                        }
                    }
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        OutlinedTextField(
                            value = settings.customUpdateProxy,
                            onValueChange = { vm.updateSettings(settings.copy(customUpdateProxy = it)) },
                            label = { Text("自定义代理（可选）") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    lastMessage?.let {
                        Text(it, color = c.textSub, fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                    }
                }
            }
        }
        item {
            GroupTitle("关于")
            SettingsGroupCard {
                Column(Modifier.padding(16.dp)) {
                    Text("开饭了 · 短剧计数（Android）", color = c.text, fontSize = 14.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("版本 " + AppVersion.full, color = c.textSub, fontSize = 12.sp)
                }
            }
        }
    }
}

// ==================== 通用脚手架 ====================
@Composable
private fun SubPageScaffold(
    title: String,
    onBack: () -> Unit,
    content: LazyListScope.() -> Unit,
) {
    val c = AppColorsHolder
    Column(Modifier.fillMaxSize().background(c.bg)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { onBack(); Haptics.tap() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Palette.blue)
            }
            Text(title, color = c.text, fontSize = 20.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f))
        }
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            content()
            item { Spacer(Modifier.height(40.dp)) }
        }
    }
}

@Composable
private fun GroupTitle(text: String) {
    Text(text, color = AppColorsHolder.textSub, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
}

@Composable
private fun SwitchRow(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, color = AppColorsHolder.text, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = { onCheckedChange(it); Haptics.tap() })
    }
}

private fun dividerColor(isDark: Boolean): Color =
    if (isDark) Color(0x33FFFFFF) else Color(0x22000000)

// ==================== 删除指定日期对话框 ====================
@Composable
private fun DeleteDayDialog(vm: AppViewModel, onDismiss: () -> Unit) {
    val c = AppColorsHolder
    val days by vm.days.collectAsState()
    var pickedDate by remember { mutableStateOf(java.util.Date()) }
    var showFirst by remember { mutableStateOf(false) }
    var showFinal by remember { mutableStateOf(false) }
    var showPicker by remember { mutableStateOf(false) }

    val pickedStr = AppConstants.dateString(pickedDate)
    val day = days[pickedStr]
    val dramaCount = day?.dramas?.size ?: 0
    val recordCount = day?.records?.size ?: 0
    val hasData = dramaCount > 0 || recordCount > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                if (!hasData) {
                    vm.showToast("该日期没有数据", ToastStyle.ERROR)
                    return@TextButton
                }
                showFirst = true
            }) { Text("删除", color = Palette.red) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
        title = { Text("删除指定日期数据") },
        text = {
            Column {
                OutlinedButton(onClick = { showPicker = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(pickedStr)
                }
                Spacer(Modifier.height(10.dp))
                Row {
                    Text("该日短剧", color = c.textSub, fontSize = 13.sp, modifier = Modifier.weight(1f))
                    Text(dramaCount.toString() + " 部", color = c.text, fontSize = 13.sp)
                }
                Spacer(Modifier.height(4.dp))
                Row {
                    Text("该日记录", color = c.textSub, fontSize = 13.sp, modifier = Modifier.weight(1f))
                    Text(recordCount.toString() + " 条", color = c.text, fontSize = 13.sp)
                }
            }
        },
    )

    if (showPicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = pickedDate.time)
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { pickedDate = java.util.Date(it) }
                    showPicker = false
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text("取消") } },
        ) { DatePicker(state = state) }
    }

    if (showFirst) {
        AlertDialog(
            onDismissRequest = { showFirst = false },
            confirmButton = { TextButton(onClick = { showFirst = false; showFinal = true }) { Text("继续", color = Palette.red) } },
            dismissButton = { TextButton(onClick = { showFirst = false }) { Text("取消") } },
            title = { Text("确认删除？") },
            text = { Text("将删除 " + pickedStr + " 的全部短剧与平台记录。此操作无法恢复。") },
        )
    }

    if (showFinal) {
        AlertDialog(
            onDismissRequest = { showFinal = false },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteDay(pickedStr)
                    vm.showToast("已删除 " + pickedStr + " 的数据", ToastStyle.SUCCESS)
                    showFinal = false
                    onDismiss()
                }) { Text("彻底删除", color = Palette.red) }
            },
            dismissButton = { TextButton(onClick = { showFinal = false }) { Text("取消") } },
            title = { Text("最后确认") },
            text = { Text("真的要删除 " + pickedStr + " 的数据吗？删除后无法撤销。") },
        )
    }
}
