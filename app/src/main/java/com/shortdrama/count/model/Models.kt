package com.shortdrama.count.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class Drama(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    @SerialName("isFast") val isFast: Boolean = false,
)

@Serializable
data class PlatformRecord(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val platform: String,
    @SerialName("isFast") val isFast: Boolean = false,
    val count: Int = 0,
    val updatedAt: String = "",
)

@Serializable
data class DayData(
    val date: String,
    val dramas: MutableList<Drama> = mutableListOf(),
    val records: MutableList<PlatformRecord> = mutableListOf(),
)

data class ShareItem(
    val title: String,
    val isFast: Boolean,
    val platforms: MutableMap<String, Int> = mutableMapOf(),
)

@Serializable
data class UndoSnapshot(
    val id: String,
    val date: String,
    val createdAt: String,
    val data: DayData,
)

@Serializable
data class PlatformConfig(
    val name: String,
    val short: String,
    val colorHex: String,
) {
    companion object {
        val defaults = listOf(
            PlatformConfig("橙子建站", "橙", "#FF8822"),
            PlatformConfig("懂车帝", "懂", "#FFD12E"),
            PlatformConfig("易车", "易", "#0084FF"),
            PlatformConfig("车主之家", "车", "#1F4E79"),
            PlatformConfig("瓜子", "瓜", "#36CFC9"),
        )
    }
}

@Serializable
enum class RemoteDatabaseType(val display: String, val defaultPort: Int) {
    SQLITE("SQLite (.db / .sq3)", 0),
    MYSQL("MySQL", 3306),
}

@Serializable
data class ReleaseInfo(
    val version: String = "",
    val tagName: String = "",
    val assetId: Long = 0,
    val assetName: String = "",
    val apiAssetURL: String = "",
    val browserDownloadURL: String = "",
    val releaseNotes: String = "",
    val publishedAt: String = "",
    val htmlURL: String = "",
)

data class PushDevice(
    val ip: String,
    val name: String,
    val deviceId: String,
    val port: Int,
)

@Serializable
data class PushPayload(
    val version: Int = 1,
    val sender: String,
    val senderId: String,
    val date: String,
    val dramas: List<PushDrama>,
    val records: List<PushRecord>,
)

@Serializable
data class PushDrama(val title: String, @SerialName("isFast") val isFast: Boolean)

@Serializable
data class PushRecord(
    val title: String,
    val platform: String,
    @SerialName("isFast") val isFast: Boolean,
    val count: Int,
    val updatedAt: String = "",
)

@Serializable
data class PendingDownload(
    val version: String,
    val localPath: String,
    val downloadedAt: Long = System.currentTimeMillis(),
    val sizeBytes: Long = 0,
)

enum class DownloadStatus { IDLE, DOWNLOADING, READY, INSTALLING }

enum class ActiveSheet { QUICK_TOOLS, IMPORT_DATA, EXPORT_TEXT, EXPORT_CODE, EXPORT_IMAGE, UNDO }

@Serializable
data class AppSettings(
    val platforms: MutableList<PlatformConfig> = PlatformConfig.defaults.toMutableList(),
    val hapticFeedback: Boolean = true,
    val soundFeedback: Boolean = true,
    val defaultPlatform: String = "橙子建站",
    val maxUndoPerDay: Int = 10,
    val sortByAds: Boolean = true,
    val dailyReminder: Boolean = false,
    val autoCollapse: Boolean = true,
    val timeInline: Boolean = true,
    val showQuickTools: Boolean = true,
    val toolButtonSize: Int = 52,
    val lanEnabled: Boolean = false,
    val showHandshakeToast: Boolean = true,
    val autoUpdateCheck: Boolean = true,
    val lastUpdateCheck: Long = 0L,
    val lastPostponedTime: Long = 0L,
    val customUpdateProxy: String = "",
    val silentDownload: Boolean = true,
    val autoPromptInstall: Boolean = true,
    val remoteEnabled: Boolean = false,
    val remoteDBType: RemoteDatabaseType = RemoteDatabaseType.MYSQL,
    val remoteHost: String = "",
    val remotePort: Int = 3306,
    val remoteDatabase: String = "",
    val remoteUser: String = "",
    val remotePassword: String = "",
    val remoteToken: String = "",
    val autoSync: Boolean = false,
)
