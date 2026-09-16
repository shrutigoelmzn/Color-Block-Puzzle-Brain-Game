package com.example.ui.achievements

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.Achievement
import com.example.domain.model.Mission
import com.example.ui.game.GameViewModel
import com.example.ui.theme.LocalThemeIsDark

@Composable
fun AchievementsScreen(
    viewModel: GameViewModel,
    onNavigateBack: () -> Unit
) {
    val isDark = LocalThemeIsDark.current
    val rawTheme by viewModel.activeTheme.collectAsState()
    val activeTheme = remember(rawTheme, isDark) { rawTheme.forMode(isDark) }
    val achievements by viewModel.achievements.collectAsState()
    val missions by viewModel.missions.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }

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
            // Top Bar
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
                    text = "REWARDS & BADGES",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = activeTheme.textColorPrimary
                )

                Spacer(modifier = Modifier.size(48.dp))
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Tab Bar
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = activeTheme.cardBg,
                contentColor = activeTheme.textColorPrimary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = activeTheme.accentColor
                    )
                },
                modifier = Modifier.clip(RoundedCornerShape(16.dp))
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            "Daily Missions",
                            fontWeight = FontWeight.Bold,
                            color = if (selectedTab == 0) activeTheme.accentColor else activeTheme.textColorSecondary
                        )
                    },
                    icon = {
                        Icon(
                            Icons.Default.TaskAlt,
                            contentDescription = null,
                            tint = if (selectedTab == 0) activeTheme.accentColor else activeTheme.textColorSecondary
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            "Achievements",
                            fontWeight = FontWeight.Bold,
                            color = if (selectedTab == 1) activeTheme.accentColor else activeTheme.textColorSecondary
                        )
                    },
                    icon = {
                        Icon(
                            Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = if (selectedTab == 1) activeTheme.accentColor else activeTheme.textColorSecondary
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (selectedTab == 0) {
                // Daily Missions List
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(missions) { mission ->
                        MissionItem(
                            mission = mission,
                            cardBg = activeTheme.cardBg,
                            cardBorder = activeTheme.cardBorder,
                            textColorPrimary = activeTheme.textColorPrimary,
                            textColorSecondary = activeTheme.textColorSecondary,
                            accentColor = activeTheme.accentColor,
                            onClaim = { viewModel.claimMission(mission) }
                        )
                    }
                }
            } else {
                // Achievements List
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(achievements) { achievement ->
                        AchievementItem(
                            achievement = achievement,
                            cardBg = activeTheme.cardBg,
                            cardBorder = activeTheme.cardBorder,
                            textColorPrimary = activeTheme.textColorPrimary,
                            textColorSecondary = activeTheme.textColorSecondary,
                            accentColor = activeTheme.accentColor,
                            onClaim = { viewModel.claimAchievement(achievement) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MissionItem(
    mission: Mission,
    cardBg: Color,
    cardBorder: Color,
    textColorPrimary: Color,
    textColorSecondary: Color,
    accentColor: Color,
    onClaim: () -> Unit
) {
    val progress = (mission.currentProgress.toFloat() / mission.target).coerceIn(0f, 1f)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(1.dp, cardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = mission.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = textColorPrimary
                )
                Text(
                    text = mission.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = textColorSecondary
                )

                Spacer(modifier = Modifier.height(8.dp))

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = accentColor,
                    trackColor = cardBorder
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "${mission.currentProgress} / ${mission.target}",
                    style = MaterialTheme.typography.labelSmall,
                    color = textColorSecondary
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Action / Reward
            if (mission.isClaimed) {
                Text(
                    text = "CLAIMED ✓",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF10B981)
                )
            } else {
                Button(
                    onClick = onClaim,
                    enabled = mission.isCompleted,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFB300),
                        contentColor = Color(0xFF3E1F00),
                        disabledContainerColor = cardBorder,
                        disabledContentColor = textColorSecondary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.MonetizationOn,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "+${mission.rewardCoins}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun AchievementItem(
    achievement: Achievement,
    cardBg: Color,
    cardBorder: Color,
    textColorPrimary: Color,
    textColorSecondary: Color,
    accentColor: Color,
    onClaim: () -> Unit
) {
    val progress = (achievement.currentProgress.toFloat() / achievement.target).coerceIn(0f, 1f)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(1.dp, cardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = achievement.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = textColorPrimary
                )
                Text(
                    text = achievement.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = textColorSecondary
                )

                Spacer(modifier = Modifier.height(8.dp))

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = Color(0xFFFFB300),
                    trackColor = cardBorder
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "${achievement.currentProgress} / ${achievement.target}",
                    style = MaterialTheme.typography.labelSmall,
                    color = textColorSecondary
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (achievement.isClaimed) {
                Text(
                    text = "CLAIMED ✓",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF10B981)
                )
            } else {
                Button(
                    onClick = onClaim,
                    enabled = achievement.isCompleted,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFB300),
                        contentColor = Color(0xFF3E1F00),
                        disabledContainerColor = cardBorder,
                        disabledContentColor = textColorSecondary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.MonetizationOn,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "+${achievement.rewardCoins}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
