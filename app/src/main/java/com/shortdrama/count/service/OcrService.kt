package com.shortdrama.count.service

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

object OcrService {
    private val recognizer by lazy {
        TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
    }

    suspend fun recognize(bitmap: Bitmap): String = suspendCancellableCoroutine { cont ->
        val image = InputImage.fromBitmap(bitmap, 0)
        recognizer.process(image)
            .addOnSuccessListener { text ->
                // 按 y（行），再按 x 排序后拼接
                val items = text.textBlocks
                    .flatMap { it.lines }
                    .map { line ->
                        val box = line.boundingBox
                        Triple(box?.top ?: 0, box?.left ?: 0, line.text)
                    }
                    .sortedWith(compareBy({ it.first }, { it.second }))
                val out = items.joinToString("\n") { it.third }
                if (cont.isActive) cont.resume(out)
            }
            .addOnFailureListener {
                if (cont.isActive) cont.resume("")
            }
    }
}
