package com.shortdrama.count

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Surface
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shortdrama.count.model.PushPayload
import com.shortdrama.count.service.NotificationHelper
import com.shortdrama.count.service.PushIntentBus
import com.shortdrama.count.ui.MainScreen
import com.shortdrama.count.ui.theme.ShortDramaCountTheme
import com.shortdrama.count.viewmodel.AppViewModel
import kotlinx.serialization.json.Json

class MainActivity : ComponentActivity() {

    private val json = Json { ignoreUnknownKeys = true }

    private val notifPermLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* 用户是否授予都继续 */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 请求通知权限（Android 13+）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // 处理通知点击携带的 payload
        handlePushIntent(intent)

        setContent {
            ShortDramaCountTheme {
                Surface {
                    val vm: AppViewModel = viewModel()
                    MainScreen(vm)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handlePushIntent(intent)
    }

    private fun handlePushIntent(intent: Intent?) {
        val str = intent?.getStringExtra(NotificationHelper.EXTRA_PUSH_PAYLOAD) ?: return
        if (str.isEmpty()) return
        try {
            val payload = json.decodeFromString(PushPayload.serializer(), str)
            val fromRelay = intent.getBooleanExtra(NotificationHelper.EXTRA_PUSH_FROM_RELAY, false)
            PushIntentBus.post(payload, fromRelay)
        } catch (_: Exception) {}
        intent.removeExtra(NotificationHelper.EXTRA_PUSH_PAYLOAD)
    }
}
