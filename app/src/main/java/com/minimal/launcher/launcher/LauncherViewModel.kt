package com.minimal.launcher.launcher
import com.minimal.launcher.data.HomeLayout
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.minimal.launcher.apps.AppEntry
import com.minimal.launcher.apps.InstalledAppsProvider
import com.minimal.launcher.data.AppPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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
    fun setShowClock(v: Boolean) { viewModelScope.launch { prefs.setShowClock(v) } }
    fun setShowDate(v: Boolean) { viewModelScope.launch { prefs.setShowDate(v) } }
    fun setUse24Hour(v: Boolean) { viewModelScope.launch { prefs.setUse24Hour(v) } }
    fun setShowSeconds(v: Boolean) { viewModelScope.launch { prefs.setShowSeconds(v) } }
    fun setLayout(v: HomeLayout) { viewModelScope.launch { prefs.setLayout(v) } }
}
