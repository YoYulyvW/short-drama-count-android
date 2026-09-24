package com.shortdrama.count.ui.sheets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shortdrama.count.model.ActiveSheet
import com.shortdrama.count.model.ShareItem
import com.shortdrama.count.ui.theme.AppColorsHolder
import com.shortdrama.count.ui.theme.Palette
import com.shortdrama.count.util.AppConstants
import com.shortdrama.count.util.Haptics
import com.shortdrama.count.util.ImageShare
import com.shortdrama.count.util.ShareCode
import com.shortdrama.count.service.ExportImageService
import com.shortdrama.count.viewmodel.AppViewModel
import com.shortdrama.count.viewmodel.ToastStyle

@Composable
fun BoxScope.SheetHost(vm: AppViewModel) {
    val sheet by vm.activeSheet.collectAsState()
    val visible = sheet != null

    var renderSheet by remember { mutableStateOf<ActiveSheet?>(null) }
    LaunchedEffect(sheet) { if (sheet != null) renderSheet = sheet }

    val current = renderSheet ?: return

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier.matchParentSize(),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.42f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { vm.setActiveSheet(null) }
        )
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it }),
        modifier = Modifier.align(Alignment.BottomCenter),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.92f),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            color = AppColorsHolder.bg,
            tonalElevation = 8.dp,
        ) {
            Column(Modifier.fillMaxWidth()) {
                Box(
                    Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier.size(width = 40.dp, height = 4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(AppColorsHolder.textSub.copy(alpha = 0.3f))
                    )
                }
                Box(Modifier.fillMaxWidth().weight(1f)) {
                    when (current) {
                        ActiveSheet.IMPORT_DATA -> ImportSheet(vm)
                        ActiveSheet.EXPORT_TEXT -> ExportTextSheet(vm)
                        ActiveSheet.EXPORT_CODE -> ExportCodeSheet(vm)
                        ActiveSheet.EXPORT_IMAGE -> ExportImageSheet(vm)
                        ActiveSheet.UNDO -> UndoSheet(vm)
                        ActiveSheet.QUICK_TOOLS -> QuickToolsSheet(vm)
                        else -> {}
                    }
                }
            }
        }
    }
}

fun Modifier.sheetContentPadding(): Modifier = this.padding(bottom = 20.dp)

