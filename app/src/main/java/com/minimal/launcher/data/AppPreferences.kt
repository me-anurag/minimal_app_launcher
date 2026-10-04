package com.minimal.launcher.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore by preferencesDataStore(name = "launcher_prefs")

/** Exactly two home-screen layouts. */
enum class HomeLayout { VERTICAL, TWO_COLUMNS }

data class LauncherSettings(
    val showClock: Boolean = true,
    val showDate: Boolean = true,
    val use24Hour: Boolean = true,
    val showSeconds: Boolean = false,
    val layout: HomeLayout = HomeLayout.VERTICAL,
    val showUsage: Boolean = true,
    /** Ordered package names. null = user has never saved (use defaults). */
    val selectedApps: List<String>? = null,
)

class AppPreferences(private val context: Context) {

    private object K {
        val showClock = booleanPreferencesKey("show_clock")
        val showDate = booleanPreferencesKey("show_date")
        val use24Hour = booleanPreferencesKey("use_24h")
        val showSeconds = booleanPreferencesKey("show_seconds")
        val layout = stringPreferencesKey("home_layout")
        val showUsage = booleanPreferencesKey("show_usage")
        val usageStart = longPreferencesKey("usage_start_day")
        val usageLog = stringPreferencesKey("usage_log")
        val rewarded = stringPreferencesKey("usage_rewarded")
        val apps = stringPreferencesKey("selected_apps")
    }

    val settings: Flow<LauncherSettings> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { p ->
            LauncherSettings(
                showClock = p[K.showClock] ?: true,
                showDate = p[K.showDate] ?: true,
                use24Hour = p[K.use24Hour] ?: true,
                showSeconds = p[K.showSeconds] ?: false,
                layout = p[K.layout]
                    ?.let { runCatching { HomeLayout.valueOf(it) }.getOrNull() }
                    ?: HomeLayout.VERTICAL,
                showUsage = p[K.showUsage] ?: true,
                selectedApps = p[K.apps]?.split("\n")?.filter { it.isNotBlank() },
            )
        }

    suspend fun setShowClock(v: Boolean) { context.dataStore.edit { it[K.showClock] = v } }
    suspend fun setShowDate(v: Boolean) { context.dataStore.edit { it[K.showDate] = v } }
    suspend fun setUse24Hour(v: Boolean) { context.dataStore.edit { it[K.use24Hour] = v } }
    suspend fun setShowSeconds(v: Boolean) { context.dataStore.edit { it[K.showSeconds] = v } }
    suspend fun setLayout(v: HomeLayout) { context.dataStore.edit { it[K.layout] = v.name } }
    suspend fun setShowUsage(v: Boolean) { context.dataStore.edit { it[K.showUsage] = v } }
    suspend fun setApps(list: List<String>) {
        context.dataStore.edit { it[K.apps] = list.joinToString("\n") }
    }

    // ---- Screen-time history: finished days only, stored as "epochDay:minutes,..." ----

    /** Returns (first tracked epoch day or null, finished-day minutes). */
    suspend fun getUsageLog(): Pair<Long?, Map<Long, Int>> {
        val p = context.dataStore.data.catch { emit(emptyPreferences()) }.first()
        val log = HashMap<Long, Int>()
        p[K.usageLog]?.split(",")?.forEach { entry ->
            val parts = entry.split(":")
            val day = parts.getOrNull(0)?.toLongOrNull()
            val minutes = parts.getOrNull(1)?.toIntOrNull()
            if (day != null && minutes != null) log[day] = minutes
        }
        return p[K.usageStart] to log
    }

    suspend fun saveUsageLog(startDay: Long, log: Map<Long, Int>) {
        context.dataStore.edit {
            it[K.usageStart] = startDay
            it[K.usageLog] = log.entries.joinToString(",") { e -> "${e.key}:${e.value}" }
        }
    }

    // ---- Days whose flag was earned (under 1h AND claimed at 4 AM) ----

    suspend fun getRewarded(): Set<Long> {
        val p = context.dataStore.data.catch { emit(emptyPreferences()) }.first()
        return p[K.rewarded]?.split(",")?.mapNotNull { it.toLongOrNull() }?.toSet() ?: emptySet()
    }

    suspend fun saveRewarded(days: Set<Long>) {
        context.dataStore.edit { it[K.rewarded] = days.sorted().joinToString(",") }
    }
}