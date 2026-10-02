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
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.max

@Composable
fun HomeScreen(
    state: LauncherUiState,
    nowMillis: Long,
    onAppClick: (AppEntry) -> Unit,
    onAllApps: () -> Unit,
    onSettings: () -> Unit,
) {
    val s = state.settings
    val twoColumns = s.layout == HomeLayout.TWO_COLUMNS

    val timeFmt = remember(s.use24Hour, s.showSeconds) {
        val pattern = (if (s.use24Hour) "HH:mm" else "h:mm") +
                (if (s.showSeconds) ":ss" else "") +
                (if (s.use24Hour) "" else " a")
        DateTimeFormatter.ofPattern(pattern)
    }
    val dateFmt = remember { DateTimeFormatter.ofPattern("EEEE, dd MMM") }
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
            // Centered over their own halves, matching the two equal app columns
            Row(Modifier.fillMaxWidth()) {
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    NavText("All Apps", onAllApps)
                }
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    NavText("Settings", onSettings)
                }
            }
        } else {
            // Left-aligned with the single app column (same 12dp text inset as app labels)
            Row(Modifier.fillMaxWidth()) {
                NavText("All Apps", onAllApps)
                NavText("Settings", onSettings)
            }
        }
        Spacer(Modifier.height(12.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF222222)))
        Spacer(Modifier.height(4.dp))

        // ---- 3. HOME APPS (only this area scrolls) ----
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            if (state.homeApps.isEmpty()) {
                Text(
                    "No apps selected.\nOpen Settings → Home Screen → Apps.",
                    color = MinimalColors.Dim,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(top = 16.dp, start = 12.dp),
                )
            } else if (twoColumns) {
                TwoColumnApps(state.homeApps, availableHeight = maxHeight, onAppClick = onAppClick)
            } else {
                VerticalApps(state.homeApps, onAppClick = onAppClick)
            }
        }
    }
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
    // Scrolls automatically only when the list is taller than the available space
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

    // Scrolling only when both columns are out of room
    LazyColumn(Modifier.fillMaxSize(), userScrollEnabled = rowsNeeded > rowsFit) {
        items(rowsNeeded, key = { left[it].packageName }) { i ->
            val rightApp = right.getOrNull(i)
            Row(Modifier.fillMaxWidth().height(rowHeight)) {
                // Two identical cells: equal weight, equal padding, no spacer or offset
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
            .padding(horizontal = 12.dp),          // identical inset everywhere
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