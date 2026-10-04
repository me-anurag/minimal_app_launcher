package com.minimal.launcher.settings

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.minimal.launcher.ui.MinimalColors
import com.minimal.launcher.ui.MinimalTextButton
import com.minimal.launcher.usage.UsageTracker

@Composable
fun UsageSettingsScreen(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    onOpenCalendar: () -> Unit,
    onRestartTracking: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(UsageTracker.hasPermission(context)) }
    var confirmRestart by remember { mutableStateOf(false) }

    // Re-check when coming back from the system "Usage access" screen
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        granted = UsageTracker.hasPermission(context)
    }

    ScreenFrame("SCREEN TIME", onBack) {
        MinimalTextButton(
            "Usage tracker",
            { onToggle(!enabled) },
            trailing = if (enabled) "ON" else "OFF",
            fontSize = 20.sp,
        )
        MinimalTextButton(
            "Usage access",
            { UsageTracker.openAccessSettings(context) },
            trailing = if (granted) "GRANTED" else "NOT GRANTED",
            fontSize = 20.sp,
        )
        MinimalTextButton("Calendar", onOpenCalendar, fontSize = 20.sp)
        MinimalTextButton(
            text = if (confirmRestart) "Tap again to confirm" else "Start tracking from today",
            onClick = {
                if (confirmRestart) {
                    onRestartTracking()
                    confirmRestart = false
                } else {
                    confirmRestart = true
                }
            },
            dim = !confirmRestart,
            fontSize = 20.sp,
        )
        Text(
            "A day earns a flag only if screen time stays under 1 hour AND you press " +
                    "\"I am ready\" at 4:00 AM the next morning (the button is there for one minute). " +
                    "\"Start tracking from today\" clears the grid, the calendar and all flags.",
            color = MinimalColors.Dim,
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 16.dp),
        )
    }
}