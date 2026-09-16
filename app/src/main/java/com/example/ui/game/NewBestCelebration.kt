package com.example.ui.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.random.Random

@Composable
fun NewBestCelebration(
    score: Int,
    onDismiss: () -> Unit
) {
    val particlesAnim = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        particlesAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(1200, easing = LinearEasing)
        )
        delay(1800)
        onDismiss()
    }

    val colors = listOf(
        Color(0xFFFFD54F), Color(0xFFFF5252), Color(0xFF00E5FF),
        Color(0xFF69F0AE), Color(0xFFE040FB), Color(0xFFFF6D00)
    )

    // Pre-calculate 40 random confetti particle trajectories
    val confettiList = remember {
        List(40) {
            val angle = Random.nextFloat() * 2f * Math.PI.toFloat()
            val speed = 200f + Random.nextFloat() * 400f
            val color = colors[Random.nextInt(colors.size)]
            val size = 8f + Random.nextFloat() * 12f
            Particle(angle, speed, color, size)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        // Confetti canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val progress = particlesAnim.value

            for (p in confettiList) {
                val dist = p.speed * progress
                val px = cx + kotlin.math.cos(p.angle) * dist
                val py = cy + kotlin.math.sin(p.angle) * dist + (progress * progress * 300f) // gravity
                val alpha = (1f - progress).coerceIn(0f, 1f)

                drawRect(
                    color = p.color.copy(alpha = alpha),
                    topLeft = Offset(px, py),
                    size = Size(p.size, p.size)
                )
            }
        }

        // Center Celebration Card
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF1E293B))
                .shadow(24.dp, RoundedCornerShape(24.dp))
                .padding(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFFFFE082), Color(0xFFFFB300), Color(0xFFFF8F00))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Crown Celebration",
                    tint = Color.White,
                    modifier = Modifier.size(52.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "NEW HIGH SCORE!",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFFFFD54F),
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "$score",
                fontSize = 42.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Tap to continue",
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.6f)
            )
        }
    }
}

private data class Particle(
    val angle: Float,
    val speed: Float,
    val color: Color,
    val size: Float
)
