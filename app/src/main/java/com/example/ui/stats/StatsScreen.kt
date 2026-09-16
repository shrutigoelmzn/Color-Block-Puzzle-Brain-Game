package com.example.ui.stats

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.ViewStream
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.game.GameViewModel
import com.example.ui.theme.LocalThemeIsDark

@Composable
fun StatsScreen(
    viewModel: GameViewModel,
    onNavigateBack: () -> Unit
) {
    val isDark = LocalThemeIsDark.current
    val rawTheme by viewModel.activeTheme.collectAsState()
    val activeTheme = remember(rawTheme, isDark) { rawTheme.forMode(isDark) }
    val stats by viewModel.stats.collectAsState()

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
                    text = "PLAYER STATS",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = activeTheme.textColorPrimary
                )

                Spacer(modifier = Modifier.size(48.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    StatCard(
                        title = "Best Score (Classic)",
                        value = "${stats.bestScore}",
                        icon = Icons.Default.EmojiEvents,
                        tint = Color(0xFFFFB300),
                        cardBg = activeTheme.cardBg,
                        cardBorder = activeTheme.cardBorder,
                        textColorPrimary = activeTheme.textColorPrimary,
                        textColorSecondary = activeTheme.textColorSecondary
                    )
                }
                item {
                    StatCard(
                        title = "Best Score (Timed)",
                        value = "${stats.bestTimedScore}",
                        icon = Icons.Default.Timer,
                        tint = Color(0xFF0284C7),
                        cardBg = activeTheme.cardBg,
                        cardBorder = activeTheme.cardBorder,
                        textColorPrimary = activeTheme.textColorPrimary,
                        textColorSecondary = activeTheme.textColorSecondary
                    )
                }
                item {
                    StatCard(
                        title = "Highest Combo",
                        value = "x${stats.highestCombo}",
                        icon = Icons.Default.LocalFireDepartment,
                        tint = Color(0xFFEA580C),
                        cardBg = activeTheme.cardBg,
                        cardBorder = activeTheme.cardBorder,
                        textColorPrimary = activeTheme.textColorPrimary,
                        textColorSecondary = activeTheme.textColorSecondary
                    )
                }
                item {
                    StatCard(
                        title = "Total Lines Cleared",
                        value = "${stats.totalLinesCleared}",
                        icon = Icons.Default.ViewStream,
                        tint = Color(0xFF10B981),
                        cardBg = activeTheme.cardBg,
                        cardBorder = activeTheme.cardBorder,
                        textColorPrimary = activeTheme.textColorPrimary,
                        textColorSecondary = activeTheme.textColorSecondary
                    )
                }
                item {
                    StatCard(
                        title = "Blocks Placed",
                        value = "${stats.totalBlocksPlaced}",
                        icon = Icons.Default.GridView,
                        tint = Color(0xFF8B5CF6),
                        cardBg = activeTheme.cardBg,
                        cardBorder = activeTheme.cardBorder,
                        textColorPrimary = activeTheme.textColorPrimary,
                        textColorSecondary = activeTheme.textColorSecondary
                    )
                }
                item {
                    StatCard(
                        title = "Games Played",
                        value = "${stats.gamesPlayed}",
                        icon = Icons.Default.SportsEsports,
                        tint = Color(0xFF06B6D4),
                        cardBg = activeTheme.cardBg,
                        cardBorder = activeTheme.cardBorder,
                        textColorPrimary = activeTheme.textColorPrimary,
                        textColorSecondary = activeTheme.textColorSecondary
                    )
                }
                item {
                    StatCard(
                        title = "Challenges Won",
                        value = "${stats.dailyChallengesCompleted}",
                        icon = Icons.Default.CalendarMonth,
                        tint = Color(0xFFF59E0B),
                        cardBg = activeTheme.cardBg,
                        cardBorder = activeTheme.cardBorder,
                        textColorPrimary = activeTheme.textColorPrimary,
                        textColorSecondary = activeTheme.textColorSecondary
                    )
                }
                item {
                    StatCard(
                        title = "Best Daily Streak",
                        value = "${stats.bestStreak} Days",
                        icon = Icons.Default.TrendingUp,
                        tint = Color(0xFFEF4444),
                        cardBg = activeTheme.cardBg,
                        cardBorder = activeTheme.cardBorder,
                        textColorPrimary = activeTheme.textColorPrimary,
                        textColorSecondary = activeTheme.textColorSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    tint: Color,
    cardBg: Color,
    cardBorder: Color,
    textColorPrimary: Color,
    textColorSecondary: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(1.dp, cardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = textColorPrimary
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = textColorSecondary
            )
        }
    }
}
