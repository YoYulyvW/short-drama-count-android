package com.shortdrama.count.ui.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shortdrama.count.model.ActiveSheet
import com.shortdrama.count.ui.theme.AppColorsHolder
import com.shortdrama.count.util.AppConstants
import com.shortdrama.count.util.Haptics
import com.shortdrama.count.util.ImageShare
import com.shortdrama.count.service.ExportImageService
import com.shortdrama.count.viewmodel.AppViewModel
import com.shortdrama.count.viewmodel.ToastStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SheetHost(vm: AppViewModel) {
    val sheet by vm.activeSheet.collectAsState()
    if (sheet == null) return
    ModalBottomSheet(
        onDismissRequest = { vm.setActiveSheet(null) },
        containerColor = AppColorsHolder.bg,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
    ) {
        when (sheet) {
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

/**
 * 读取系统导航栏真实高度（px）。
 * ModalBottomSheet 内的 WindowInsets 常常为 0，这里从 Activity decorView 直接读取，
 * 并监听变化，保证任何状态下都能拿到真实高度。
 */
@Composable
fun rememberNavigationBarHeightPx(): Int {
    val view = androidx.compose.ui.platform.LocalView.current
    var bottomPx by androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableIntStateOf(
            androidx.core.view.ViewCompat.getRootWindowInsets(view)
                ?.getInsets(androidx.core.view.WindowInsetsCompat.Type.navigationBars())?.bottom ?: 0
        )
    }
    androidx.compose.runtime.DisposableEffect(view) {
        val root = view.rootView
        val listener = android.view.View.OnApplyWindowInsetsListener { _, insets ->
            bottomPx = insets.getInsets(android.view.WindowInsets.Type.navigationBars()).bottom
            insets
        }
        root.setOnApplyWindowInsetsListener(listener)
        root.requestApplyInsets()
        onDispose {
            root.setOnApplyWindowInsetsListener(null)
        }
    }
    return bottomPx
}

/** 内容统一底部内边距 + 高度限制，确保底部按钮可见 */
@Composable
fun Modifier.sheetContentPadding(): Modifier {
    val density = androidx.compose.ui.platform.LocalDensity.current
    val bottomPx = rememberNavigationBarHeightPx()
    val bottomDp = with(density) { bottomPx.toDp() }
    return this.padding(bottom = bottomDp + 24.dp)
}

@Composable
private fun ImportSheet(vm: AppViewModel) {
    val c = AppColorsHolder
    var text by remember { mutableStateOf("") }
    val pending by vm.pendingImportText.collectAsState()
    var dedup by remember { mutableStateOf(true) }
    LaunchedEffect(pending) { if (pending != null) text = pending!! }
    val date = vm.currentDateString

    Column(
        Modifier.fillMaxWidth()
            .heightIn(max = 620.dp)
            .verticalScroll(rememberScrollState())
            .sheetContentPadding().padding(horizontal = 20.dp, vertical = 8.dp),
    ) {
        Text("导入数据", color = c.text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text("支持明文、CHEN 分享码、DCT1 密文", color = c.textSub, fontSize = 12.sp)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = text, onValueChange = { text = it },
            label = { Text("粘贴导入内容") },
            modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
        )
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = dedup, onCheckedChange = { dedup = it })
            Text("去重（跳过已存在剧名）", color = c.textSub, fontSize = 13.sp)
        }
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = {
                if (text.isBlank()) { vm.showToast("请输入内容", ToastStyle.ERROR); return@Button }
                val trimmed = text.trim()
                val result = if (trimmed.startsWith(AppConstants.sharePrefix) || trimmed.startsWith(AppConstants.encPrefix))
                    vm.importShareCode(trimmed, date, dedup)
                else {
                    val (items, _) = vm.parseImportText(trimmed)
                    if (items.isEmpty()) null else vm.importItems(date, items, dedup)
                }
                if (result == null) { vm.showToast("解析失败或格式错误", ToastStyle.ERROR); Haptics.warning() }
                else {
                    vm.showToast("✅ 导入 " + result.first + " 条，跳过 " + result.second + " 条", ToastStyle.SUCCESS)
                    vm.consumePendingImport(); vm.setActiveSheet(null); Haptics.success()
                }
            },
            modifier = Modifier.fillMaxWidth().height(48.dp),
        ) { Text("导入到 " + date) }
    }
}

@Composable
private fun ExportTextSheet(vm: AppViewModel) {
    val c = AppColorsHolder
    val clipboard = LocalClipboardManager.current
    val date = vm.currentDateString
    val text = vm.exportText(date)
    Column(
        Modifier.fillMaxWidth().heightIn(max = 620.dp)
            .verticalScroll(rememberScrollState())
            .sheetContentPadding().padding(horizontal = 20.dp, vertical = 8.dp),
    ) {
        Text("明文导出", color = c.text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        if (text.isEmpty()) {
            Text("当前日期没有数据", color = c.textSub, fontSize = 13.sp)
        } else {
            Box(
                Modifier.fillMaxWidth().heightIn(max = 300.dp).clip(RoundedCornerShape(12.dp))
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
        Modifier.fillMaxWidth().heightIn(max = 620.dp)
            .verticalScroll(rememberScrollState())
            .sheetContentPadding().padding(horizontal = 20.dp, vertical = 8.dp),
    ) {
        Text("密文导出", color = c.text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text("CHEN 分享码（PBKDF2 + HMAC + zlib）", color = c.textSub, fontSize = 12.sp)
        Spacer(Modifier.height(12.dp))
        if (code.isEmpty()) {
            Text("当前日期没有数据", color = c.textSub, fontSize = 13.sp)
        } else {
            Box(
                Modifier.fillMaxWidth().heightIn(max = 200.dp).clip(RoundedCornerShape(12.dp))
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
        Modifier.fillMaxWidth().sheetContentPadding().padding(horizontal = 20.dp, vertical = 8.dp),
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
                    if (shared) {
                        vm.showToast("✅ 已保存到相册并分享", ToastStyle.SUCCESS)
                        Haptics.success()
                    } else {
                        vm.showToast("✅ 已保存到相册", ToastStyle.SUCCESS)
                        Haptics.success()
                    }
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
        Modifier.fillMaxWidth().sheetContentPadding().padding(horizontal = 20.dp, vertical = 8.dp),
    ) {
        Text("回档", color = c.text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text("恢复 " + date + " 的历史快照", color = c.textSub, fontSize = 12.sp)
        Spacer(Modifier.height(12.dp))
        if (snapshots.isEmpty()) {
            Text("暂无快照", color = c.textSub, fontSize = 13.sp)
        } else {
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 300.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
        Modifier.fillMaxWidth().sheetContentPadding().padding(horizontal = 20.dp, vertical = 8.dp),
    ) {
        Text("快捷工具", color = c.text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Text("从首页工具行进入各功能", color = c.textSub, fontSize = 13.sp)
    }
}
