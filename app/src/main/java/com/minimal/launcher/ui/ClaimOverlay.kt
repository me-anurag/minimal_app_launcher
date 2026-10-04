package com.minimal.launcher.ui

import android.content.Context
import android.media.MediaPlayer
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minimal.launcher.R
import com.minimal.launcher.usage.ClaimResult
import com.minimal.launcher.usage.UsageTracker
import com.minimal.launcher.usage.UsageUi
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.ZoneId
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

private val Gold = Color(0xFFFFD54F)
private const val BUTTON_ASPECT = 512f / 164f      // size of btn_i_am_ready.png

/**
 * Drawn over the whole home screen:
 *  - the full-width "I am ready" bar at the bottom, only 04:00 - 04:01
 *  - sound + golden party poppers when a flag is earned
 */
@Composable
fun ClaimOverlay(
    nowMillis: Long,
    usage: UsageUi,
    claim: ClaimResult?,
    celebrate: Flow<Unit>,
    onClaim: () -> Unit,
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var burstId by remember { mutableIntStateOf(0) }
    var bursting by remember { mutableStateOf(false) }

    LaunchedEffect(celebrate) {
        celebrate.collect {
            burstId++
            bursting = true
            WinSound.play(context)
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    Box(Modifier.fillMaxSize()) {
        if (UsageTracker.inClaimWindow(nowMillis)) {
            ClaimBar(nowMillis, usage, claim, onClaim, Modifier.align(Alignment.BottomCenter))
        }
        if (bursting) {
            GoldenPoppers(burstId) { bursting = false }   // drawn last = on top of everything
        }
    }
}

@Composable
private fun ClaimBar(
    nowMillis: Long,
    usage: UsageUi,
    claim: ClaimResult?,
    onClaim: () -> Unit,
    modifier: Modifier,
) {
    val yesterday = Instant.ofEpochMilli(nowMillis)
        .atZone(ZoneId.systemDefault()).toLocalDate().toEpochDay() - 1
    val result = claim?.takeIf { it.day == yesterday }
    val alreadyRewarded = yesterday in usage.rewarded
    val message = result?.text ?: if (alreadyRewarded) "Flag claimed" else null
    val earned = result?.earned == true || (result == null && alreadyRewarded)

    Box(
        modifier
            .fillMaxWidth()
            .background(Color.Black)                                   // solid, covers the apps below
            .clickable(                                                // swallow taps so nothing underneath opens
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
            )
            .navigationBarsPadding()
            .padding(horizontal = 4.dp, vertical = 12.dp),
    ) {
        // Message and button share the same height, so the bar never jumps
        Box(
            Modifier.fillMaxWidth().aspectRatio(BUTTON_ASPECT),
            contentAlignment = Alignment.Center,
        ) {
            if (message != null) {
                Text(
                    message,
                    color = if (earned) Gold else MinimalColors.Text,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Light,
                    textAlign = TextAlign.Center,
                )
            } else {
                PressableImageButton(onClaim)
            }
        }
    }
}

@Composable
private fun PressableImageButton(onClick: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.94f else 1f, tween(80), label = "press")
    val haptic = LocalHapticFeedback.current

    Image(
        painter = painterResource(R.drawable.btn_i_am_ready),
        contentDescription = "I am ready",
        contentScale = ContentScale.FillWidth,
        alpha = if (pressed) 0.75f else 1f,                    // darkens while pressed
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }  // sinks in while pressed
            .clickable(interactionSource = source, indication = null, role = Role.Button) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            },
    )
}

// ---------------------------------------------------------------------------------------
// Sound
// ---------------------------------------------------------------------------------------

private object WinSound {
    fun play(context: Context) {
        try {
            val player = MediaPlayer.create(
                context.applicationContext, R.raw.winner_game_sound
            ) ?: return
            player.setOnCompletionListener { it.release() }
            player.setOnErrorListener { mp, _, _ -> mp.release(); true }
            player.start()
        } catch (_: Exception) {
        }
    }
}

// ---------------------------------------------------------------------------------------
// Golden party poppers: two cannons (bottom-left / bottom-right), two waves
// ---------------------------------------------------------------------------------------

private const val TOTAL_SECONDS = 4.0f
private const val FADE_START = 3.0f

private val GoldPalette = listOf(
    Color(0xFFFFD700), Color(0xFFFFC107), Color(0xFFFFB300), Color(0xFFFFE082),
    Color(0xFFFFF1A8), Color(0xFFD4A017), Color(0xFFFFCA28),
)

