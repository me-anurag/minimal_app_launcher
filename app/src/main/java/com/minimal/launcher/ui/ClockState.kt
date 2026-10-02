package com.minimal.launcher.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay

/**
 * Current time in millis, updated efficiently:
 *  - minute ticks come from the system (ACTION_TIME_TICK), only while the screen is STARTED
 *  - a 1s loop runs ONLY if seconds are enabled AND the screen is visible
 * Nothing runs while another app is in the foreground.
 */
@Composable
fun rememberNow(showSeconds: Boolean): State<Long> {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val now = remember { mutableLongStateOf(System.currentTimeMillis()) }

    DisposableEffect(owner) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, i: Intent?) {
                now.longValue = System.currentTimeMillis()
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    now.longValue = System.currentTimeMillis()
                    ContextCompat.registerReceiver(
                        context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED
                    )
                }
                Lifecycle.Event.ON_STOP -> runCatching { context.unregisterReceiver(receiver) }
                else -> Unit
            }
        }
        owner.lifecycle.addObserver(observer)
        onDispose {
            owner.lifecycle.removeObserver(observer)
            runCatching { context.unregisterReceiver(receiver) }
        }
    }

    if (showSeconds) {
        LaunchedEffect(owner) {
            owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    val t = System.currentTimeMillis()
                    now.longValue = t
                    delay(1000L - t % 1000L)
                }
            }
        }
    }
    return now
}
