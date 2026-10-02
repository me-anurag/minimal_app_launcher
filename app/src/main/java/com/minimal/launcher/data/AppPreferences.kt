package com.minimal.launcher.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore by preferencesDataStore(name = "launcher_prefs")

data class LauncherSettings(
    val showClock: Boolean = true,
    val showDate: Boolean = true,
    val use24Hour: Boolean = true,
    val showSeconds: Boolean = false,
    /** Ordered package names. null = user has never saved (use defaults). */
    val selectedApps: List<String>? = null,
)

class AppPreferences(private val context: Context) {

    private object K {
        val showClock = booleanPreferencesKey("show_clock")
        val showDate = booleanPreferencesKey("show_date")
        val use24Hour = booleanPreferencesKey("use_24h")
        val showSeconds = booleanPreferencesKey("show_seconds")
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
                selectedApps = p[K.apps]?.split("\n")?.filter { it.isNotBlank() },
            )
        }

    suspend fun setShowClock(v: Boolean) { context.dataStore.edit { it[K.showClock] = v } }
    suspend fun setShowDate(v: Boolean) { context.dataStore.edit { it[K.showDate] = v } }
    suspend fun setUse24Hour(v: Boolean) { context.dataStore.edit { it[K.use24Hour] = v } }
    suspend fun setShowSeconds(v: Boolean) { context.dataStore.edit { it[K.showSeconds] = v } }
    suspend fun setApps(list: List<String>) {
        context.dataStore.edit { it[K.apps] = list.joinToString("\n") }
    }
}
