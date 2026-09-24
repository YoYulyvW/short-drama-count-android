package com.shortdrama.count.data

import com.shortdrama.count.App
import com.shortdrama.count.model.AppSettings
import com.shortdrama.count.model.DayData
import com.shortdrama.count.model.UndoSnapshot
import kotlinx.serialization.json.Json
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.builtins.ListSerializer
import java.io.File

class LocalDataSource {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = false
    }

    private val docs: File get() = App.instance.filesDir

    private val daysFile get() = File(docs, "drama_data.json")
    private val undosFile get() = File(docs, "drama_undo.json")
    private val settingsFile get() = File(docs, "drama_settings.json")
    private val pendingFile get() = File(docs, "pending_download.json")

    fun loadDays(): MutableMap<String, DayData> {
        if (!daysFile.exists()) return mutableMapOf()
        return try {
            json.decodeFromString(
                MapSerializer(String.serializer(), DayData.serializer()),
                daysFile.readText()
            ).toMutableMap()
        } catch (e: Exception) { mutableMapOf() }
    }

    fun saveDays(days: Map<String, DayData>) {
        try {
            daysFile.writeText(json.encodeToString(
                MapSerializer(String.serializer(), DayData.serializer()), days))
        } catch (_: Exception) {}
    }

    fun loadUndos(): MutableList<UndoSnapshot> {
        if (!undosFile.exists()) return mutableListOf()
        return try {
            json.decodeFromString(ListSerializer(UndoSnapshot.serializer()),
                undosFile.readText()).toMutableList()
        } catch (e: Exception) { mutableListOf() }
    }

    fun saveUndos(undos: List<UndoSnapshot>) {
        try {
            undosFile.writeText(json.encodeToString(
                ListSerializer(UndoSnapshot.serializer()), undos))
        } catch (_: Exception) {}
    }

    fun loadSettings(): AppSettings {
        if (!settingsFile.exists()) return AppSettings()
        return try {
            json.decodeFromString(AppSettings.serializer(), settingsFile.readText())
        } catch (e: Exception) { AppSettings() }
    }

    fun saveSettings(s: AppSettings) {
        try {
            settingsFile.writeText(json.encodeToString(AppSettings.serializer(), s))
        } catch (_: Exception) {}
    }
}