@Composable
private fun ImportSheet(vm: AppViewModel) {
    val c = AppColorsHolder
    val clipboard = LocalClipboardManager.current
    var text by remember { mutableStateOf("") }
    val pending by vm.pendingImportText.collectAsState()
    LaunchedEffect(pending) { if (pending != null) text = pending!! }

    var previewItems by remember { mutableStateOf<List<ShareItem>?>(null) }
    var badLines by remember { mutableStateOf<List<String>>(emptyList()) }

    val items = previewItems
    if (items != null) {
        ImportPreview(
            vm = vm,
            initialItems = items,
            bad = badLines,
            onBack = { previewItems = null },
            onDone = { vm.setActiveSheet(null) },
        )
        return
    }

    Column(
        Modifier.fillMaxSize().imePadding().sheetContentPadding().padding(horizontal = 20.dp),
    ) {
        Text("导入数据", color = c.text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("粘贴内容", color = c.textSub, fontSize = 13.sp)
            Spacer(Modifier.weight(1f))
            if (text.isNotEmpty()) {
                TextButton(onClick = { text = ""; Haptics.tap() }) {
                    Text("清空", color = Palette.red, fontSize = 13.sp)
                }
            }
            TextButton(onClick = {
                val clip = clipboard.getText()?.text ?: ""
                if (clip.isEmpty()) {
                    vm.showToast("剪贴板为空", ToastStyle.ERROR)
                } else {
                    text = if (text.isEmpty()) clip else text + "\n" + clip
                    Haptics.tap()
                }
            }) {
                Icon(Icons.Filled.ContentPaste, null, tint = Palette.blue, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("粘贴", color = Palette.blue, fontSize = 13.sp)
            }
        }
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = text, onValueChange = { text = it },
            placeholder = { Text("【剧名】易:2 | 懂:3  /  DCT1密文 / CHEN分享码", fontSize = 12.sp) },
            modifier = Modifier.fillMaxWidth().weight(1f),
        )
        Spacer(Modifier.height(10.dp))
        Text("支持：【剧名】易:2 | 懂:3、【剧名】、DCT1 密文、CHEN 分享码",
            color = c.textSub, fontSize = 11.sp)
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = {
                val trimmed = text.trim()
                if (trimmed.isEmpty()) return@Button
                val result = parseInput(vm, trimmed)
                if (result == null) {
                    vm.showToast("解析失败或格式错误", ToastStyle.ERROR); Haptics.warning()
                } else if (result.first.isEmpty()) {
                    var msg = "未识别到有效数据"
                    if (result.second.isNotEmpty()) {
                        msg = msg + "\n" + result.second.take(3).joinToString("\n") { it.trim().take(30) }
                    }
                    vm.showToast(msg, ToastStyle.ERROR); Haptics.warning()
                } else {
                    previewItems = result.first
                    badLines = result.second
                    Haptics.success()
                }
            },
            enabled = text.trim().isNotEmpty(),
            modifier = Modifier.fillMaxWidth().height(50.dp),
        ) {
            Text("解析", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

private fun parseInput(vm: AppViewModel, raw: String): Pair<List<ShareItem>, List<String>>? {
    return try {
        if (raw.startsWith(AppConstants.sharePrefix)) {
            val unpacked = ShareCode.unpack(ShareCode.decrypt(raw))
            val order = unpacked.first
            val groups = unpacked.second
            val items = order.mapNotNull { pair ->
                val title = pair.first
                val fast = pair.second
                val pl = groups[title + "|" + fast] ?: emptyMap()
                if (pl.isEmpty()) null else ShareItem(title, fast, pl.toMutableMap())
            }
            items to emptyList()
        } else {
            var body = raw
            if (raw.startsWith(AppConstants.encPrefix)) {
                body = ShareCode.decryptDCT1(raw)
            }
            vm.parseImportText(body)
        }
    } catch (e: Exception) {
        null
    }
}

@Composable
private fun ImportPreview(
    vm: AppViewModel,
    initialItems: List<ShareItem>,
    bad: List<String>,
    onBack: () -> Unit,
    onDone: () -> Unit,
) {
    val c = AppColorsHolder
    val date = vm.currentDateString
    var editable by remember { mutableStateOf(initialItems) }
    var dedup by remember { mutableStateOf(true) }

    var totalAds = 0
    for (it in editable) totalAds += it.platforms.values.sum()

    Column(
        Modifier.fillMaxSize().sheetContentPadding().padding(horizontal = 20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("确认导入", color = c.text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onBack) { Text("返回编辑") }
        }
        Spacer(Modifier.height(6.dp))
        Text("目标日期：" + date + "  ·  共 " + editable.size + " 部 · " + totalAds + " 条",
            color = c.textSub, fontSize = 12.sp)
        Spacer(Modifier.height(10.dp))

        if (editable.isEmpty()) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text("已清空所有待导入项", color = c.textSub, fontSize = 13.sp)
            }
        } else {
            LazyColumn(
                Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(editable, key = { it.title + "|" + it.isFast }) { item ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                            .background(c.card).padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(item.title, color = c.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                if (item.isFast) {
                                    Spacer(Modifier.width(6.dp))
                                    Text("极速", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold,
                                        modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Palette.blue)
                                            .padding(horizontal = 6.dp, vertical = 1.dp))
                                }
                            }
                            Spacer(Modifier.height(2.dp))
                            Text(
                                if (item.platforms.isEmpty()) "（无平台数据）"
                                else item.platforms.entries.sortedByDescending { it.value }
                                    .joinToString("  ") { it.key.take(1) + ":" + it.value },
                                color = c.textSub, fontSize = 12.sp,
                            )
                        }
                        IconButton(onClick = {
                            editable = editable.filterNot { it.title == item.title && it.isFast == item.isFast }
                            Haptics.tap()
                        }) {
                            Icon(Icons.Filled.Delete, null, tint = Palette.red, modifier = Modifier.size(18.dp))
                        }
                    }
                }
                if (bad.isNotEmpty()) {
                    item {
                        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                            .background(c.card).padding(12.dp)) {
                            Text("已忽略 " + bad.size + " 行", color = c.textSub, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            bad.take(5).forEach { line ->
                                Text(line.trim().take(40), color = c.textSub, fontSize = 11.sp)
                            }
                            if (bad.size > 5) Text("... 还有 " + (bad.size - 5) + " 行", color = c.textSub, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("按剧名去重", color = c.text, fontSize = 14.sp, modifier = Modifier.weight(1f))
            Switch(checked = dedup, onCheckedChange = { dedup = it; Haptics.tap() })
        }
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = {
                val result = vm.importItems(date, editable, dedup)
                vm.showToast("导入 " + result.first + " 部 · 跳过 " + result.second + " 部", ToastStyle.SUCCESS)
                Haptics.success()
                onDone()
            },
            enabled = editable.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().height(50.dp),
        ) { Text("确认导入", fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
    }
}

@Composable
private fun ExportTextSheet(vm: AppViewModel) {
    val c = AppColorsHolder
    val clipboard = LocalClipboardManager.current
    val date = vm.currentDateString
    val text = vm.exportText(date)
    Column(
        Modifier.fillMaxSize().sheetContentPadding().padding(horizontal = 20.dp),
    ) {
        Text("明文导出", color = c.text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        if (text.isEmpty()) {
            Text("当前日期没有数据", color = c.textSub, fontSize = 13.sp)
        } else {
            Box(
                Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(12.dp))
                    .background(c.card).padding(12.dp).verticalScroll(rememberScrollState())
            ) {
                Text(text, color = c.text, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
            }
            Spacer(Modifier.height(12.dp))
            Button(onClick = {
                clipboard.setText(AnnotatedString(text))
                vm.showToast("已复制到剪贴板", ToastStyle.SUCCESS); Haptics.success()
                vm.setActiveSheet(null)
            }, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                Icon(Icons.Filled.ContentCopy, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp)); Text("复制")
            }
        }
    }
}

@Composable
private fun ExportCodeSheet(vm: AppViewModel) {
    val c = AppColorsHolder
    val clipboard = LocalClipboardManager.current
    val date = vm.currentDateString
    val code = remember(date) { vm.exportShareCode(date) }
    Column(
        Modifier.fillMaxSize().sheetContentPadding().padding(horizontal = 20.dp),
    ) {
        Text("密文导出", color = c.text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text("CHEN 分享码（PBKDF2 + HMAC + zlib）", color = c.textSub, fontSize = 12.sp)
        Spacer(Modifier.height(12.dp))
        if (code.isEmpty()) {
            Text("当前日期没有数据", color = c.textSub, fontSize = 13.sp)
        } else {
            Box(
                Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(12.dp))
                    .background(c.card).padding(12.dp).verticalScroll(rememberScrollState())
            ) {
                Text(code, color = c.text, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            }
            Spacer(Modifier.height(12.dp))
            Button(onClick = {
                clipboard.setText(AnnotatedString(code))
                vm.showToast("已复制分享码", ToastStyle.SUCCESS); Haptics.success()
                vm.setActiveSheet(null)
            }, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                Icon(Icons.Filled.ContentCopy, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp)); Text("复制")
            }
        }
    }
}

@Composable
private fun ExportImageSheet(vm: AppViewModel) {
    val c = AppColorsHolder
    val context = LocalContext.current
    val date = vm.currentDateString
    val days by vm.days.collectAsState()
    val day = days[date]

    Column(
        Modifier.fillMaxSize().sheetContentPadding().padding(horizontal = 20.dp),
    ) {
        Text("导出图片", color = c.text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        if (day == null || day.dramas.isEmpty()) {
            Text("当前日期没有数据", color = c.textSub, fontSize = 13.sp)
        } else {
            Button(onClick = {
                try {
                    val bmp = ExportImageService.render(date, day)
                    if (bmp == null) {
                        vm.showToast("生成失败：无有效数据", ToastStyle.ERROR); return@Button
                    }
                    val name = "drama_export_" + System.currentTimeMillis() + ".png"
                    val uri = ImageShare.saveToGallery(context, bmp, name)
                    if (uri == null) {
                        vm.showToast("保存失败，请检查存储权限", ToastStyle.ERROR); return@Button
                    }
                    val shared = ImageShare.shareUri(context, uri)
                    vm.showToast(if (shared) "✅ 已保存到相册并分享" else "✅ 已保存到相册", ToastStyle.SUCCESS)
                    Haptics.success()
                    vm.setActiveSheet(null)
                } catch (e: Exception) {
                    vm.showToast("生成失败：" + (e.message ?: "未知错误"), ToastStyle.ERROR)
                }
            }, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                Text("生成并分享")
            }
        }
    }
}

@Composable
private fun UndoSheet(vm: AppViewModel) {
    val c = AppColorsHolder
    val date = vm.currentDateString
    val snapshots = remember(date) { vm.undosFor(date) }
    Column(
        Modifier.fillMaxSize().sheetContentPadding().padding(horizontal = 20.dp),
    ) {
        Text("回档", color = c.text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text("恢复 " + date + " 的历史快照", color = c.textSub, fontSize = 12.sp)
        Spacer(Modifier.height(12.dp))
        if (snapshots.isEmpty()) {
            Text("暂无快照", color = c.textSub, fontSize = 13.sp)
        } else {
            LazyColumn(Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(snapshots, key = { it.id }) { snap ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.card).padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(snap.createdAt, color = c.text, fontSize = 13.sp)
                            Text("短剧 " + snap.data.dramas.size + " 部", color = c.textSub, fontSize = 11.sp)
                        }
                        TextButton(onClick = {
                            vm.restore(snap); vm.showToast("已恢复快照", ToastStyle.SUCCESS); Haptics.success()
                            vm.setActiveSheet(null)
                        }) { Text("恢复") }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickToolsSheet(vm: AppViewModel) {
    val c = AppColorsHolder
    Column(
        Modifier.fillMaxSize().sheetContentPadding().padding(horizontal = 20.dp),
    ) {
        Text("快捷工具", color = c.text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Text("从首页工具行进入各功能", color = c.textSub, fontSize = 13.sp)
    }
}
