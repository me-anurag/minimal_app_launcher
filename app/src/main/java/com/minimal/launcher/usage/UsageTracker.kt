package com.minimal.launcher.usage

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** What the UI needs to draw the timer, the grid and the calendar. */
data class UsageUi(
    val hasPermission: Boolean = true,
    /** Minutes used today so far; null = not loaded yet. */
    val todayMinutes: Int? = null,
    val today: Long = 0L,          // epoch day of "today"
    val cycleStart: Long = 0L,     // epoch day of box #0 in the current 60-day cycle
    val days: Map<Long, Int> = emptyMap(),   // finished days -> minutes used
    val rewarded: Set<Long> = emptySet(),    // days with an earned flag
)

/** Result of pressing the 4 AM button, shown in place of the button. */
data class ClaimResult(val day: Long, val text: String, val earned: Boolean = false)

object UsageTracker {
    /** A day must stay under this many minutes to be eligible for a flag. */
    const val LIMIT_MINUTES = 60

    /** The claim button is visible from CLAIM_HOUR:CLAIM_MINUTE:00 to :59 only. */
    const val CLAIM_HOUR = 4
    const val CLAIM_MINUTE = 0

    // UsageEvents.Event types (numeric so they work on every API level)
    private const val FOREGROUND = 1      // MOVE_TO_FOREGROUND / ACTIVITY_RESUMED
    private const val BACKGROUND = 2      // MOVE_TO_BACKGROUND / ACTIVITY_PAUSED
    private const val SCREEN_OFF = 16     // SCREEN_NON_INTERACTIVE
    private const val SHUTDOWN = 26       // DEVICE_SHUTDOWN

    fun inClaimWindow(nowMillis: Long): Boolean {
        val t = Instant.ofEpochMilli(nowMillis).atZone(ZoneId.systemDefault())
        return t.hour == CLAIM_HOUR && t.minute == CLAIM_MINUTE
    }

    fun formatDuration(minutes: Int?): String = when {
        minutes == null -> "–"
        minutes < 60 -> "${minutes}m"
        else -> "${minutes / 60}h ${minutes % 60}m"
    }

    fun hasPermission(context: Context): Boolean {
        val ops = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ops.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            ops.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun openAccessSettings(context: Context) {
        val attempts = listOf(
            Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                .setData(Uri.parse("package:${context.packageName}")),
            Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS),
        )
        for (intent in attempts) {
            try {
                context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                return
            } catch (_: Exception) {
            }
        }
    }

    /**
     * Minutes spent in other apps on the given day (00:00 until midnight, or until now for today).
     * Returns null when Android has no event data for that day.
     * The launcher itself is not counted, like Digital Wellbeing.
     */
    fun minutesForDay(context: Context, epochDay: Long, nowMillis: Long): Int? {
        val zone = ZoneId.systemDefault()
        val date = LocalDate.ofEpochDay(epochDay)
        val start = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = minOf(date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli(), nowMillis)
        if (end <= start) return null

        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val events = usm.queryEvents(start, end)
        val e = UsageEvents.Event()
        val open = HashMap<String, Long>()   // activity -> time it came to the foreground
        var total = 0L
        var anyEvent = false
        val own = context.packageName

        while (events.hasNextEvent()) {
            events.getNextEvent(e)
            anyEvent = true
            when (e.eventType) {
                SCREEN_OFF, SHUTDOWN -> {
                    open.values.forEach { total += maxOf(0L, e.timeStamp - it) }
                    open.clear()
                }
                FOREGROUND, BACKGROUND -> {
                    val pkg = e.packageName
                    if (pkg == null || pkg == own) continue
                    val key = pkg + "/" + (e.className ?: "")
                    if (e.eventType == FOREGROUND) {
                        open.getOrPut(key) { e.timeStamp }
                    } else {
                        open.remove(key)?.let { total += maxOf(0L, e.timeStamp - it) }
                    }
                }
            }
        }
        open.values.forEach { total += maxOf(0L, end - it) }

        return if (anyEvent) (total / 60_000L).toInt() else null
    }
}