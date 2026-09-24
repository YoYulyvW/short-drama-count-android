package com.shortdrama.count.ui.home

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.shortdrama.count.service.OcrService
import com.shortdrama.count.viewmodel.AppViewModel
import com.shortdrama.count.viewmodel.ToastStyle
import kotlinx.coroutines.launch

@Composable
fun OcrDialog(vm: AppViewModel, onDismiss: (String?) -> Unit) {
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var recognized by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var launched by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bmp ->
        if (bmp != null) {
            bitmap = bmp
            loading = true
            scope.launch {
                recognized = try { OcrService.recognize(bmp) } catch (e: Exception) { "" }
                loading = false
            }
        } else {
            onDismiss(null)
        }
    }

    LaunchedEffect(Unit) {
        if (!launched) { launched = true; cameraLauncher.launch(null) }
    }

    AlertDialog(
        onDismissRequest = { onDismiss(null) },
        confirmButton = {
            TextButton(onClick = {
                if (recognized.isBlank()) {
                    vm.showToast("未识别到文字", ToastStyle.ERROR)
                    onDismiss(null)
                } else onDismiss(recognized)
            }) { Text("使用") }
        },
        dismissButton = { TextButton(onClick = { onDismiss(null) }) { Text("取消") } },
        title = { Text("OCR 识别") },
        text = {
            Column(Modifier.fillMaxWidth()) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap!!.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxWidth().height(180.dp),
                    )
                    Spacer(Modifier.height(8.dp))
                }
                if (loading) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("识别中…")
                    }
                } else {
                    OutlinedTextField(
                        value = recognized, onValueChange = { recognized = it },
                        label = { Text("识别结果（可编辑）") },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp),
                    )
                }
            }
        },
    )
}
