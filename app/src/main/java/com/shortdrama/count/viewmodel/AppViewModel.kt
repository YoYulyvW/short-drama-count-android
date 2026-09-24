package com.shortdrama.count.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shortdrama.count.data.LocalDataSource
import com.shortdrama.count.model.*
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import com.shortdrama.count.service.DeviceDiscovery
import com.shortdrama.count.service.LanServer
import com.shortdrama.count.service.UpdateChecker
import com.shortdrama.count.service.UpdateDownloader
import com.shortdrama.count.util.AppConstants
import com.shortdrama.count.util.Haptics
import com.shortdrama.count.util.ShareCode
import com.shortdrama.count.util.ShareCodeException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Date

class AppViewModel : ViewModel() {
    private val source = LocalDataSource()

    private val _days = MutableStateFlow<Map<String, DayData>>(emptyMap())
    val days: StateFlow<Map<String, DayData>> = _days.asStateFlow()

    private val _undos = MutableStateFlow<List<UndoSnapshot>>(emptyList())
    val undos: StateFlow<List<UndoSnapshot>> = _undos.asStateFlow()

    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private val _isLoaded = MutableStateFlow(false)
    val isLoaded: StateFlow<Boolean> = _isLoaded.asStateFlow()

    private val _currentDate = MutableStateFlow(Date())
    val currentDate: StateFlow<Date> = _currentDate.asStateFlow()

    private val _activeSheet = MutableStateFlow<ActiveSheet?>(null)
    val activeSheet: StateFlow<ActiveSheet?> = _activeSheet.asStateFlow()

    // 更新相关
    private val _checkingUpdate = MutableStateFlow(false)
    val checkingUpdate = _checkingUpdate.asStateFlow()
    private val _pendingRelease = MutableStateFlow<ReleaseInfo?>(null)
    val pendingRelease = _pendingRelease.asStateFlow()
    private val _showUpdateAlert = MutableStateFlow(false)
    val showUpdateAlert = _showUpdateAlert.asStateFlow()
    private val _lastMessage = MutableStateFlow<String?>(null)
    val lastMessage = _lastMessage.asStateFlow()
    private val _installing = MutableStateFlow(false)
    val installing = _installing.asStateFlow()
    private val _pendingDownload = MutableStateFlow<PendingDownload?>(null)
    val pendingDownload = _pendingDownload.asStateFlow()
    private val _downloadStatus = MutableStateFlow(DownloadStatus.IDLE)
    val downloadStatus = _downloadStatus.asStateFlow()
    private val _downloadProgress = MutableStateFlow(0.0)
    val downloadProgress = _downloadProgress.asStateFlow()
    private val _manualDownloadActive = MutableStateFlow(false)
    val manualDownloadActive = _manualDownloadActive.asStateFlow()

    // 推送
    private val _pushDevices = MutableStateFlow<List<PushDevice>>(emptyList())
    val pushDevices = _pushDevices.asStateFlow()
    private val _scanningDevices = MutableStateFlow(false)
    val scanningDevices = _scanningDevices.asStateFlow()
    private val _showDevicePicker = MutableStateFlow(false)
    val showDevicePicker = _showDevicePicker.asStateFlow()

    // 接收推送后预填
    private val _pendingImportText = MutableStateFlow<String?>(null)
    val pendingImportText = _pendingImportText.asStateFlow()
    private val _pendingImportDate = MutableStateFlow<String?>(null)
    val pendingImportDate = _pendingImportDate.asStateFlow()

    // 推送/更新/其他消息
    private val _toast = MutableStateFlow<ToastData?>(null)
    val toast = _toast.asStateFlow()

    // 代理测试
    private val _testingProxy = MutableStateFlow(false)
    val testingProxy = _testingProxy.asStateFlow()
    private val _proxyTestResult = MutableStateFlow<String?>(null)
    val proxyTestResult = _proxyTestResult.asStateFlow()

