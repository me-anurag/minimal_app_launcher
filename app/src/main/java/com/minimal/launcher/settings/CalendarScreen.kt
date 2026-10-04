package com.minimal.launcher.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minimal.launcher.R
import com.minimal.launcher.ui.MinimalColors
import com.minimal.launcher.usage.UsageTracker
import com.minimal.launcher.usage.UsageUi
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.TextStyle as DateTextStyle
import java.time.temporal.WeekFields
import java.util.Locale

@Composable
fun CalendarScreen(usage: UsageUi, onBack: () -> Unit) {
    var monthsBack by rememberSaveable { mutableIntStateOf(0) }   // 0 = current month
    val locale = Locale.getDefault()
    val now = remember { ZonedDateTime.now(ZoneId.systemDefault()) }
    val todayDay = now.toLocalDate().toEpochDay()
    val ym = remember(monthsBack) { YearMonth.from(now).minusMonths(monthsBack.toLong()) }

    // Yesterday is only settled after the 04:00 claim minute has passed
    val resolved = now.toLocalTime() >= LocalTime.of(UsageTracker.CLAIM_HOUR, UsageTracker.CLAIM_MINUTE + 1)
    val anchor = todayDay - if (resolved) 1 else 2
    var current = 0
    var d = anchor
    while (d in usage.rewarded) { current++; d-- }
    val best = bestStreak(usage.rewarded)

    val firstDow = WeekFields.of(locale).firstDayOfWeek
    val lead = (ym.atDay(1).dayOfWeek.value - firstDow.value + 7) % 7
    val length = ym.lengthOfMonth()
    val weekRows = (lead + length + 6) / 7

    ScreenFrame("SCREEN TIME CALENDAR", onBack) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
            StatRow("Current streak", "$current ${if (current == 1) "day" else "days"}")
            StatRow("Best streak", "$best ${if (best == 1) "day" else "days"}")
            StatRow("Flags earned", "${usage.rewarded.size}")
            Spacer(Modifier.height(16.dp))

            // ---- Month header with ‹ › ----
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "‹",
                    color = MinimalColors.Text,
                    fontSize = 26.sp,
                    modifier = Modifier.clickable { monthsBack++ }.padding(horizontal = 16.dp, vertical = 8.dp),
                )
                Text(
                    "${ym.month.getDisplayName(DateTextStyle.FULL, locale)} ${ym.year}".uppercase(),
                    color = MinimalColors.Text,
                    fontSize = 16.sp,
                    letterSpacing = 2.sp,
                )
                Text(
                    "›",
                    color = if (monthsBack > 0) MinimalColors.Text else Color(0xFF333333),
                    fontSize = 26.sp,
                    modifier = Modifier
                        .clickable(enabled = monthsBack > 0) { monthsBack-- }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            Spacer(Modifier.height(8.dp))

            // ---- Weekday header ----
            Row(Modifier.fillMaxWidth()) {
                for (k in 0 until 7) {
                    val dow: DayOfWeek = firstDow.plus(k.toLong())
                    Text(
                        dow.getDisplayName(DateTextStyle.NARROW, locale),
                        color = MinimalColors.Dim,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Spacer(Modifier.height(4.dp))

            // ---- Weeks ----
            for (r in 0 until weekRows) {
                Row(Modifier.fillMaxWidth()) {
                    for (c in 0 until 7) {
                        val dayNum = r * 7 + c - lead + 1
                        DayCell(
                            date = if (dayNum in 1..length) ym.atDay(dayNum) else null,
                            usage = usage,
                            todayDay = todayDay,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            Text(
                "A flag means: under 1 hour that day and you pressed \"I am ready\" at 4 AM. " +
                        "Small numbers are the screen time of each finished day.",
                color = MinimalColors.Dim,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 16.dp),
            )
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = MinimalColors.Dim, fontSize = 16.sp)
        Text(value, color = MinimalColors.Text, fontSize = 16.sp)
    }
}

@Composable
private fun DayCell(date: LocalDate?, usage: UsageUi, todayDay: Long, modifier: Modifier) {
    val epoch = date?.toEpochDay()
    val isToday = epoch == todayDay
    Column(
        modifier
            .height(58.dp)
            .padding(2.dp)
            .then(if (isToday) Modifier.border(1.dp, Color(0xFF8A8A8A)) else Modifier),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (date != null && epoch != null) {
            Text(
                "${date.dayOfMonth}",
                color = if (isToday) MinimalColors.Text else MinimalColors.Dim,
                fontSize = 13.sp,
            )
            when {
                epoch in usage.rewarded -> Image(
                    painter = painterResource(R.drawable.flag_conquered),
                    contentDescription = "Flag earned",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(width = 24.dp, height = 26.dp),
                )
                isToday -> usage.todayMinutes?.let { SmallMinutes(UsageTracker.formatDuration(it)) }
                epoch < todayDay -> usage.days[epoch]?.let { SmallMinutes(UsageTracker.formatDuration(it)) }
            }
        }
    }
}

@Composable
private fun SmallMinutes(text: String) {
    Text(text, color = Color(0xFF5E5E5E), fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp))
}

private fun bestStreak(days: Set<Long>): Int {
    var best = 0
    var run = 0
    var prev = Long.MIN_VALUE
    for (day in days.sorted()) {
        run = if (day == prev + 1) run + 1 else 1
        if (run > best) best = run
        prev = day
    }
    return best
}