private class Particle(
    val x0: Float, val y0: Float,       // launch point (px)
    val vx: Float, val vy: Float,       // launch velocity (px/s)
    val drag: Float, val gravity: Float,
    val delay: Float,
    val pw: Float, val ph: Float,       // piece size (px)
    val rot0: Float, val rotSpeed: Float,
    val flipSpeed: Float, val flipPhase: Float,
    val color: Color,
    val round: Boolean,
)

private fun makeParticles(w: Float, h: Float, d: Float): List<Particle> {
    val rnd = Random(System.nanoTime())
    val list = ArrayList<Particle>()
    for (wave in 0 until 2) {
        val waveDelay = wave * 0.30f
        for (side in 0 until 2) {
            val fromLeft = side == 0
            repeat(48) {
                // left cannon fires up and to the right, right cannon up and to the left
                val deg = if (fromLeft) -(35f + rnd.nextFloat() * 50f)
                else -(95f + rnd.nextFloat() * 50f)
                val rad = Math.toRadians(deg.toDouble())
                val speed = (650f + rnd.nextFloat() * 800f) * d
                list += Particle(
                    x0 = if (fromLeft) w * 0.03f else w * 0.97f,
                    y0 = h - 100f * d,
                    vx = (cos(rad) * speed).toFloat(),
                    vy = (sin(rad) * speed).toFloat(),
                    drag = 1.9f + rnd.nextFloat() * 1.8f,
                    gravity = (620f + rnd.nextFloat() * 200f) * d,
                    delay = waveDelay + rnd.nextFloat() * 0.08f,
                    pw = (6f + rnd.nextFloat() * 6f) * d,
                    ph = (3f + rnd.nextFloat() * 3f) * d,
                    rot0 = rnd.nextFloat() * 360f,
                    rotSpeed = (rnd.nextFloat() - 0.5f) * 900f,
                    flipSpeed = 6f + rnd.nextFloat() * 10f,
                    flipPhase = rnd.nextFloat() * 6.28f,
                    color = GoldPalette[rnd.nextInt(GoldPalette.size)],
                    round = rnd.nextInt(5) == 0,
                )
            }
        }
    }
    return list
}

@Composable
private fun GoldenPoppers(burstId: Int, onFinished: () -> Unit) {
    val density = LocalDensity.current
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        val particles = remember(burstId) { makeParticles(widthPx, heightPx, density.density) }
        var time by remember(burstId) { mutableFloatStateOf(0f) }

        // Runs only while the confetti is flying (about 4 seconds)
        LaunchedEffect(burstId) {
            val start = withFrameNanos { it }
            while (true) {
                val elapsed = (withFrameNanos { it } - start) / 1_000_000_000f
                time = elapsed
                if (elapsed >= TOTAL_SECONDS) break
            }
            onFinished()
        }

        Canvas(Modifier.fillMaxSize()) {
            val t = time
            val fade = if (t > FADE_START) {
                (1f - (t - FADE_START) / (TOTAL_SECONDS - FADE_START)).coerceIn(0f, 1f)
            } else 1f

            for (p in particles) {
                val tt = t - p.delay
                if (tt < 0f) continue

                // Motion with air drag + gravity (closed form, no per-frame integration)
                val decay = exp(-p.drag * tt)
                val x = p.x0 + p.vx / p.drag * (1f - decay)
                val terminal = p.gravity / p.drag
                val y = p.y0 + terminal * tt + (p.vy - terminal) / p.drag * (1f - decay)
                if (y > size.height + 40f) continue

                // Tumbling: the piece flips edge-on and face-on, so it glints
                val flip = abs(cos(p.flipPhase + p.flipSpeed * tt))
                val color = p.color.copy(alpha = fade * (0.7f + 0.3f * flip))

                rotate(degrees = p.rot0 + p.rotSpeed * tt, pivot = Offset(x, y)) {
                    if (p.round) {
                        drawCircle(color, radius = p.pw / 2f, center = Offset(x, y))
                    } else {
                        val hh = p.ph * (0.25f + 0.75f * flip)
                        drawRect(
                            color = color,
                            topLeft = Offset(x - p.pw / 2f, y - hh / 2f),
                            size = Size(p.pw, hh),
                        )
                    }
                }
            }
        }
    }
}