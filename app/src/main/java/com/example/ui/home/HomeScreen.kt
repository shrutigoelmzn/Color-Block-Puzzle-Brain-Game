package com.example.ui.home

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FormatPaint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ads.BannerAdView
import com.example.domain.model.AppThemeMode
import com.example.domain.model.GameMode
import com.example.ui.game.GameViewModel
import com.example.ui.settings.SettingsDialog
import com.example.ui.theme.LocalThemeIsDark

@Composable
fun HomeScreen(
    viewModel: GameViewModel,
    onStartGame: (GameMode) -> Unit,
    onNavigateThemes: () -> Unit,
    onNavigateDaily: () -> Unit,
    onNavigateAchievements: () -> Unit,
    onNavigateStats: () -> Unit
) {
    val isDark = LocalThemeIsDark.current
    val rawTheme by viewModel.activeTheme.collectAsState()
    val activeTheme = remember(rawTheme, isDark) { rawTheme.forMode(isDark) }
    val themeMode by viewModel.themeMode.collectAsState()

    val stats by viewModel.stats.collectAsState()
    val leaderboard by viewModel.leaderboard.collectAsState()
    val dailyChallenge by viewModel.dailyChallenge.collectAsState()
    val dailyStreak by viewModel.dailyStreak.collectAsState()
    val timedLevelNum by viewModel.timedLevel.collectAsState()
    val currentTimedLevel = remember(timedLevelNum) {
        com.example.domain.engine.TimedLevelGenerator.getLevel(timedLevelNum)
    }

    var selectedMode by remember { mutableStateOf(GameMode.CLASSIC) }
    var showSettings by remember { mutableStateOf(false) }

    // Pulsing play button animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "playPulse"
    )

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
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // TOP BAR: Coins Chip, Theme Mode Quick Switch, Settings
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Coins Chip (M3 Surface)
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = activeTheme.cardBg,
                    border = BorderStroke(1.dp, activeTheme.cardBorder),
                    shadowElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.MonetizationOn,
                            contentDescription = "Coins",
                            tint = Color(0xFFFFB300),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${stats.coins}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = activeTheme.textColorPrimary
                        )
                    }
                }

                // Action Icons (Theme switch & Settings)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Quick Light / Dark toggle button
                    IconButton(
                        onClick = {
                            val nextMode = if (isDark) AppThemeMode.LIGHT else AppThemeMode.DARK
                            viewModel.setThemeMode(nextMode)
                        }
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = activeTheme.cardBg,
                            border = BorderStroke(1.dp, activeTheme.cardBorder),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                                    contentDescription = "Toggle Light/Dark Theme",
                                    tint = if (isDark) Color(0xFFFFB300) else Color(0xFF4F46E5),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Ad Inspector Button for Testing Mediation (InMobi & Unity Ads)
                    val homeContext = androidx.compose.ui.platform.LocalContext.current
                    IconButton(
                        onClick = {
                            com.example.ads.AdManager.openAdInspector(homeContext)
                        }
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = activeTheme.cardBg,
                            border = BorderStroke(1.dp, Color(0xFF10B981)),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Test Mediation (Ad Inspector)",
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Settings Button
                    IconButton(onClick = { showSettings = true }) {
                        Surface(
                            shape = CircleShape,
                            color = activeTheme.cardBg,
                            border = BorderStroke(1.dp, activeTheme.cardBorder),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Settings",
                                    tint = activeTheme.textColorPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Quick Debug Banner for Ad Inspector
            val currentContext = androidx.compose.ui.platform.LocalContext.current
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        com.example.ads.AdManager.openAdInspector(currentContext)
                    },
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF10B981).copy(alpha = 0.15f),
                border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.7f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Test Mediation: Tap to open Ad Inspector",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = activeTheme.textColorPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // HERO BRANDING EMBLEM
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFFFFB300), Color(0xFFFF6D00))
                        )
                    )
                    .shadow(12.dp, RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Game Emblem",
                    tint = Color.White,
                    modifier = Modifier.size(50.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "COLOR BLOCK",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = activeTheme.textColorPrimary,
                letterSpacing = 1.sp
            )

            Text(
                text = "PUZZLE • BRAIN GAME",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = activeTheme.accentColor,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(18.dp))

            // BEST SCORE BANNER (M3 Elevated Card)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = activeTheme.cardBg),
                border = BorderStroke(1.dp, activeTheme.cardBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp, horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "BEST SCORE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (activeTheme.isDark) Color(0xFFFFB300) else Color(0xFFB45309),
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "${stats.bestScore}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black,
                            color = activeTheme.textColorPrimary
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "TIMED RECORD",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = activeTheme.accentColor,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "${stats.bestTimedScore}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = activeTheme.accentColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // BANNER AD 1: Below Best Score
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = activeTheme.cardBg.copy(alpha = 0.95f),
                border = BorderStroke(0.5.dp, activeTheme.cardBorder.copy(alpha = 0.5f)),
                shadowElevation = 2.dp
            ) {
                BannerAdView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    backgroundColor = Color.Transparent
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // GAME MODE SELECTOR PILLS
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = activeTheme.cardBg,
                border = BorderStroke(1.dp, activeTheme.cardBorder),
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    GameMode.values().forEach { mode ->
                        val isSelected = selectedMode == mode
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (isSelected) activeTheme.accentColor
                                    else Color.Transparent
                                )
                                .clickable { selectedMode = mode },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = mode.displayName.uppercase(),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) activeTheme.onAccentColor else activeTheme.textColorSecondary
                            )
                        }
                    }
                }
            }

            // Mode Details / Progression Preview Card
            if (selectedMode == GameMode.TIMED) {
                Spacer(modifier = Modifier.height(12.dp))
                val diffColor = Color(currentTimedLevel.difficulty.colorHex)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = activeTheme.cardBg),
                    border = BorderStroke(1.dp, diffColor.copy(alpha = 0.45f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = diffColor.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = currentTimedLevel.difficulty.label.uppercase(),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Black,
                                        color = diffColor
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "LEVEL ${currentTimedLevel.levelNumber}: ${currentTimedLevel.title}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = activeTheme.textColorPrimary
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Target: ${currentTimedLevel.targetScore} pts • ${currentTimedLevel.targetLines} lines in ${currentTimedLevel.timeLimitSeconds}s",
                                style = MaterialTheme.typography.bodySmall,
                                color = activeTheme.textColorSecondary
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFFFB300).copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "+${currentTimedLevel.rewardCoins} 🪙",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (activeTheme.isDark) Color(0xFFFFD54F) else Color(0xFFB45309)
                            )
                        }
                    }
                }
            } else if (selectedMode == GameMode.CHALLENGE && dailyChallenge != null) {
                Spacer(modifier = Modifier.height(12.dp))
                val challenge = dailyChallenge!!
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = activeTheme.cardBg),
                    border = BorderStroke(1.dp, if (challenge.isCompleted) Color(0xFF10B981) else Color(0xFFFF9800).copy(alpha = 0.45f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = challenge.title.uppercase(),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Black,
                                color = if (challenge.isCompleted) Color(0xFF10B981) else Color(0xFFFF9800)
                            )
                            if (challenge.isCompleted) {
                                Text(
                                    text = "DONE FOR TODAY ✓",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF10B981)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = if (challenge.isCompleted) "Completed! Come back tomorrow for a new challenge."
                            else "Goal: ${challenge.targetScore} pts • ${challenge.targetLines} lines • ${challenge.targetCombos}x combo",
                            style = MaterialTheme.typography.bodySmall,
                            color = activeTheme.textColorSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // BIG "PLAY NOW" GLOWING BUTTON
            Button(
                onClick = {
                    if (selectedMode == GameMode.CHALLENGE && dailyChallenge?.isCompleted == true) {
                        onNavigateDaily()
                    } else {
                        onStartGame(selectedMode)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .scale(pulseScale),
                shape = RoundedCornerShape(22.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF10B981)
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "PLAY ${selectedMode.displayName.uppercase()}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // DAILY CHALLENGE & STREAK QUICK CARD
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .clickable { onNavigateDaily() },
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = activeTheme.cardBg),
                border = BorderStroke(1.dp, activeTheme.cardBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF10B981).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column {
                            Text(
                                text = "Daily Challenge",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = activeTheme.textColorPrimary
                            )
                            Text(
                                text = if (dailyChallenge?.isCompleted == true) "Completed today! ⭐"
                                else "Streak: ${dailyStreak?.currentStreak ?: 0} Days 🔥",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (activeTheme.isDark) Color(0xFFFFB300) else Color(0xFFC2410C),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = activeTheme.textColorSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // HIGH SCORE LEADERBOARD (Persisted across sessions)
            LeaderboardSection(
                leaderboard = leaderboard,
                theme = activeTheme
            )

            Spacer(modifier = Modifier.height(14.dp))

            // BANNER AD 2: Below Leaderboard Score
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = activeTheme.cardBg.copy(alpha = 0.95f),
                border = BorderStroke(0.5.dp, activeTheme.cardBorder.copy(alpha = 0.5f)),
                shadowElevation = 2.dp
            ) {
                BannerAdView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    backgroundColor = Color.Transparent
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3-GRID QUICK NAV: Themes, Badges, Stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickNavCard(
                    title = "Themes",
                    subtitle = activeTheme.name,
                    icon = Icons.Default.FormatPaint,
                    iconTint = Color(0xFF8B5CF6),
                    cardBg = activeTheme.cardBg,
                    cardBorder = activeTheme.cardBorder,
                    textColorPrimary = activeTheme.textColorPrimary,
                    textColorSecondary = activeTheme.textColorSecondary,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateThemes
                )

                QuickNavCard(
                    title = "Badges",
                    subtitle = "Missions",
                    icon = Icons.Default.EmojiEvents,
                    iconTint = Color(0xFFF59E0B),
                    cardBg = activeTheme.cardBg,
                    cardBorder = activeTheme.cardBorder,
                    textColorPrimary = activeTheme.textColorPrimary,
                    textColorSecondary = activeTheme.textColorSecondary,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateAchievements
                )

                QuickNavCard(
                    title = "Stats",
                    subtitle = "${stats.gamesPlayed} plays",
                    icon = Icons.Default.Insights,
                    iconTint = Color(0xFF06B6D4),
                    cardBg = activeTheme.cardBg,
                    cardBorder = activeTheme.cardBorder,
                    textColorPrimary = activeTheme.textColorPrimary,
                    textColorSecondary = activeTheme.textColorSecondary,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateStats
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Settings Dialog
        if (showSettings) {
            SettingsDialog(
                theme = activeTheme,
                currentThemeMode = themeMode,
                soundEnabled = viewModel.soundManager.isSoundEnabled,
                hapticsEnabled = viewModel.hapticManager.isHapticsEnabled,
                onSelectThemeMode = { viewModel.setThemeMode(it) },
                onToggleSound = { viewModel.toggleSound() },
                onToggleHaptics = { viewModel.toggleHaptics() },
                onDismiss = { showSettings = false }
            )
        }
    }
}

@Composable
private fun QuickNavCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    cardBg: Color,
    cardBorder: Color,
    textColorPrimary: Color,
    textColorSecondary: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(1.dp, cardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = textColorPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = textColorSecondary,
                maxLines = 1
            )
        }
    }
}
