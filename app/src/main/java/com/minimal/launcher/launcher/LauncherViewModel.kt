package com.minimal.launcher.launcher
import kotlinx.coroutines.cancelAndJoin
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.minimal.launcher.apps.AppEntry
import com.minimal.launcher.apps.InstalledAppsProvider
import com.minimal.launcher.data.AppPreferences
import com.minimal.launcher.data.HomeLayout
import com.minimal.launcher.usage.UsageTracker
import com.minimal.launcher.usage.UsageUi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class LauncherViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = AppPreferences(app)
    private val provider = InstalledAppsProvider(app)

    private val installed = MutableStateFlow<List<AppEntry>>(emptyList())
    private val defaults = MutableStateFlow<List<String>>(emptyList())

    val state: StateFlow<LauncherUiState> =
        combine(prefs.settings, installed, defaults) { settings, all, def ->
            val byPkg = all.associateBy { it.packageName }
            val order = settings.selectedApps ?: def
            LauncherUiState(
                settings = settings,
                homeApps = order.mapNotNull { byPkg[it] },
                allApps = all,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LauncherUiState())

    // ---------------- Screen time ----------------

    private val _usage = MutableStateFlow(UsageUi())
    val usage: StateFlow<UsageUi> = _usage
    private var usageJob: Job? = null

    /**
     * Recomputes today's usage and finalizes any finished days.
     * Called when the home screen starts and once per minute while it is visible.
     */
    fun refreshUsage() {
        if (usageJob?.isActive == true) return
        usageJob = viewModelScope.launch(Dispatchers.Default) {
            if (!prefs.settings.first().showUsage) return@launch
            val app = getApplication<Application>()
            val now = System.currentTimeMillis()
            val today = LocalDate.now().toEpochDay()

            val (storedStart, stored) = prefs.getUsageLog()
            val start = storedStart ?: (today - 6)   // first run: try the last week
            val log = stored.toMutableMap()
            var changed = storedStart == null

            val granted = UsageTracker.hasPermission(app)
            var todayMinutes: Int? = null
            if (granted) {
                // Finalize finished days we haven't recorded yet (Android keeps ~a week of events)
                for (day in maxOf(start, today - 9) until today) {
                    if (day !in log) {
                        UsageTracker.minutesForDay(app, day, now)?.let { log[day] = it; changed = true }
                    }
                }
                todayMinutes = UsageTracker.minutesForDay(app, today, now) ?: 0
            }
            if (log.keys.removeAll { it < today - 130 }) changed = true
            if (changed) prefs.saveUsageLog(start, log)

            // Grid restarts every 60 days
            val cycleStart = start + (maxOf(0L, today - start) / 60) * 60
            _usage.value = UsageUi(granted, todayMinutes, today, cycleStart, log)
        }
    }
    /** Clears the history and makes today box #1. No backfill of earlier days. */
    fun restartUsageTracking() {
        viewModelScope.launch(Dispatchers.Default) {
            usageJob?.cancelAndJoin()   // make sure an in-flight refresh can't write old data back
            prefs.saveUsageLog(LocalDate.now().toEpochDay(), emptyMap())
            refreshUsage()
        }
    }
    // ---------------- Apps ----------------

    /** Called from onStart so newly installed / removed apps show up. */
    fun refreshApps() {
        viewModelScope.launch(Dispatchers.Default) {
            installed.value = provider.launchableApps()
            defaults.value = provider.defaultPackages()
        }
    }

    fun saveApps(selected: Set<String>) {
        val s = state.value
        val kept = s.homeApps.map { it.packageName }.filter { it in selected }
        val added = s.allApps.map { it.packageName }.filter { it in selected && it !in kept }
        viewModelScope.launch { prefs.setApps(kept + added) }
    }

    fun moveApp(from: Int, to: Int) {
        val list = state.value.homeApps.map { it.packageName }.toMutableList()
        if (from !in list.indices || to !in list.indices) return
        list.add(to, list.removeAt(from))
        viewModelScope.launch { prefs.setApps(list) }
    }

    // ---------------- Settings ----------------

    fun setShowClock(v: Boolean) { viewModelScope.launch { prefs.setShowClock(v) } }
    fun setShowDate(v: Boolean) { viewModelScope.launch { prefs.setShowDate(v) } }
    fun setUse24Hour(v: Boolean) { viewModelScope.launch { prefs.setUse24Hour(v) } }
    fun setShowSeconds(v: Boolean) { viewModelScope.launch { prefs.setShowSeconds(v) } }
    fun setLayout(v: HomeLayout) { viewModelScope.launch { prefs.setLayout(v) } }
    fun setShowUsage(v: Boolean) {
        viewModelScope.launch {
            prefs.setShowUsage(v)
            if (v) refreshUsage()
        }
    }
}