package com.minimal.launcher.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minimal.launcher.data.LauncherSettings
import com.minimal.launcher.launcher.Screen
import com.minimal.launcher.ui.MinimalColors
import com.minimal.launcher.ui.MinimalTextButton

@Composable
fun ScreenFrame(title: String, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(horizontal = 32.dp, vertical = 16.dp),
    ) {
        MinimalTextButton("‹ Back", onClick = onBack, dim = true, fontSize = 18.sp)
        Spacer(Modifier.height(16.dp))
        Text(title, color = MinimalColors.Dim, fontSize = 14.sp, letterSpacing = 3.sp)
        Spacer(Modifier.height(16.dp))
        content()
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = MinimalColors.Text, fontSize = 20.sp)
        Text(value, color = MinimalColors.Dim, fontSize = 20.sp)
    }
}

@Composable
fun SettingsScreen(onOpen: (Screen) -> Unit, onBack: () -> Unit) {
    ScreenFrame("SETTINGS", onBack) {
        MinimalTextButton("Home Screen", { onOpen(Screen.HOME_SETTINGS) })
        MinimalTextButton("Clock", { onOpen(Screen.CLOCK) })
        MinimalTextButton("Screen Time", { onOpen(Screen.USAGE) })
        MinimalTextButton("Appearance", { onOpen(Screen.APPEARANCE) })
        MinimalTextButton("Notifications", { onOpen(Screen.NOTIFICATIONS) })
        MinimalTextButton("System", { onOpen(Screen.SYSTEM) })
        MinimalTextButton("About", { onOpen(Screen.ABOUT) })
    }
}

@Composable
fun HomeSettingsScreen(onOpen: (Screen) -> Unit, onBack: () -> Unit) {
    ScreenFrame("HOME SCREEN", onBack) {
        MinimalTextButton("Apps", { onOpen(Screen.APPS) })
        MinimalTextButton("Order", { onOpen(Screen.ORDER) })
        MinimalTextButton("Layout", { onOpen(Screen.LAYOUT) })
    }
}

@Composable
fun ClockSettingsScreen(
    settings: LauncherSettings,
    onShowClock: (Boolean) -> Unit,
    onShowDate: (Boolean) -> Unit,
    onUse24Hour: (Boolean) -> Unit,
    onShowSeconds: (Boolean) -> Unit,
    onBack: () -> Unit,
) {
    fun onOff(v: Boolean) = if (v) "ON" else "OFF"
    ScreenFrame("CLOCK", onBack) {
        MinimalTextButton("Show clock", { onShowClock(!settings.showClock) }, trailing = onOff(settings.showClock), fontSize = 20.sp)
        MinimalTextButton("Show date", { onShowDate(!settings.showDate) }, trailing = onOff(settings.showDate), fontSize = 20.sp)
        MinimalTextButton("24-hour format", { onUse24Hour(!settings.use24Hour) }, trailing = onOff(settings.use24Hour), fontSize = 20.sp)
        MinimalTextButton("Show seconds", { onShowSeconds(!settings.showSeconds) }, trailing = onOff(settings.showSeconds), fontSize = 20.sp)
    }
}

@Composable
fun AppearanceSettingsScreen(onBack: () -> Unit) {
    // v1: fixed minimal defaults, shown read-only
    ScreenFrame("APPEARANCE", onBack) {
        InfoRow("Background", "Black")
        InfoRow("Text", "White")
        InfoRow("Font size", "Medium")
        InfoRow("Animations", "OFF")
        InfoRow("App icons", "OFF")
        InfoRow("Wallpaper", "OFF")
    }
}

@Composable
fun NotificationsScreen(onBack: () -> Unit) {
    ScreenFrame("NOTIFICATIONS", onBack) {
        Text("Notification counts are not part of v1.", color = MinimalColors.Dim, fontSize = 16.sp)
    }
}

@Composable
fun SystemScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    ScreenFrame("SYSTEM", onBack) {
        MinimalTextButton("Change default launcher", { openDefaultLauncherSettings(context) }, fontSize = 20.sp)
    }
}

@Composable
fun AboutScreen(onBack: () -> Unit) {
    ScreenFrame("ABOUT", onBack) {
        InfoRow("Minimal Launcher", "v1.0")
        Text(
            "Make the phone functional without making it visually distracting.",
            color = MinimalColors.Dim,
            fontSize = 16.sp,
        )
    }
}

private fun openDefaultLauncherSettings(context: Context) {
    val attempts = listOf(
        Intent(Settings.ACTION_HOME_SETTINGS),
        Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS),
        Intent(Settings.ACTION_SETTINGS),
    )
    for (intent in attempts) {
        try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            return
        } catch (_: ActivityNotFoundException) {
        } catch (_: SecurityException) {
        }
    }
}
