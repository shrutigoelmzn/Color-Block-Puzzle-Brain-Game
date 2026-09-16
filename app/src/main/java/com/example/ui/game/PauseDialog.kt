package com.example.ui.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.domain.model.GameTheme

@Composable
fun PauseDialog(
    theme: GameTheme,
    score: Int = 0,
    linesCleared: Int = 0,
    timeRemaining: Int? = null,
    soundEnabled: Boolean,
    hapticsEnabled: Boolean,
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onToggleSound: () -> Unit,
    onToggleHaptics: () -> Unit,
    onHome: () -> Unit
) {
    Dialog(
        onDismissRequest = onResume,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .padding(24.dp)
                .widthIn(max = 380.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = theme.cardBg,
            border = BorderStroke(1.dp, theme.cardBorder),
            tonalElevation = 6.dp,
            shadowElevation = 12.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "GAME PAUSED",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = theme.textColorPrimary,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Stats preview card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = theme.cellEmptyBg,
                    border = BorderStroke(1.dp, theme.cardBorder.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp, horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "SCORE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = theme.textColorSecondary
                            )
                            Text(
                                text = "$score",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                color = theme.accentColor
                            )
                        }

                        Box(
                            modifier = Modifier
                                .height(28.dp)
                                .width(1.dp)
                                .background(theme.cardBorder)
                        )

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "LINES",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = theme.textColorSecondary
                            )
                            Text(
                                text = "$linesCleared",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                color = theme.textColorPrimary
                            )
                        }

                        if (timeRemaining != null) {
                            Box(
                                modifier = Modifier
                                    .height(28.dp)
                                    .width(1.dp)
                                    .background(theme.cardBorder)
                            )

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "TIME",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = theme.textColorSecondary
                                )
                                Text(
                                    text = "${timeRemaining}s",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (timeRemaining <= 15) Color(0xFFFF5252) else theme.textColorPrimary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Sound & Haptic Quick Toggles Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(theme.cellEmptyBg)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledTonalIconButton(
                            onClick = onToggleSound,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (soundEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                                contentDescription = "Sound Toggle",
                                tint = if (soundEnabled) Color(0xFF10B981) else Color.Gray,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (soundEnabled) "Sound ON" else "Sound OFF",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = theme.textColorPrimary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .height(24.dp)
                            .width(1.dp)
                            .background(theme.cardBorder)
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledTonalIconButton(
                            onClick = onToggleHaptics,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.TouchApp,
                                contentDescription = "Haptics Toggle",
                                tint = if (hapticsEnabled) Color(0xFFFF9800) else Color.Gray,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (hapticsEnabled) "Vibe ON" else "Vibe OFF",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = theme.textColorPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Resume button (M3 primary fill)
                Button(
                    onClick = onResume,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = theme.accentColor,
                        contentColor = Color.White
                    )
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "RESUME",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Restart button
                OutlinedButton(
                    onClick = onRestart,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, theme.cardBorder)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        tint = theme.textColorPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "RESTART",
                        style = MaterialTheme.typography.labelLarge,
                        color = theme.textColorPrimary
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Quit to Home
                OutlinedButton(
                    onClick = onHome,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, theme.cardBorder)
                ) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = null,
                        tint = theme.textColorSecondary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "MAIN MENU",
                        style = MaterialTheme.typography.labelLarge,
                        color = theme.textColorSecondary
                    )
                }
            }
        }
    }
}
