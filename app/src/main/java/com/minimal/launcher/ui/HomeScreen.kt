package com.minimal.launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minimal.launcher.apps.AppEntry
import com.minimal.launcher.data.HomeLayout
import com.minimal.launcher.launcher.LauncherUiState
import com.minimal.launcher.usage.UsageUi
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.max

@Composable
fun HomeScreen(
    state: LauncherUiState,
    usage: UsageUi,
    nowMillis: Long,
    onAppClick: (AppEntry) -> Unit,
    onAllApps: () -> Unit,
    onSettings: () -> Unit,
    onGrantUsageAccess: () -> Unit,
) {
    val s = state.settings
    val twoColumns = s.layout == HomeLayout.TWO_COLUMNS

    val timeFmt = remember(s.use24Hour, s.showSeconds) {
        val pattern = (if (s.use24Hour) "HH:mm" else "h:mm") +
                (if (s.showSeconds) ":ss" else "") +
                (if (s.use24Hour) "" else " a")
        DateTimeFormatter.ofPattern(pattern)
    }
    val dateFmt = remember { DateTimeFormatter.ofPattern("EEE '·' dd MMM") }
    val dt = Instant.ofEpochMilli(nowMillis).atZone(ZoneId.systemDefault())

    Column(
        Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(horizontal = 24.dp),
    ) {
        Spacer(Modifier.height(32.dp))

        // ---- 1. CLOCK + DATE (fixed) ----
        if (s.showClock) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    dt.format(timeFmt),
                    color = MinimalColors.Text,
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Light,
                )
                if (s.showDate) {
                    Text(
                        dt.format(dateFmt).uppercase(),
                        color = MinimalColors.Dim,
                        fontSize = 16.sp,
                        letterSpacing = 2.sp,
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }

        // ---- 2. ACTION BUTTONS (fixed, same position in both layouts) ----
        if (twoColumns) {
            Row(Modifier.fillMaxWidth()) {
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    NavText("All Apps", onAllApps)
                }
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    NavText("Settings", onSettings)
                }
            }
        } else {
            Row(Modifier.fillMaxWidth()) {
                NavText("All Apps", onAllApps)
                NavText("Settings", onSettings)
            }
        }
        Spacer(Modifier.height(12.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF222222)))
        Spacer(Modifier.height(4.dp))

        // ---- 3. APP AREA (only this area scrolls) ----
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val areaHeight = maxHeight   // read it here, before entering Row/Box scopes

            if (twoColumns) {
                if (state.homeApps.isEmpty()) EmptyHint()
                else TwoColumnApps(state.homeApps, availableHeight = areaHeight, onAppClick = onAppClick)
            } else {
                Row(Modifier.fillMaxSize()) {
                    Box(Modifier.weight(1f).fillMaxHeight()) {
                        if (state.homeApps.isEmpty()) EmptyHint()
                        else VerticalApps(state.homeApps, onAppClick)
                    }
                    // Screen-time panel lives on the empty right side (Vertical layout only)
                    if (s.showUsage) {
                        UsagePanel(
                            usage = usage,
                            availableHeight = areaHeight,
                            onGrantAccess = onGrantUsageAccess,
                            modifier = Modifier.padding(start = 8.dp, top = 4.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyHint() {
    Text(
        "No apps selected.\nOpen Settings → Home Screen → Apps.",
        color = MinimalColors.Dim,
        fontSize = 16.sp,
        modifier = Modifier.padding(top = 16.dp, start = 12.dp),
    )
}

@Composable
private fun NavText(text: String, onClick: () -> Unit) {
    Text(
        text,
        color = MinimalColors.Dim,
        fontSize = 18.sp,
        letterSpacing = 1.sp,
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = 12.dp, vertical = 12.dp),
    )
}

/** Row height follows the user's font size, so layouts adapt on every device. */
@Composable
private fun rememberRowHeight(): Dp = with(LocalDensity.current) { 26.sp.toDp() } + 16.dp

// ---------------- Layout 1: Vertical ----------------

@Composable
private fun VerticalApps(apps: List<AppEntry>, onAppClick: (AppEntry) -> Unit) {
    val rowHeight = rememberRowHeight()
    LazyColumn(Modifier.fillMaxSize()) {
        items(apps, key = { it.packageName }) { app ->
            AppCell(app, Modifier.fillMaxWidth().height(rowHeight)) { onAppClick(app) }
        }
    }
}

// ---------------- Layout 2: Two Columns ----------------

@Composable
private fun TwoColumnApps(
    apps: List<AppEntry>,
    availableHeight: Dp,
    onAppClick: (AppEntry) -> Unit,
) {
    val rowHeight = rememberRowHeight()
    val rowsFit = max(1, (availableHeight / rowHeight).toInt())

    // left = first ceil(N/2), right = the rest (order preserved)
    val rowsNeeded = (apps.size + 1) / 2
    val left = apps.take(rowsNeeded)
    val right = apps.drop(rowsNeeded)

    LazyColumn(Modifier.fillMaxSize(), userScrollEnabled = rowsNeeded > rowsFit) {
        items(rowsNeeded, key = { left[it].packageName }) { i ->
            val rightApp = right.getOrNull(i)
            Row(Modifier.fillMaxWidth().height(rowHeight)) {
                AppCell(left[i], Modifier.weight(1f)) { onAppClick(left[i]) }
                AppCell(rightApp, Modifier.weight(1f)) { rightApp?.let(onAppClick) }
            }
        }
    }
}

// ---------------- Shared cell ----------------

@Composable
private fun AppCell(app: AppEntry?, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .fillMaxHeight()
            .clickable(
                enabled = app != null,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (app != null) {
            Text(
                app.label,
                color = MinimalColors.Text,
                fontSize = 20.sp,
                lineHeight = 26.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}