package com.minimal.launcher.launcher

import android.content.Intent
import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.minimal.launcher.apps.AppLauncher
import com.minimal.launcher.settings.AboutScreen
import com.minimal.launcher.settings.AllAppsScreen
import com.minimal.launcher.settings.AppSelectionScreen
import com.minimal.launcher.settings.AppearanceSettingsScreen
import com.minimal.launcher.settings.ClockSettingsScreen
import com.minimal.launcher.settings.NotificationsScreen
import com.minimal.launcher.settings.OrderScreen
import com.minimal.launcher.settings.SettingsScreen
import com.minimal.launcher.settings.SystemScreen
import com.minimal.launcher.ui.HomeScreen
import com.minimal.launcher.ui.MinimalTheme
import com.minimal.launcher.ui.rememberNow

enum class Screen {
    HOME, ALL_APPS, SETTINGS, APPS, ORDER, CLOCK, APPEARANCE, NOTIFICATIONS, SYSTEM, ABOUT
}

private fun Screen.back(): Screen = when (this) {
    Screen.HOME, Screen.SETTINGS, Screen.ALL_APPS -> Screen.HOME
    else -> Screen.SETTINGS
}

class LauncherActivity : ComponentActivity() {
    private val vm: LauncherViewModel by viewModels()
    private var homeSignal by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
        )
        setContent { MinimalTheme { LauncherApp(vm, homeSignal) } }
    }

    override fun onStart() {
        super.onStart()
        vm.refreshApps()
    }

    // Home button pressed while we're already running -> go back to the home screen
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == Intent.ACTION_MAIN && intent.hasCategory(Intent.CATEGORY_HOME)) {
            homeSignal++
        }
    }
}

@Composable
private fun LauncherApp(vm: LauncherViewModel, homeSignal: Int) {
    val state by vm.state.collectAsStateWithLifecycle()
    var screen by rememberSaveable { mutableStateOf(Screen.HOME) }
    val context = LocalContext.current

    LaunchedEffect(homeSignal) { if (homeSignal > 0) screen = Screen.HOME }
    // A launcher must never "exit" on Back
    BackHandler { screen = screen.back() }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        when (screen) {
            Screen.HOME -> {
                val now by rememberNow(state.settings.showSeconds)
                HomeScreen(
                    state = state,
                    nowMillis = now,
                    onAppClick = { AppLauncher.launch(context, it.packageName) },
                    onAllApps = { screen = Screen.ALL_APPS },
                    onSettings = { screen = Screen.SETTINGS },
                )
            }
            Screen.ALL_APPS -> AllAppsScreen(
                allApps = state.allApps,
                onLaunch = { AppLauncher.launch(context, it.packageName) },
                onBack = { screen = Screen.HOME },
            )
            Screen.SETTINGS -> SettingsScreen(
                onOpen = { screen = it },
                onBack = { screen = Screen.HOME },
            )
            Screen.APPS -> AppSelectionScreen(
                allApps = state.allApps,
                selectedPackages = state.homeApps.map { it.packageName }.toSet(),
                onSave = { vm.saveApps(it); screen = Screen.SETTINGS },
                onBack = { screen = Screen.SETTINGS },
            )
            Screen.ORDER -> OrderScreen(
                apps = state.homeApps,
                onMove = vm::moveApp,
                onBack = { screen = Screen.SETTINGS },
            )
            Screen.CLOCK -> ClockSettingsScreen(
                settings = state.settings,
                onShowClock = vm::setShowClock,
                onShowDate = vm::setShowDate,
                onUse24Hour = vm::setUse24Hour,
                onShowSeconds = vm::setShowSeconds,
                onBack = { screen = Screen.SETTINGS },
            )
            Screen.APPEARANCE -> AppearanceSettingsScreen { screen = Screen.SETTINGS }
            Screen.NOTIFICATIONS -> NotificationsScreen { screen = Screen.SETTINGS }
            Screen.SYSTEM -> SystemScreen { screen = Screen.SETTINGS }
            Screen.ABOUT -> AboutScreen { screen = Screen.SETTINGS }
        }
    }
}