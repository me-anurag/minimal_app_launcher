package com.minimal.launcher.launcher

import com.minimal.launcher.apps.AppEntry
import com.minimal.launcher.data.LauncherSettings

data class LauncherUiState(
    val settings: LauncherSettings = LauncherSettings(),
    val homeApps: List<AppEntry> = emptyList(),
    val allApps: List<AppEntry> = emptyList(),
)
