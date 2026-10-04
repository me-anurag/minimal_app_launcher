package com.minimal.launcher.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minimal.launcher.R
import com.minimal.launcher.usage.UsageTracker
import com.minimal.launcher.usage.UsageUi

private const val ROWS = 15
private const val COLS = 4

private val EmptyBorder = Color(0xFF3A3A3A)
private val TodayBorder = Color(0xFF8A8A8A)
//private val FilledWhite = Color(0xFFEDEDED)

// Flag artwork: 137 x 144 px, the pole sits 23% from its left edge
private const val FLAG_ASPECT = 137f / 144f
private const val FLAG_POLE_X = 0.232f

private enum class DayState { FILLED, EMPTY, TODAY }

@Composable
fun UsagePanel(
    usage: UsageUi,
    availableHeight: Dp,
    onGrantAccess: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hGap = 4.dp
    val vGap = 8.dp          // taller vertical gap so the flags have room
    val headerHeight = 56.dp
    val spacing = 12.dp

    // Box size adapts to the height we have, so the grid always fits the screen
    val box = ((availableHeight - headerHeight - spacing - vGap * (ROWS - 1)) / ROWS)
        .coerceIn(8.dp, 20.dp)

    Column(modifier, horizontalAlignment = Alignment.End) {
        // ---- Timer ----
        Box(Modifier.height(headerHeight), contentAlignment = Alignment.TopEnd) {
            if (usage.hasPermission) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        formatMinutes(usage.todayMinutes),
                        color = MinimalColors.Text,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Light,
                    )
                    Text(
                        "TODAY",
                        color = MinimalColors.Dim,
                        fontSize = 11.sp,
                        letterSpacing = 2.sp,
                    )
                }
            } else {
                Text(
                    "Allow usage\naccess",
                    color = MinimalColors.Dim,
                    fontSize = 14.sp,
                    textAlign = TextAlign.End,
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        role = Role.Button,
                        onClick = onGrantAccess,
                    ),
                )
            }
        }
        Spacer(Modifier.height(spacing))

        // ---- 4 columns x 15 rows, filled top-to-bottom then left-to-right ----
        Row(horizontalArrangement = Arrangement.spacedBy(hGap)) {
            repeat(COLS) { col ->
                Column(verticalArrangement = Arrangement.spacedBy(vGap)) {
                    repeat(ROWS) { row ->
                        DayBox(stateFor(usage, col * ROWS + row), box)
                    }
                }
            }
        }
    }
}

private fun stateFor(usage: UsageUi, index: Int): DayState {
    if (usage.today == 0L) return DayState.EMPTY       // not loaded yet
    val day = usage.cycleStart + index
    return when {
        day == usage.today -> DayState.TODAY            // still in progress
        day > usage.today -> DayState.EMPTY             // future
        else -> {
            val minutes = usage.days[day]
            if (minutes != null && minutes < UsageTracker.LIMIT_MINUTES) DayState.FILLED
            else DayState.EMPTY                         // 1h+ or no data
        }
    }
}

@Composable
private fun DayBox(state: DayState, size: Dp) {
    // No fill: a conquered day is the same outlined box, with the flag planted in it
    val border = if (state == DayState.TODAY) TodayBorder else EmptyBorder

    Box(Modifier.size(size).border(1.dp, border)) {
        if (state == DayState.FILLED) {
            // The pole's foot sits at the centre of the box; the flag rises above it
            val flagH = size * 0.9f
            val flagW = flagH * FLAG_ASPECT
            Image(
                painter = painterResource(R.drawable.flag_conquered),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .requiredSize(flagW, flagH)
                    .offset(
                        x = size / 2 - flagW * FLAG_POLE_X,
                        y = size / 2 - flagH,
                    ),
            )
        }
    }
}

private fun formatMinutes(minutes: Int?): String = when {
    minutes == null -> "–"
    minutes < 60 -> "${minutes}m"
    else -> "${minutes / 60}h ${minutes % 60}m"
}