    // 同步状态
    private val _lastSyncStatus = MutableStateFlow("")
    val lastSyncStatus = _lastSyncStatus.asStateFlow()

    // 待导入的远程剧名（用于输入框预填）
    private val _pendingRemoteTitles = MutableStateFlow<List<String>>(emptyList())
    val pendingRemoteTitles = _pendingRemoteTitles.asStateFlow()

    val currentDateString: String get() = AppConstants.dateString(_currentDate.value)

    // 局域网服务状态（供设置页展示）
    val lanRunning: StateFlow<Boolean> = LanServer.runningFlow
    val lanPort: StateFlow<Int> = LanServer.portFlow

    private val _lanIp = MutableStateFlow<String?>(null)
    val lanIp: StateFlow<String?> = _lanIp.asStateFlow()

    fun refreshLanIp() {
        _lanIp.value = DeviceDiscovery.localIpv4()
    }

    init {
        viewModelScope.launch {
            LanServer.pushEvents.collect { payload -> handleIncomingPush(payload) }
        }
        viewModelScope.launch {
            UpdateDownloader.progress.collect { _downloadProgress.value = it }
        }
        viewModelScope.launch { load() }
    }

    // ---------- 加载 ----------
    private suspend fun load() {
        withContext(Dispatchers.IO) {
            _days.value = source.loadDays()
            _undos.value = source.loadUndos()
            _settings.value = source.loadSettings()
        }
        syncFeedbackSettings()
        applyLanSetting()
        cleanUpPendingDownload()
        _isLoaded.value = true
    }

    private fun cleanUpPendingDownload() {
        val pd = _pendingDownload.value ?: return
        if (AppConstants.compareVersions(AppVersion.name, pd.version) >= 0) {
            UpdateDownloader.deleteExistingApk()
            _pendingDownload.value = null
            _downloadStatus.value = DownloadStatus.IDLE
        }
    }

    private fun syncFeedbackSettings() {
        val s = _settings.value
        Haptics.enabled = s.hapticFeedback
        Haptics.soundEnabled = s.soundFeedback
    }

    private fun applyLanSetting() {
        if (_settings.value.lanEnabled) {
            LanServer.start()
            viewModelScope.launch {
                delay(300)
                _lanIp.value = DeviceDiscovery.localIpv4()
            }
        } else {
            LanServer.stop()
            _lanIp.value = null
        }
    }

    // ---------- 日期 ----------
    fun shiftDate(delta: Int) {
        val cal = Calendar.getInstance().apply { time = _currentDate.value; add(Calendar.DAY_OF_YEAR, delta) }
        val next = cal.time
        if (delta < 0 || !next.after(Date())) _currentDate.value = next
    }
    fun goToToday() { _currentDate.value = Date() }
    fun setDate(d: Date) { _currentDate.value = d }

    fun setActiveSheet(s: ActiveSheet?) { _activeSheet.value = s }

    fun consumePendingRemoteTitle() {
        val list = _pendingRemoteTitles.value
        if (list.isNotEmpty()) _pendingRemoteTitles.value = list.drop(1)
    }

    // ---------- 保存 ----------
    private var saveJob: kotlinx.coroutines.Job? = null
    private fun scheduleSaveDays() {
        saveJob?.cancel()
        val snapshot = _days.value
        saveJob = viewModelScope.launch {
            delay(200)
            withContext(Dispatchers.IO) { source.saveDays(snapshot) }
        }
    }
    fun updateSettings(newSettings: AppSettings) {
        _settings.value = newSettings
    }

    fun saveSettings() {
        syncFeedbackSettings()
        applyLanSetting()
        val snap = _settings.value
        viewModelScope.launch { withContext(Dispatchers.IO) { source.saveSettings(snap) } }
    }
    private fun saveUndosNow() {
        val snap = _undos.value
        viewModelScope.launch { withContext(Dispatchers.IO) { source.saveUndos(snap) } }
    }

