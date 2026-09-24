package com.shortdrama.count.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shortdrama.count.ui.theme.AppColorsHolder
import com.shortdrama.count.ui.theme.Palette
import com.shortdrama.count.util.AppConstants
import com.shortdrama.count.viewmodel.AppViewModel

@Composable
fun PushDevicePickerHost(vm: AppViewModel) {
    val show by vm.showDevicePicker.collectAsState()
    val devices by vm.pushDevices.collectAsState()
    if (!show) return
    AlertDialog(
        onDismissRequest = { vm.dismissDevicePicker() },
        confirmButton = {
            TextButton(onClick = {
                vm.pushCurrentDateTo(devices)
                vm.dismissDevicePicker()
            }) { Text("全部推送") }
        },
        dismissButton = { TextButton(onClick = { vm.dismissDevicePicker() }) { Text("取消") } },
        title = { Text("选择推送设备") },
        text = {
            LazyColumn(Modifier.heightIn(max = 280.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(devices, key = { it.deviceId + it.ip }) { dev ->
                    Row(
                        Modifier.fillMaxWidth().clickable {
                            vm.pushCurrentDateTo(listOf(dev))
                            vm.dismissDevicePicker()
                        }.padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(dev.name, color = AppColorsHolder.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text(dev.ip + ":" + dev.port, color = AppColorsHolder.textSub, fontSize = 12.sp)
                        }
                    }
                }
            }
        },
    )
}

@Composable
fun UpdateDialogHost(vm: AppViewModel) {
    val show by vm.showUpdateAlert.collectAsState()
    val release by vm.pendingRelease.collectAsState()
    val downloadStatus by vm.downloadStatus.collectAsState()
    val progress by vm.downloadProgress.collectAsState()
    if (!show || release == null) return
    val r = release!!
    AlertDialog(
        onDismissRequest = { vm.dismissUpdateAlert() },
        confirmButton = {
            when (downloadStatus) {
                com.shortdrama.count.model.DownloadStatus.READY -> {
                    TextButton(onClick = {
                        val pd = vm.pendingDownload.value
                        if (pd != null) vm.installApk(pd.localPath)
                        vm.dismissUpdateAlert()
                    }) { Text("立即安装") }
                }
                com.shortdrama.count.model.DownloadStatus.DOWNLOADING -> {
                    TextButton(onClick = { vm.dismissUpdateAlert() }) { Text("后台下载") }
                }
                else -> {
                    TextButton(onClick = { vm.downloadLatest() }) { Text("下载更新") }
                }
            }
        },
        dismissButton = { TextButton(onClick = { vm.postponeUpdate() }) { Text("稍后") } },
        title = { Text("发现新版本 v" + r.version) },
        text = {
            Column {
                if (downloadStatus == com.shortdrama.count.model.DownloadStatus.DOWNLOADING) {
                    LinearProgressIndicator(progress = { progress.toFloat() }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(6.dp))
                    Text("下载中 " + (progress * 100).toInt() + "%", color = AppColorsHolder.textSub, fontSize = 12.sp)
                } else {
                    Text(r.releaseNotes.ifEmpty { "包含功能更新与修复" }, color = AppColorsHolder.textSub, fontSize = 13.sp)
                }
            }
        },
    )
}
