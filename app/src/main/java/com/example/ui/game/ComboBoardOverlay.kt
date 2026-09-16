package com.example.ui.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.ComboBannerEvent
import com.example.domain.model.FloatingScoreEvent
import com.example.domain.model.GameTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Data specification for radiant sparkles bursting around the combo banner.
 */
private data class ComboSparkle(
    val angleRad: Float,
    val startDistance: Float,
    val endDistance: Float,
    val maxRadius: Float,
    val spinDegrees: Float,
    val colorIndex: Int,
    val isDiamond: Boolean = true
)

/**
 * Generates an energetic constellation of 16 sparkles distributed around the combo banner.
 */
private fun generateComboSparkles(): List<ComboSparkle> {
    val sparkles = mutableListOf<ComboSparkle>()
    val count = 16
    for (i in 0 until count) {
        val baseAngle = (i.toFloat() / count) * 2f * PI.toFloat()
        // Add subtle natural jitter to angle and distance
        val angleJitter = ((i * 37) % 15 - 7) * 0.03f
        val angle = baseAngle + angleJitter
        val startDist = 55f + ((i * 17) % 25)
        val endDist = 135f + ((i * 31) % 45)
        val maxR = 9f + ((i * 19) % 10)
        val spin = if (i % 2 == 0) 180f + ((i * 23) % 120) else -(180f + ((i * 23) % 120))
        sparkles.add(
            ComboSparkle(
                angleRad = angle,
                startDistance = startDist,
                endDistance = endDist,
                maxRadius = maxR,
                spinDegrees = spin,
                colorIndex = i % 4,
                isDiamond = i % 5 != 0
            )
        )
    }
    return sparkles
}

/**
 * High-energy bouncy combo banner with radiating animated sparkles,
 * inspired by top block puzzle games (Block Blast, Woodoku).
 */
