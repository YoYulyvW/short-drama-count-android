package com.shortdrama.count.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shortdrama.count.ui.detail.DetailScreen
import com.shortdrama.count.ui.home.HomeScreen
import com.shortdrama.count.ui.month.MonthStatsScreen
import com.shortdrama.count.ui.settings.SettingsScreen
import com.shortdrama.count.ui.sheets.SheetHost
import com.shortdrama.count.ui.theme.AppColorsHolder
import com.shortdrama.count.ui.theme.Palette
import com.shortdrama.count.viewmodel.AppViewModel

@Composable
fun MainScreen(vm: AppViewModel) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val c = AppColorsHolder
    val tabs = listOf(
        "首页" to Icons.Filled.Home,
        "详情" to Icons.Filled.List,
        "月统计" to Icons.Filled.BarChart,
        "设置" to Icons.Filled.Settings,
    )
    Scaffold(
        containerColor = c.bg,
        bottomBar = {
            NavigationBar(
                containerColor = c.card,
                tonalElevation = 0.dp,
            ) {
                tabs.forEachIndexed { index, (label, icon) ->
                    NavigationBarItem(
                        selected = tab == index,
                        onClick = { tab = index },
                        icon = { Icon(icon, contentDescription = label) },
                        label = { Text(label, fontSize = 11.sp) },
                        alwaysShowLabel = true,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Palette.blue,
                            selectedTextColor = Palette.blue,
                            indicatorColor = Palette.blue.copy(alpha = 0.14f),
                            unselectedIconColor = c.textSub,
                            unselectedTextColor = c.textSub,
                        ),
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (tab) {
                0 -> HomeScreen(vm)
                1 -> DetailScreen(vm)
                2 -> MonthStatsScreen(vm)
                else -> SettingsScreen(vm)
            }
            // 弹窗渲染在内容区内，天然被底栏约束
            SheetHost(vm)
        }
    }
    PushDevicePickerHost(vm)
    UpdateDialogHost(vm)
    ToastHost(vm)
}