    // ---------- 数据访问 ----------
    fun day(date: String): DayData = _days.value[date] ?: DayData(date)
    fun summary(date: String): Summary {
        val d = day(date)
        val valid = d.dramas.map { it.title + "|" + it.isFast }.toSet()
        val sums = mutableMapOf<String, Int>()
        var total = 0
        val counted = mutableSetOf<String>()
        for (r in d.records) {
            val k = r.title + "|" + r.isFast
            if (k !in valid) continue
            sums[r.platform] = (sums[r.platform] ?: 0) + r.count
            total += r.count
            counted.add(k)
        }
        return Summary(counted.size, sums, total)
    }
    fun platformConfig(name: String): PlatformConfig =
        _settings.value.platforms.firstOrNull { it.name == name }
            ?: PlatformConfig(name, name.take(1), "#8E8E93")

    // ---------- 增删改 ----------
    fun addDrama(date: String, title: String, isFast: Boolean) {
        val t = title.trim()
        if (t.isEmpty()) return
        val days = _days.value.toMutableMap()
        val d = (days[date] ?: DayData(date)).let {
            DayData(it.date, it.dramas.toMutableList(), it.records.toMutableList())
        }
        if (d.dramas.any { it.title == t && it.isFast == isFast }) return
        d.dramas.add(Drama(title = t, isFast = isFast))
        days[date] = d
        _days.value = days
        scheduleSaveDays()
    }

    fun deleteDrama(date: String, title: String, isFast: Boolean) {
        val days = _days.value.toMutableMap()
        val orig = days[date] ?: return
        val d = DayData(orig.date, orig.dramas.toMutableList(), orig.records.toMutableList())
        d.dramas.removeAll { it.title == title && it.isFast == isFast }
        d.records.removeAll { it.title == title && it.isFast == isFast }
        days[date] = d
        _days.value = days
        scheduleSaveDays()
    }

    fun renameDrama(date: String, oldTitle: String, newTitle: String, isFast: Boolean) {
        val nt = newTitle.trim()
        if (nt.isEmpty() || oldTitle == nt) return
        val days = _days.value.toMutableMap()
        val orig = days[date] ?: return
        val d = DayData(orig.date, orig.dramas.toMutableList(), orig.records.toMutableList())
        val merged = d.dramas.any { it.title == nt && it.isFast == isFast }
        if (merged) {
            val moving = d.records.filter { it.title == oldTitle && it.isFast == isFast }
            for (r in moving) {
                val idx = d.records.indexOfFirst {
                    it.title == nt && it.platform == r.platform && it.isFast == isFast
                }
                if (idx >= 0) d.records[idx] = d.records[idx].copy(count = d.records[idx].count + r.count)
                else d.records.add(r.copy(title = nt))
            }
            d.records.removeAll { it.title == oldTitle && it.isFast == isFast }
            d.dramas.removeAll { it.title == oldTitle && it.isFast == isFast }
        } else {
            for (i in d.dramas.indices) if (d.dramas[i].title == oldTitle && d.dramas[i].isFast == isFast)
                d.dramas[i] = d.dramas[i].copy(title = nt)
            for (i in d.records.indices) if (d.records[i].title == oldTitle && d.records[i].isFast == isFast)
                d.records[i] = d.records[i].copy(title = nt)
        }
        days[date] = d
        _days.value = days
        scheduleSaveDays()
    }