@Composable
fun ComboBoardOverlay(
    banner: ComboBannerEvent?,
    modifier: Modifier = Modifier
) {
    if (banner == null) return

    val sparkles = remember(banner.id) { generateComboSparkles() }

    // Bouncy spring scale with overshoot & rebound
    val bouncyScale = remember(banner.id) { Animatable(0.22f) }
    // Energetic entrance tilt wobble
    val wobbleRotation = remember(banner.id) { Animatable(-9f) }
    // Bounce drop-in offset
    val bounceOffsetY = remember(banner.id) { Animatable(-22f) }
    // Radiating sparkle progress (0f -> 1f)
    val sparkleProgress = remember(banner.id) { Animatable(0f) }
    // Overall banner alpha for smooth dismiss
    val bannerAlpha = remember(banner.id) { Animatable(1f) }

    LaunchedEffect(banner.id) {
        launch {
            // Spring animation with lively overshoot (damping 0.38 gives that signature bouncy cartoon punch)
            bouncyScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = 0.38f,
                    stiffness = 380f
                )
            )
        }
        launch {
            wobbleRotation.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = 0.45f,
                    stiffness = 420f
                )
            )
        }
        launch {
            bounceOffsetY.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = 0.40f,
                    stiffness = 400f
                )
            )
        }
        launch {
            sparkleProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(950, easing = FastOutSlowInEasing)
            )
        }
        // Smooth dismiss toward end of banner lifecycle (around 800ms)
        delay(780)
        launch {
            bannerAlpha.animateTo(0f, tween(200, easing = LinearEasing))
        }
        launch {
            bouncyScale.animateTo(1.15f, tween(200, easing = FastOutSlowInEasing))
        }
    }

    val bannerGradients = when {
        banner.combo >= 5 -> listOf(Color(0xFFFF007A), Color(0xFFFF5E00), Color(0xFFFFD600))
        banner.combo == 4 -> listOf(Color(0xFF7C3AED), Color(0xFFEC4899), Color(0xFFF59E0B))
        banner.combo == 3 -> listOf(Color(0xFF0072FF), Color(0xFF00C6FF), Color(0xFF00FFA3))
        banner.combo == 2 -> listOf(Color(0xFFFF8008), Color(0xFFFFC837))
        else -> listOf(Color(0xFF10B981), Color(0xFF34D399))
    }

    val sparkleColors = when {
        banner.combo >= 5 -> listOf(Color(0xFFFFD700), Color(0xFFFF007A), Color(0xFFFFFFFF), Color(0xFFFF9100))
        banner.combo == 4 -> listOf(Color(0xFFFFD700), Color(0xFFEC4899), Color(0xFFFFFFFF), Color(0xFF8B5CF6))
        banner.combo == 3 -> listOf(Color(0xFF00E5FF), Color(0xFF00FFA3), Color(0xFFFFFFFF), Color(0xFFFFD700))
        banner.combo == 2 -> listOf(Color(0xFFFFD700), Color(0xFFFF9800), Color(0xFFFFFFFF), Color(0xFFFFEA00))
        else -> listOf(Color(0xFF34D399), Color(0xFF6EE7B7), Color(0xFFFFFFFF), Color(0xFFFFD700))
    }

    val icon = when {
        banner.combo >= 5 -> Icons.Default.Stars
        banner.combo >= 4 -> Icons.Default.AutoAwesome
        banner.combo >= 3 -> Icons.Default.ElectricBolt
        else -> Icons.Default.LocalFireDepartment
    }

    val sProg = sparkleProgress.value

    Box(
        modifier = modifier
            .alpha(bannerAlpha.value),
        contentAlignment = Alignment.Center
    ) {
        // Sparkle Particles & Luminous Energy Shockwave Canvas
        Canvas(
            modifier = Modifier.size(360.dp)
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)

            // Luminous shockwave energy ring expanding outward
            if (sProg > 0f) {
                val ringRadius = 60f + (sProg * 105f)
                val ringAlpha = ((1f - sProg) * 0.65f).coerceIn(0f, 1f)
                drawCircle(
                    color = bannerGradients.first().copy(alpha = ringAlpha),
                    radius = ringRadius,
                    center = center,
                    style = Stroke(width = 3.dp.toPx() * (1f - sProg * 0.5f))
                )
            }

            // Radiating 4-pointed sparkle stars
            val sparklePath = Path()
            for (sparkle in sparkles) {
                val dist = sparkle.startDistance + sProg * (sparkle.endDistance - sparkle.startDistance)
                // Position expanding along angle
                val sx = center.x + cos(sparkle.angleRad) * dist
                val sy = center.y + sin(sparkle.angleRad) * dist

                // Twinkle alpha: fade in rapidly, sustain, then fade out
                val alpha = if (sProg < 0.2f) {
                    (sProg / 0.2f)
                } else {
                    ((1f - sProg) / 0.8f)
                }.coerceIn(0f, 1f)

                if (alpha <= 0.01f) continue

                val color = sparkleColors[sparkle.colorIndex % sparkleColors.size].copy(alpha = alpha)
                val currentRadius = sparkle.maxRadius * (if (sProg < 0.25f) sProg / 0.25f else 1f - (sProg - 0.25f) * 0.4f)
                val currentRotation = (sparkle.angleRad * 180f / PI.toFloat()) + (sProg * sparkle.spinDegrees)

                rotate(degrees = currentRotation, pivot = Offset(sx, sy)) {
                    sparklePath.reset()
                    val pinch = currentRadius * 0.22f

                    // 4-pointed diamond star
                    sparklePath.moveTo(sx, sy - currentRadius)
                    sparklePath.quadraticTo(sx + pinch, sy - pinch, sx + currentRadius, sy)
                    sparklePath.quadraticTo(sx + pinch, sy + pinch, sx, sy + currentRadius)
                    sparklePath.quadraticTo(sx - pinch, sy + pinch, sx - currentRadius, sy)
                    sparklePath.quadraticTo(sx - pinch, sy - pinch, sx, sy - currentRadius)
                    sparklePath.close()

                    drawPath(sparklePath, color = color)

                    // Brilliant specular center glint
                    drawCircle(
                        color = Color.White.copy(alpha = alpha * 0.9f),
                        radius = (currentRadius * 0.28f).coerceAtLeast(1.5f),
                        center = Offset(sx, sy)
                    )
                }
            }
        }

        // Bouncy Combo Banner Card
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = Color(0xEE0B1220),
            border = BorderStroke(
                2.5.dp,
                Brush.linearGradient(bannerGradients)
            ),
            shadowElevation = 24.dp,
            modifier = Modifier
                .offset { IntOffset(0, bounceOffsetY.value.roundToInt()) }
                .scale(bouncyScale.value)
                .rotate(wobbleRotation.value)
                .padding(horizontal = 20.dp)
                .shadow(28.dp, RoundedCornerShape(26.dp))
        ) {
            Column(
                modifier = Modifier
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xF01E293B),
                                Color(0xF80F172A)
                            )
                        )
                    )
                    .padding(horizontal = 26.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(bannerGradients)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = banner.title,
                        style = TextStyle(
                            fontSize = 25.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            letterSpacing = 1.3.sp,
                            shadow = Shadow(
                                color = bannerGradients.first(),
                                offset = Offset(0f, 4f),
                                blurRadius = 14f
                            )
                        )
                    )
                }

                Spacer(modifier = Modifier.height(5.dp))

                // Subtitle: Multiplier / Points with energetic highlight
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color(0xFFFFD54F),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = banner.subtitle,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFFFD54F),
                        letterSpacing = 0.6.sp
                    )
                }
            }
        }
    }
}

/**
 * Floating score popup animating upward from the line clear epicenter.
 */
@Composable
fun FloatingScoreItem(
    event: FloatingScoreEvent,
    boardWidthPx: Float,
    boardHeightPx: Float
) {
    val animProgress = remember { Animatable(0f) }

    LaunchedEffect(event.id) {
        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(1100, easing = FastOutSlowInEasing)
        )
    }

    val p = animProgress.value
    val offsetY = -p * 90f // floats upward
    val alpha = (1f - (p - 0.4f) / 0.6f).coerceIn(0f, 1f)
    val scale = if (p < 0.2f) {
        0.6f + (p / 0.2f) * 0.55f
    } else {
        1.15f - (p - 0.2f) * 0.18f
    }

    val startX = event.normalizedX * boardWidthPx
    val startY = event.normalizedY * boardHeightPx

    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    x = (startX - 80f).roundToInt(),
                    y = (startY + offsetY).roundToInt()
                )
            }
            .scale(scale)
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = event.text,
            style = TextStyle(
                fontSize = if (event.isCombo) 20.sp else 17.sp,
                fontWeight = FontWeight.Black,
                color = if (event.isCombo) Color(0xFFFFD54F).copy(alpha = alpha) else Color.White.copy(alpha = alpha),
                shadow = Shadow(
                    color = if (event.isCombo) Color(0xFFFF6D00).copy(alpha = alpha) else Color.Black.copy(alpha = alpha * 0.8f),
                    offset = Offset(0f, 4f),
                    blurRadius = 8f
                )
            )
        )
    }
}
