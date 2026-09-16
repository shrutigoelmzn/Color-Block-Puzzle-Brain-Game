package com.example.ui.game

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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.ClearEffectEvent
import com.example.domain.model.GameTheme
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

private enum class ParticleType {
    SHARD,
    STAR,
    ORB
}

private data class GridParticle(
    val normStartX: Float,
    val normStartY: Float,
    val velocityX: Float,
    val velocityY: Float,
    val gravity: Float,
    val size: Float,
    val color: Color,
    val spinSpeed: Float,
    val type: ParticleType
)

/**
 * High-satisfaction particle effects and visual pop-up banners rendered
 * directly on the game grid when the player clears multiple lines simultaneously.
 */
@Composable
fun MultiLineClearOverlay(
    clearEffect: ClearEffectEvent?,
    theme: GameTheme,
    modifier: Modifier = Modifier
) {
    if (clearEffect == null || !clearEffect.isMultiLine) return

    val totalLines = clearEffect.totalLines
    val animProgress = remember(clearEffect.id) { Animatable(0f) }
    val badgeScale = remember(clearEffect.id) { Animatable(0.3f) }
    val badgeFloatY = remember(clearEffect.id) { Animatable(18f) }

    LaunchedEffect(clearEffect.id) {
        // Particle progress
        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(1000, easing = LinearEasing)
        )
    }

    LaunchedEffect(clearEffect.id) {
        // Pop-up badge spring entrance
        badgeScale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = 0.48f,
                stiffness = 700f
            )
        )
    }

    LaunchedEffect(clearEffect.id) {
        // Pop-up badge slow float up
        badgeFloatY.animateTo(
            targetValue = -16f,
            animationSpec = tween(900, easing = FastOutSlowInEasing)
        )
    }

    // Palette for particles & glow based on lines cleared
    val palette = remember(totalLines) {
        when {
            totalLines >= 4 -> listOf(
                Color(0xFFFF0055), Color(0xFFFF5E00), Color(0xFFFFD700),
                Color(0xFFFFAA00), Color(0xFFFF3366), Color(0xFFFFFFFF)
            )
            totalLines == 3 -> listOf(
                Color(0xFF8B5CF6), Color(0xFFEC4899), Color(0xFFFBBF24),
                Color(0xFF38BDF8), Color(0xFFF43F5E), Color(0xFFFFFFFF)
            )
            else -> listOf(
                Color(0xFF00E5FF), Color(0xFF10B981), Color(0xFF34D399),
                Color(0xFF38BDF8), Color(0xFFFBBF24), Color(0xFFFFFFFF)
            )
        }
    }

    // Pre-generate ~65 particles bursting from cleared lines and intersection points
    val particles = remember(clearEffect.id) {
        val list = mutableListOf<GridParticle>()
        val random = Random(clearEffect.id)

        // 1. Burst from all cleared row cells
        for (r in clearEffect.rows) {
            for (c in 0 until 8 step 2) {
                val normX = (c + 0.5f) / 8f
                val normY = (r + 0.5f) / 8f
                val count = 2
                for (i in 0 until count) {
                    val angle = random.nextFloat() * 2f * Math.PI.toFloat()
                    val speed = 250f + random.nextFloat() * 450f
                    list.add(
                        GridParticle(
                            normStartX = normX,
                            normStartY = normY,
                            velocityX = cos(angle) * speed,
                            velocityY = sin(angle) * speed,
                            gravity = 180f + random.nextFloat() * 180f,
                            size = 8f + random.nextFloat() * 12f,
                            color = palette[random.nextInt(palette.size)],
                            spinSpeed = -500f + random.nextFloat() * 1000f,
                            type = ParticleType.entries[random.nextInt(ParticleType.entries.size)]
                        )
                    )
                }
            }
        }

        // 2. Burst from all cleared col cells
        for (c in clearEffect.cols) {
            for (r in 0 until 8 step 2) {
                val normX = (c + 0.5f) / 8f
                val normY = (r + 0.5f) / 8f
                val count = 2
                for (i in 0 until count) {
                    val angle = random.nextFloat() * 2f * Math.PI.toFloat()
                    val speed = 250f + random.nextFloat() * 450f
                    list.add(
                        GridParticle(
                            normStartX = normX,
                            normStartY = normY,
                            velocityX = cos(angle) * speed,
                            velocityY = sin(angle) * speed,
                            gravity = 180f + random.nextFloat() * 180f,
                            size = 8f + random.nextFloat() * 12f,
                            color = palette[random.nextInt(palette.size)],
                            spinSpeed = -500f + random.nextFloat() * 1000f,
                            type = ParticleType.entries[random.nextInt(ParticleType.entries.size)]
                        )
                    )
                }
            }
        }

        // 3. Dense explosive burst at intersection points (row crosses col)
        for (r in clearEffect.rows) {
            for (c in clearEffect.cols) {
                val normX = (c + 0.5f) / 8f
                val normY = (r + 0.5f) / 8f
                for (i in 0 until 8) {
                    val angle = random.nextFloat() * 2f * Math.PI.toFloat()
                    val speed = 350f + random.nextFloat() * 550f
                    list.add(
                        GridParticle(
                            normStartX = normX,
                            normStartY = normY,
                            velocityX = cos(angle) * speed,
                            velocityY = sin(angle) * speed,
                            gravity = 150f + random.nextFloat() * 160f,
                            size = 10f + random.nextFloat() * 14f,
                            color = palette[random.nextInt(palette.size)],
                            spinSpeed = -600f + random.nextFloat() * 1200f,
                            type = if (i % 2 == 0) ParticleType.STAR else ParticleType.SHARD
                        )
                    )
                }
            }
        }

        list.take(75)
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // 1. Grid Canvas for Beams, Shockwaves & Particles
        Canvas(modifier = Modifier.fillMaxSize()) {
            val progress = animProgress.value
            val alpha = (1f - progress).coerceIn(0f, 1f)
            val boardW = size.width
            val boardH = size.height
            val cellSize = boardW / 8f

            // A. Radiant line flashes along cleared rows
            val beamAlpha = (1f - progress * 1.8f).coerceIn(0f, 0.85f)
            if (beamAlpha > 0f) {
                for (r in clearEffect.rows) {
                    val y = r * cellSize
                    drawRoundRect(
                        brush = Brush.horizontalGradient(
                            listOf(
                                Color.White.copy(alpha = 0f),
                                palette[0].copy(alpha = beamAlpha),
                                Color.White.copy(alpha = beamAlpha),
                                palette[1].copy(alpha = beamAlpha),
                                Color.White.copy(alpha = 0f)
                            )
                        ),
                        topLeft = Offset(0f, y + 2f),
                        size = Size(boardW, cellSize - 4f),
                        cornerRadius = CornerRadius(8f, 8f)
                    )
                }

                // Radiant line flashes along cleared cols
                for (c in clearEffect.cols) {
                    val x = c * cellSize
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = 0f),
                                palette[0].copy(alpha = beamAlpha),
                                Color.White.copy(alpha = beamAlpha),
                                palette[1].copy(alpha = beamAlpha),
                                Color.White.copy(alpha = 0f)
                            )
                        ),
                        topLeft = Offset(x + 2f, 0f),
                        size = Size(cellSize - 4f, boardH),
                        cornerRadius = CornerRadius(8f, 8f)
                    )
                }
            }

            // B. Expanding Shockwave Rings from center of clear
            val shockwaveCenter = Offset(
                x = (clearEffect.centerCol + 0.5f) * cellSize,
                y = (clearEffect.centerRow + 0.5f) * cellSize
            )
            val ringProgress = (progress * 1.4f).coerceAtMost(1f)
            val ringRadius = ringProgress * (boardW * 0.65f)
            val ringAlpha = (1f - ringProgress).coerceIn(0f, 0.9f)
            if (ringAlpha > 0f) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            palette[0].copy(alpha = ringAlpha * 0.35f),
                            palette[1].copy(alpha = ringAlpha * 0.85f),
                            Color.White.copy(alpha = ringAlpha)
                        ),
                        center = shockwaveCenter,
                        radius = ringRadius.coerceAtLeast(1f)
                    ),
                    center = shockwaveCenter,
                    radius = ringRadius,
                    style = Stroke(width = 6f * (1f - ringProgress).coerceAtLeast(0.3f))
                )

                // Secondary subtle ring
                val secondRadius = ringRadius * 0.7f
                if (secondRadius > 0f) {
                    drawCircle(
                        color = Color.White.copy(alpha = ringAlpha * 0.6f),
                        center = shockwaveCenter,
                        radius = secondRadius,
                        style = Stroke(width = 3.5f)
                    )
                }
            }

            // C. Dynamic Flying Particles (Shards, Stars, Orbs)
            for (p in particles) {
                val startX = p.normStartX * boardW
                val startY = p.normStartY * boardH
                val currentX = startX + (p.velocityX * progress)
                val currentY = startY + (p.velocityY * progress) + (p.gravity * progress * progress)
                val pAlpha = (1f - progress * 1.15f).coerceIn(0f, 1f)

                if (pAlpha > 0f && currentX in -50f..(boardW + 50f) && currentY in -50f..(boardH + 50f)) {
                    val pSize = p.size * (1f - progress * 0.4f).coerceAtLeast(0.4f)
                    val pColor = p.color.copy(alpha = pAlpha)

                    when (p.type) {
                        ParticleType.STAR -> {
                            drawStarParticle(
                                center = Offset(currentX, currentY),
                                radius = pSize,
                                color = pColor,
                                rotation = p.spinSpeed * progress
                            )
                        }
                        ParticleType.SHARD -> {
                            rotate(
                                degrees = p.spinSpeed * progress,
                                pivot = Offset(currentX, currentY)
                            ) {
                                drawRoundRect(
                                    color = pColor,
                                    topLeft = Offset(currentX - pSize / 2f, currentY - pSize / 3f),
                                    size = Size(pSize, pSize * 0.65f),
                                    cornerRadius = CornerRadius(2f, 2f)
                                )
                            }
                        }
                        ParticleType.ORB -> {
                            drawCircle(
                                color = pColor,
                                radius = pSize / 2f,
                                center = Offset(currentX, currentY)
                            )
                            // Inner specular glint
                            drawCircle(
                                color = Color.White.copy(alpha = pAlpha),
                                radius = pSize / 5f,
                                center = Offset(currentX - pSize / 6f, currentY - pSize / 6f)
                            )
                        }
                    }
                }
            }
        }

        // 2. High-Energy Visual Pop-Up Badge on the Grid
        val badgeProgress = animProgress.value
        val badgeAlpha = when {
            badgeProgress < 0.75f -> 1f
            else -> ((1f - badgeProgress) / 0.25f).coerceIn(0f, 1f)
        }

        if (badgeAlpha > 0f) {
            val titleText = when (totalLines) {
                2 -> "DOUBLE CLEAR!"
                3 -> "TRIPLE CLEAR!"
                4 -> "QUAD CLEAR!"
                else -> "MEGA CLEAR!"
            }

            val bonusText = when (totalLines) {
                2 -> "+50% BONUS"
                3 -> "+120% BONUS"
                4 -> "+200% SUPER BONUS"
                else -> "+300% ULTRA BONUS"
            }

            val icon = when {
                totalLines >= 4 -> Icons.Default.Stars
                totalLines == 3 -> Icons.Default.ElectricBolt
                else -> Icons.Default.AutoAwesome
            }

            val bannerGradients = when {
                totalLines >= 4 -> listOf(Color(0xFFFF0055), Color(0xFFFF5E00), Color(0xFFFFD700))
                totalLines == 3 -> listOf(Color(0xFF8B5CF6), Color(0xFFEC4899), Color(0xFFFBBF24))
                else -> listOf(Color(0xFF00E5FF), Color(0xFF10B981), Color(0xFF34D399))
            }

            Box(
                modifier = Modifier
                    .offset { IntOffset(0, badgeFloatY.value.roundToInt()) }
                    .scale(badgeScale.value)
                    .alpha(badgeAlpha)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xF20B1329),
                    border = BorderStroke(
                        2.5.dp,
                        Brush.linearGradient(bannerGradients)
                    ),
                    shadowElevation = 24.dp,
                    modifier = Modifier
                        .shadow(28.dp, RoundedCornerShape(24.dp), ambientColor = bannerGradients[0], spotColor = bannerGradients[1])
                ) {
                    Column(
                        modifier = Modifier
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color(0xEE1E293B),
                                        Color(0xFA0F172A)
                                    )
                                )
                            )
                            .padding(horizontal = 22.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Title row with glowing badge icon
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Brush.linearGradient(bannerGradients)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = "Multi-Line Clear",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Text(
                                text = titleText,
                                style = TextStyle(
                                    color = Color.White,
                                    fontSize = 21.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.2.sp,
                                    shadow = Shadow(
                                        color = bannerGradients[0],
                                        offset = Offset(0f, 2f),
                                        blurRadius = 10f
                                    )
                                )
                            )
                        }

                        // Subtitle with score and bonus multiplier
                        Row(
                            modifier = Modifier
                                .padding(top = 6.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(bannerGradients[0].copy(alpha = 0.22f))
                                .padding(horizontal = 10.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "+${clearEffect.scoreGained} PTS",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = bannerGradients.last()
                            )

                            Text(
                                text = " • $bonusText",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.9f)
                            )

                            if (clearEffect.combo >= 2) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "🔥 ${clearEffect.combo}x",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFFFFD54F)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Draws a 4-pointed glowing sparkle star on the Canvas.
 */
private fun DrawScope.drawStarParticle(
    center: Offset,
    radius: Float,
    color: Color,
    rotation: Float
) {
    rotate(degrees = rotation, pivot = center) {
        val path = Path().apply {
            moveTo(center.x, center.y - radius)
            quadraticTo(center.x, center.y, center.x + radius, center.y)
            quadraticTo(center.x, center.y, center.x, center.y + radius)
            quadraticTo(center.x, center.y, center.x - radius, center.y)
            quadraticTo(center.x, center.y, center.x, center.y - radius)
            close()
        }
        drawPath(path = path, color = color)
    }
}
