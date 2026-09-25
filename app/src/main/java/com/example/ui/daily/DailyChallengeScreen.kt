package com.example.ui.daily

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.GameMode
import com.example.ui.game.GameViewModel
import com.example.ui.theme.LocalThemeIsDark

@Composable
fun DailyChallengeScreen(
    viewModel: GameViewModel,
    onStartGame: (GameMode) -> Unit,
    onNavigateBack: () -> Unit
) {
    val isDark = LocalThemeIsDark.current
    val rawTheme by viewModel.activeTheme.collectAsState()
    val activeTheme = remember(rawTheme, isDark) { rawTheme.forMode(isDark) }
    val dailyChallenge by viewModel.dailyChallenge.collectAsState()
    val dailyStreak by viewModel.dailyStreak.collectAsState()
    var showCompletedPopup by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(activeTheme.bgGradientStart, activeTheme.bgGradientEnd)
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = activeTheme.textColorPrimary
                    )
                }

                Text(
                    text = "DAILY CHALLENGE",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = activeTheme.textColorPrimary
                )

                Spacer(modifier = Modifier.size(48.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // DAILY CHALLENGE CARD
            if (dailyChallenge != null) {
                val challenge = dailyChallenge!!
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = activeTheme.cardBg),
                    border = BorderStroke(1.dp, activeTheme.cardBorder),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = challenge.title.uppercase(),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Black,
                                color = if (activeTheme.isDark) Color(0xFFFFB300) else Color(0xFFC2410C)
                            )
                            if (challenge.isCompleted) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Completed",
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "DONE",
                                        color = Color(0xFF10B981),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = challenge.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = activeTheme.textColorSecondary,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Targets Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(activeTheme.cellEmptyBg)
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "TARGET SCORE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = activeTheme.textColorSecondary
                                )
                                Text(
                                    "${challenge.targetScore}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = activeTheme.textColorPrimary
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "TARGET LINES",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = activeTheme.textColorSecondary
                                )
                                Text(
                                    "${challenge.targetLines}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = activeTheme.textColorPrimary
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "REWARD",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = activeTheme.textColorSecondary
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.MonetizationOn,
                                        contentDescription = null,
                                        tint = Color(0xFFFFB300),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        "${challenge.rewardCoins}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (activeTheme.isDark) Color(0xFFFFD54F) else Color(0xFFB45309)
                                    )
                                }
                            }
                        }

                        if (challenge.isCompleted) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFF10B981).copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.35f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarMonth,
                                        contentDescription = null,
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "Today's Challenge Completed! 🎉",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF10B981)
                                        )
                                        Text(
                                            text = "Come back tomorrow for a new challenge.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = activeTheme.textColorSecondary
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = {
                                if (challenge.isCompleted) {
                                    showCompletedPopup = true
                                } else {
                                    onStartGame(GameMode.CHALLENGE)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (challenge.isCompleted) Color(0xFF10B981) else Color(0xFF10B981),
                                contentColor = Color.White
                            )
                        ) {
                            Icon(
                                imageVector = if (challenge.isCompleted) Icons.Default.CheckCircle else Icons.Default.PlayArrow,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (challenge.isCompleted) "COMPLETED TODAY ✓" else "START CHALLENGE",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            if (showCompletedPopup) {
                AlertDialog(
                    onDismissRequest = { showCompletedPopup = false },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(36.dp)
                        )
                    },
                    title = {
                        Text(
                            text = "Come Back Tomorrow!",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = activeTheme.textColorPrimary
                        )
                    },
                    text = {
                        Text(
                            text = "You've successfully finished today's daily challenge! Come back tomorrow for a brand new challenge with fresh coin rewards.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = activeTheme.textColorSecondary
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = { showCompletedPopup = false },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                        ) {
                            Text("GOT IT", fontWeight = FontWeight.Bold)
                        }
                    },
                    containerColor = activeTheme.cardBg,
                    shape = RoundedCornerShape(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 7-DAY STREAK TRACK
            Text(
                text = "DAILY LOGIN STREAK",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = activeTheme.textColorPrimary
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (dailyStreak != null) {
                val streak = dailyStreak!!
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = activeTheme.cardBg),
                    border = BorderStroke(1.dp, activeTheme.cardBorder),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Current Streak: ${streak.currentStreak} Days 🔥",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (activeTheme.isDark) Color(0xFFFFB300) else Color(0xFFC2410C)
                            )
                            Text(
                                text = "Best: ${streak.bestStreak} Days",
                                style = MaterialTheme.typography.bodySmall,
                                color = activeTheme.textColorSecondary
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // 7 Days icons row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            for (day in 1..7) {
                                val isReached = streak.currentStreak >= day
                                val isToday = streak.currentStreak + 1 == day
                                val reward = streak.getRewardForDay(day)

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isReached) Color(0xFF10B981)
                                                else if (isToday && !streak.hasClaimedToday) Color(0xFFFFB300)
                                                else activeTheme.cellEmptyBg
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isReached) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        } else {
                                            Text(
                                                text = "+$reward",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isToday) Color(0xFF3E1F00) else activeTheme.textColorSecondary
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Day $day",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = activeTheme.textColorSecondary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Claim Button
                        Button(
                            onClick = { viewModel.claimStreakReward() },
                            enabled = !streak.hasClaimedToday,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFFB300),
                                contentColor = Color(0xFF3E1F00),
                                disabledContainerColor = activeTheme.cardBorder,
                                disabledContentColor = activeTheme.textColorSecondary
                            )
                        ) {
                            Text(
                                text = if (streak.hasClaimedToday) "CLAIMED FOR TODAY ✓" else "CLAIM TODAY'S REWARD",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
