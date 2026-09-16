package com.example.ui.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.ComboBannerEvent
import com.example.domain.model.FloatingScoreEvent
import com.example.domain.model.GameTheme
import kotlin.math.roundToInt

/**
 * High-energy 1-second auto-dismissing combo banner rendered directly
 * over the game board, inspired by top block puzzle hits (Block Blast, Woodoku).
 */
@Composable
fun ComboBoardOverlay(
    banner: ComboBannerEvent?,
    modifier: Modifier = Modifier
) {
    if (banner == null) return

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = true,
            enter = scaleIn(
                initialScale = 0.35f,
                animationSpec = spring(
                    dampingRatio = 0.52f,
                    stiffness = 650f
                )
            ) + fadeIn(tween(120)),
            exit = scaleOut(
                targetScale = 1.25f,
                animationSpec = tween(220, easing = FastOutSlowInEasing)
            ) + fadeOut(tween(180))
        ) {
            if (banner != null) {
                val bannerGradients = when {
                    banner.combo >= 5 -> listOf(Color(0xFFFF007A), Color(0xFFFF5E00), Color(0xFFFFD600))
                    banner.combo == 4 -> listOf(Color(0xFF7C3AED), Color(0xFFEC4899), Color(0xFFF59E0B))
                    banner.combo == 3 -> listOf(Color(0xFF0072FF), Color(0xFF00C6FF), Color(0xFF00FFA3))
                    banner.combo == 2 -> listOf(Color(0xFFFF8008), Color(0xFFFFC837))
                    else -> listOf(Color(0xFF10B981), Color(0xFF34D399))
                }

                val icon = when {
                    banner.combo >= 5 -> Icons.Default.Stars
                    banner.combo >= 4 -> Icons.Default.AutoAwesome
                    banner.combo >= 3 -> Icons.Default.ElectricBolt
                    else -> Icons.Default.LocalFireDepartment
                }

                Surface(
                    shape = RoundedCornerShape(26.dp),
                    color = Color(0xDD0F172A),
                    border = BorderStroke(
                        2.5.dp,
                        Brush.linearGradient(bannerGradients)
                    ),
                    shadowElevation = 20.dp,
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .shadow(24.dp, RoundedCornerShape(26.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color(0xEE1E293B),
                                        Color(0xF50F172A)
                                    )
                                )
                            )
                            .padding(horizontal = 24.dp, vertical = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Brush.linearGradient(bannerGradients)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = banner.title,
                                style = TextStyle(
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    letterSpacing = 1.2.sp,
                                    shadow = Shadow(
                                        color = bannerGradients.first(),
                                        offset = Offset(0f, 4f),
                                        blurRadius = 12f
                                    )
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Subtitle: Multiplier / Points
                        Text(
                            text = banner.subtitle,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFFFD54F),
                            letterSpacing = 0.5.sp
                        )
                    }
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