    fun incrementPlatform(date: String, title: String, platform: String, isFast: Boolean, delta: Int): Int {
        val days = _days.value.toMutableMap()
        val orig = days[date] ?: DayData(date)
        val d = DayData(orig.date, orig.dramas.toMutableList(), orig.records.toMutableList())
        val now = AppConstants.shortTime()
        val idx = d.records.indexOfFirst { it.title == title && it.platform == platform && it.isFast == isFast }
        if (idx >= 0) {
            val nc = d.records[idx].count + delta
            if (nc <= 0) {
                d.records.removeAt(idx)
                days[date] = d; _days.value = days; scheduleSaveDays(); return 0
            }
            d.records[idx] = d.records[idx].copy(count = nc, updatedAt = now)
            days[date] = d; _days.value = days; scheduleSaveDays(); return nc
        } else if (delta > 0) {
            d.records.add(PlatformRecord(title = title, platform = platform, isFast = isFast, count = delta, updatedAt = now))
            if (d.dramas.none { it.title == title && it.isFast == isFast })
                d.dramas.add(Drama(title = title, isFast = isFast))
            days[date] = d; _days.value = days; scheduleSaveDays(); return delta
        }
        return 0
    }

    fun importItems(date: String, items: List<ShareItem>, dedup: Boolean): Pair<Int, Int> {
        val days = _days.value.toMutableMap()
        val orig = days[date] ?: DayData(date)
        val d = DayData(orig.date, orig.dramas.toMutableList(), orig.records.toMutableList())
        val now = AppConstants.shortTime()
        var processed = 0; var skipped = 0
        for (item in items) {
            val exists = d.dramas.any { it.title == item.title && it.isFast == item.isFast }
            if (exists && dedup) { skipped++; continue }
            if (!exists) d.dramas.add(Drama(title = item.title, isFast = item.isFast))
            for ((pl, c) in item.platforms) {
                if (c <= 0) continue
                val idx = d.records.indexOfFirst { it.title == item.title && it.platform == pl && it.isFast == item.isFast }
                if (idx >= 0) d.records[idx] = d.records[idx].copy(count = d.records[idx].count + c, updatedAt = now)
                else d.records.add(PlatformRecord(title = item.title, platform = pl, isFast = item.isFast, count = c, updatedAt = now))
            }
            processed++
        }
        days[date] = d
        _days.value = days
        scheduleSaveDays()
        return processed to skipped
    }

    // ---------- 撤销 ----------
    fun pushUndo(date: String) {
        val d = day(date)
        val snap = UndoSnapshot(
            id = java.util.UUID.randomUUID().toString(),
            date = date,
            createdAt = AppConstants.nowString(),
            data = DayData(d.date, d.dramas.toMutableList(), d.records.toMutableList()),
        )
        val list = _undos.value.toMutableList()
        list.add(0, snap)
        val max = _settings.value.maxUndoPerDay
        val sameDate = list.filter { it.date == date }
        if (sameDate.size > max) {
            val toRemove = sameDate.drop(max).map { it.id }.toSet()
            list.removeAll { it.id in toRemove }
        }
        _undos.value = list
        saveUndosNow()
    }

    fun undosFor(date: String): List<UndoSnapshot> = _undos.value.filter { it.date == date }

    fun restore(snap: UndoSnapshot) {
        val days = _days.value.toMutableMap()
        days[snap.date] = DayData(snap.data.date, snap.data.dramas.toMutableList(), snap.data.records.toMutableList())
        _days.value = days
        scheduleSaveDays()
    }

    // ---------- 导出文本 ----------
    fun exportText(date: String): String {
        val d = day(date)
        val valid = d.dramas.filter { drama -> d.records.any { it.title == drama.title && it.isFast == drama.isFast } }
        if (valid.isEmpty()) return ""
        val lines = mutableListOf("统计 $date", "")
        for (drama in valid) {
            val recs = d.records.filter { it.title == drama.title && it.isFast == drama.isFast }
            val disp = if (drama.isFast) drama.title + AppConstants.fastSuffix else drama.title
            val parts = recs.map { it.platform.take(1) + ":" + it.count }
            lines.add("【$disp】" + parts.joinToString(" | "))
        }
        lines.add("——————————")
        lines.add("共 ${valid.size} 部剧")
        val sums = mutableMapOf<String, Int>()
        for (drama in valid) d.records.filter { it.title == drama.title && it.isFast == drama.isFast }
            .forEach { sums[it.platform] = (sums[it.platform] ?: 0) + it.count }
        lines.add(sums.entries.joinToString(" | ") { it.key.take(1) + ":" + it.value })
        lines.add("全部合计:" + sums.values.sum())
        return lines.joinToString("\n")
    }

    fun exportShareCode(date: String): String {
        val d = day(date)
        val valid = d.dramas.filter { drama -> d.records.any { it.title == drama.title && it.isFast == drama.isFast } }
        if (valid.isEmpty()) return ""
        val order = valid.map { it.title to it.isFast }
        val groups = mutableMapOf<String, MutableMap<String, Int>>()
        for (drama in valid) {
            val key = drama.title + "|" + drama.isFast
            val m = mutableMapOf<String, Int>()
            d.records.filter { it.title == drama.title && it.isFast == drama.isFast }
                .forEach { m[it.platform] = it.count }
            groups[key] = m
        }
        val packed = ShareCode.pack(order, groups)
        return ShareCode.encrypt(packed)
    }

    fun parseImportText(text: String): Pair<List<ShareItem>, List<String>> {
        val items = mutableListOf<ShareItem>()
        val bad = mutableListOf<String>()
        val lines = text.split("\n")
        var currentTitle: String? = null
        var currentFast = false
        var currentPlatforms = mutableMapOf<String, Int>()
        val platformShort = _settings.value.platforms

        fun flush() {
            val t = currentTitle ?: return
            if (currentPlatforms.isNotEmpty()) items.add(ShareItem(t, currentFast, currentPlatforms))
            currentPlatforms = mutableMapOf()
        }

        for (raw in lines) {
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("统计 ") || line.startsWith("——") ||
                line.startsWith("共 ") || line.startsWith("全部合计")) continue
            val m = Regex("^[【\\[](.+?)[】\\]]\\s*(.+)$").find(line)
            if (m != null) {
                flush()
                val (title, fast) = AppConstants.splitFast(m.groupValues[1])
                currentTitle = title
                currentFast = fast
                val parts = m.groupValues[2].split("|")
                for (p in parts) {
                    val kv = p.trim().split(":")
                    if (kv.size != 2) continue
                    val pname = platformShort.firstOrNull { it.short == kv[0].trim() }?.name
                        ?: platformShort.firstOrNull { it.name.startsWith(kv[0].trim()) }?.name
                    val cnt = kv[1].trim().toIntOrNull()
                    if (pname != null && cnt != null) currentPlatforms[pname] = cnt
                }
                continue
            }
            if (line.contains(":")) {
                val parts = line.split("|")
                for (p in parts) {
                    val kv = p.trim().split(":")
                    if (kv.size != 2) continue
                    val pname = platformShort.firstOrNull { it.short == kv[0].trim() }?.name
                        ?: platformShort.firstOrNull { it.name.startsWith(kv[0].trim()) }?.name
                    val cnt = kv[1].trim().toIntOrNull()
                    if (pname != null && cnt != null && currentTitle != null) currentPlatforms[pname] = cnt
                }
                continue
            }
            bad.add(line)
        }
        flush()
        return items to bad
    }

    // ---------- 分享码导入 ----------
    fun importShareCode(text: String, date: String, dedup: Boolean): Pair<Int, Int>? {
        return try {
            val raw = if (text.startsWith(AppConstants.sharePrefix)) ShareCode.decrypt(text)
            else if (text.startsWith(AppConstants.encPrefix)) {
                val plain = ShareCode.decryptDCT1(text)
                parseImportText(plain).first.let { items ->
                    return importItems(date, items, dedup)
                }
            } else throw ShareCodeException(ShareCodeException.Kind.INVALID_FORMAT)
            val (order, groups) = ShareCode.unpack(raw)
            val items = order.map { (title, fast) ->
                ShareItem(title, fast, (groups[title + "|" + fast] ?: emptyMap()).toMutableMap())
            }
            importItems(date, items, dedup)
        } catch (e: Exception) { null }
    }

    // ---------- 局域网推送 ----------
    fun scanDevicesForPush() {
        if (_scanningDevices.value) return
        _scanningDevices.value = true
        _pushDevices.value = emptyList()
        viewModelScope.launch {
            val devices = DeviceDiscovery.scan(
                if (LanServer.port > 0) LanServer.port else LanServer.DEFAULT_PORT,
                timeoutMs = 800,
            )
            _pushDevices.value = devices
            _scanningDevices.value = false
            if (devices.isEmpty()) showToast("未发现局域网内的其他设备", ToastStyle.ERROR)
            else _showDevicePicker.value = true
        }
    }

    fun pushCurrentDateTo(devices: List<PushDevice>) {
        val ds = currentDateString
        val d = day(ds)
        if (d.dramas.isEmpty()) { showToast("当前日期没有数据", ToastStyle.ERROR); return }
        val payload = PushPayload(
            sender = DeviceDiscovery.selfDisplayName(),
            senderId = DeviceDiscovery.selfDeviceId(),
            date = ds,
            dramas = d.dramas.map { PushDrama(it.title, it.isFast) },
            records = d.records.map { PushRecord(it.title, it.platform, it.isFast, it.count, it.updatedAt) },
        )
        viewModelScope.launch {
            var ok = 0
            for (dev in devices) if (DeviceDiscovery.push(payload, dev)) ok++
            if (ok == devices.size) {
                showToast(if (devices.size == 1) "✅ 已发送到 ${devices[0].name}" else "✅ 已发送到 $ok 台设备", ToastStyle.SUCCESS)
            } else showToast("⚠️ 部分发送失败 ($ok/${devices.size})", ToastStyle.ERROR)
        }
    }

    private fun handleIncomingPush(payload: PushPayload) {
        val text = payloadToExportText(payload)
        _pendingImportText.value = text
        _pendingImportDate.value = payload.date
        _activeSheet.value = ActiveSheet.IMPORT_DATA
        showToast("收到来自「${payload.sender}」的数据", ToastStyle.SUCCESS)
    }

    private fun payloadToExportText(payload: PushPayload): String {
        val lines = mutableListOf("统计 ${payload.date}", "")
        for (pd in payload.dramas) {
            val recs = payload.records.filter { it.title == pd.title && it.isFast == pd.isFast }
            if (recs.isEmpty()) continue
            val disp = if (pd.isFast) pd.title + AppConstants.fastSuffix else pd.title
            lines.add("【$disp】" + recs.joinToString(" | ") { it.platform.take(1) + ":" + it.count })
        }
        lines.add("——————————")
        lines.add("共 ${payload.dramas.size} 部剧")
        val sums = mutableMapOf<String, Int>()
        payload.records.forEach { sums[it.platform] = (sums[it.platform] ?: 0) + it.count }
        lines.add(sums.entries.joinToString(" | ") { it.key.take(1) + ":" + it.value })
        lines.add("全部合计:" + sums.values.sum())
        return lines.joinToString("\n")
    }

    // ---------- 更新 ----------
    fun checkForUpdate(silent: Boolean = false, force: Boolean = false) {
        if (_checkingUpdate.value) return
        val last = _settings.value.lastUpdateCheck
        if (silent && !force && last > 0 && System.currentTimeMillis() - last < AppConstants.updateCheckMinIntervalMs) return
        _checkingUpdate.value = true
        viewModelScope.launch {
            try {
                val info = UpdateChecker.fetchLatestInfo(_settings.value.customUpdateProxy)
                _settings.value = _settings.value.copy(lastUpdateCheck = System.currentTimeMillis())
                saveSettings()
                if (AppConstants.compareVersions(info.version, AppVersion.name) > 0) {
                    _pendingRelease.value = info
                    val pd = _pendingDownload.value
                    if (pd != null && pd.version == info.version) {
                        _downloadStatus.value = DownloadStatus.READY
                        if (_settings.value.autoPromptInstall) _showUpdateAlert.value = true
                        return@launch
                    }
                    if (_settings.value.silentDownload) startDownload(info, manual = !silent)
                    else _showUpdateAlert.value = true
                } else {
                    if (!silent) _lastMessage.value = "已是最新版本（${AppVersion.name}）"
                }
            } catch (e: Exception) {
                if (!silent) _lastMessage.value = e.message
            } finally { _checkingUpdate.value = false }
        }
    }

    private fun startDownload(info: ReleaseInfo, manual: Boolean) {
        _downloadStatus.value = DownloadStatus.DOWNLOADING
        _manualDownloadActive.value = manual
        _downloadProgress.value = 0.0
        viewModelScope.launch {
            val res = UpdateDownloader.startDownload(info.apiAssetURL, info.version, _settings.value.customUpdateProxy)
            res.onSuccess { file ->
                val pd = PendingDownload(info.version, file.absolutePath, System.currentTimeMillis(), file.length())
                _pendingDownload.value = pd
                _downloadStatus.value = DownloadStatus.READY
                _downloadProgress.value = 1.0
                _manualDownloadActive.value = false
                if (_settings.value.autoPromptInstall) _showUpdateAlert.value = true
                showToast("✅ v${info.version} 已下载完成", ToastStyle.SUCCESS)
            }.onFailure { e ->
                _downloadStatus.value = DownloadStatus.IDLE
                _downloadProgress.value = 0.0
                _manualDownloadActive.value = false
                _lastMessage.value = "下载失败：${e.message}"
            }
        }
    }

    fun postponeUpdate() {
        _settings.value = _settings.value.copy(lastPostponedTime = System.currentTimeMillis())
        saveSettings()
        _showUpdateAlert.value = false
    }

    fun clearPendingDownload() {
        UpdateDownloader.deleteExistingApk()
        _pendingDownload.value = null
        _downloadStatus.value = DownloadStatus.IDLE
        _downloadProgress.value = 0.0
    }

    fun dismissUpdateAlert() { _showUpdateAlert.value = false }

    fun dismissDevicePicker() { _showDevicePicker.value = false }

    fun downloadLatest() {
        val info = _pendingRelease.value ?: return
        startDownload(info, manual = true)
    }

    fun installApk(path: String) {
        try {
            val ctx = com.shortdrama.count.App.instance
            val file = java.io.File(path)
            val uri = androidx.core.content.FileProvider.getUriForFile(
                ctx, ctx.packageName + ".fileprovider", file)
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            ctx.startActivity(intent)
        } catch (e: Exception) {
            showToast("无法启动安装：" + e.message, ToastStyle.ERROR)
        }
    }

    fun testCustomProxy(proxy: String) {
        if (_testingProxy.value) return
        _testingProxy.value = true
        _proxyTestResult.value = null
        viewModelScope.launch {
            val err = UpdateChecker.testProxy(proxy)
            _proxyTestResult.value = if (err == null) "✅ 代理可用" else "❌ 失败：$err"
            _testingProxy.value = false
        }
    }

    // ---------- 远程同步 ----------
    fun syncToRemote() {
        val s = _settings.value
        if (!s.remoteEnabled || s.remoteHost.isEmpty()) { _lastSyncStatus.value = "远程未启用"; return }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val base = "https://" + s.remoteHost.trimEnd('/') + "/"
                val json = kotlinx.serialization.json.Json { encodeDefaults = true }
                val daysElement = json.encodeToJsonElement(
                    kotlinx.serialization.builtins.MapSerializer(
                        String.serializer(),
                        DayData.serializer()), _days.value)
                val undosElement = json.encodeToJsonElement(
                    kotlinx.serialization.builtins.ListSerializer(UndoSnapshot.serializer()), _undos.value)
                postJson(base + "api/days", s.remoteToken, s.remoteDBType.name.lowercase(), daysElement)
                postJson(base + "api/undos", s.remoteToken, s.remoteDBType.name.lowercase(), undosElement)
                _lastSyncStatus.value = "同步成功"
            } catch (e: Exception) {
                _lastSyncStatus.value = "同步失败：${e.message}"
            }
        }
    }

    private fun postJson(urlStr: String, token: String, dbType: String, payload: kotlinx.serialization.json.JsonElement) {
        val conn = (java.net.URL(urlStr).openConnection() as java.net.HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 20000
            readTimeout = 20000
            doOutput = true
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("X-DB-Type", dbType)
            setRequestProperty("Content-Type", "application/json")
        }
        val body = payload.toString()
        conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
        conn.responseCode
    }

    // ---------- Toast ----------
    fun showToast(message: String, style: ToastStyle = ToastStyle.INFO) {
        _toast.value = ToastData(message, style)
        viewModelScope.launch {
            delay(1800)
            if (_toast.value?.message == message) _toast.value = null
        }
    }
    fun clearToast() { _toast.value = null }

    /** 导出全部数据为 JSON 字符串（备份用） */
    fun exportBackup(): String {
        val daysJson = kotlinx.serialization.json.Json { encodeDefaults = true }
            .encodeToString(
                kotlinx.serialization.builtins.MapSerializer(
                    String.serializer(),
                    DayData.serializer()), _days.value)
        val undosJson = kotlinx.serialization.json.Json { encodeDefaults = true }
            .encodeToString(
                kotlinx.serialization.builtins.ListSerializer(UndoSnapshot.serializer()), _undos.value)
        val settingsJson = kotlinx.serialization.json.Json { encodeDefaults = true }
            .encodeToString(AppSettings.serializer(), _settings.value)
        val obj = kotlinx.serialization.json.buildJsonObject {
            put("days", kotlinx.serialization.json.Json.parseToJsonElement(daysJson))
            put("undos", kotlinx.serialization.json.Json.parseToJsonElement(undosJson))
            put("settings", kotlinx.serialization.json.Json.parseToJsonElement(settingsJson))
        }
        return obj.toString()
    }

    /** 从备份 JSON 恢复 */
    fun importBackup(json: String): Boolean {
        return try {
            val root = kotlinx.serialization.json.Json.parseToJsonElement(json).jsonObject
            val j = kotlinx.serialization.json.Json { ignoreUnknownKeys = true; encodeDefaults = true }
            root["days"]?.let {
                _days.value = j.decodeFromJsonElement(
                    kotlinx.serialization.builtins.MapSerializer(
                        String.serializer(),
                        DayData.serializer()), it)
            }
            root["undos"]?.let {
                _undos.value = j.decodeFromJsonElement(
                    kotlinx.serialization.builtins.ListSerializer(UndoSnapshot.serializer()), it)
            }
            root["settings"]?.let {
                _settings.value = j.decodeFromJsonElement(AppSettings.serializer(), it)
            }
            saveSettings()
            viewModelScope.launch { withContext(Dispatchers.IO) {
                source.saveDays(_days.value); source.saveUndos(_undos.value)
            } }
            true
        } catch (e: Exception) { false }
    }

    fun consumePendingImport() {
        _pendingImportText.value = null
        _pendingImportDate.value = null
    }

    override fun onCleared() {
        super.onCleared()
        LanServer.stop()
    }
}

data class Summary(val dramaCount: Int, val sums: Map<String, Int>, val total: Int)

enum class ToastStyle { INFO, SUCCESS, ERROR }
data class ToastData(val message: String, val style: ToastStyle = ToastStyle.INFO)
