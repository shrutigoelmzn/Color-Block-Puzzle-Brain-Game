package com.example.ui.home

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.PlayGamesManager
import com.example.domain.model.GameTheme
import com.example.domain.model.LeaderboardEntry
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LeaderboardSection(
    leaderboard: List<LeaderboardEntry>,
    theme: GameTheme,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val isPlayGamesSignedIn by PlayGamesManager.isSignedIn.collectAsState()
    val gamerTag by PlayGamesManager.currentGamerTag.collectAsState()
    var showConfigDialog by remember { mutableStateOf(false) }

    val isGmsAvailable = remember(activity) {
        activity?.let { PlayGamesManager.isGooglePlayServicesAvailable(it) } ?: false
    }

    if (showConfigDialog) {
        AlertDialog(
            onDismissRequest = { showConfigDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.SportsEsports,
                        contentDescription = null,
                        tint = Color(0xFF0F9D58),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Google Play Games",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (isPlayGamesSignedIn && !gamerTag.isNullOrBlank()) {
                        Text(
                            text = "Connected Gamer: $gamerTag",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF0F9D58)
                        )
                    } else if (isGmsAvailable) {
                        Text(
                            text = "Play Games Services: Ready",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF0F9D58)
                        )
                    } else {
                        Text(
                            text = "Play Games Services: Configured",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF0F9D58)
                        )
                    }

                    Text(
                        text = "Your Project ID and Leaderboard IDs are configured in the app.",
                        style = MaterialTheme.typography.bodySmall,
                        color = theme.textColorSecondary
                    )

                    if (!isGmsAvailable) {
                        Text(
                            text = "Note: Google Play Services requires a real Android device with Google Play Store installed and test accounts added in your Google Play Console. On this browser preview emulator, scores are tracked safely via local offline storage.",
                            style = MaterialTheme.typography.bodySmall,
                            color = theme.textColorSecondary
                        )
                    }

                    Text(
                        text = "All game records and personal bests are always preserved on your device.",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = theme.textColorPrimary
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showConfigDialog = false }) {
                    Text("Got It", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = theme.cardBg),
        border = BorderStroke(1.dp, theme.cardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFFFFD54F), Color(0xFFFF9800))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = "Leaderboard",
                            tint = Color(0xFF422006),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "HIGH SCORE LEADERBOARD",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Black,
                            color = theme.textColorPrimary,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = if (isPlayGamesSignedIn && gamerTag != null) "Cloud Sync: $gamerTag"
                            else "Offline & Google Play Sync",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = if (isPlayGamesSignedIn) Color(0xFF10B981) else theme.textColorSecondary
                        )
                    }
                }

                val playGamesTint = if (theme.isDark) Color(0xFF34D399) else Color(0xFF0F9D58)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = playGamesTint.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, playGamesTint.copy(alpha = 0.5f)),
                    modifier = Modifier.clickable {
                        if (activity != null) {
                            if (!isGmsAvailable) {
                                showConfigDialog = true
                            } else if (!isPlayGamesSignedIn) {
                                PlayGamesManager.signInExplicitly(activity) { success ->
                                    if (success) {
                                        val classicId = context.getString(R.string.leaderboard_classic_id)
                                        PlayGamesManager.showLeaderboard(
                                            activity = activity,
                                            leaderboardId = classicId,
                                            onNotConfigured = { showConfigDialog = true }
                                        )
                                    } else {
                                        showConfigDialog = true
                                    }
                                }
                            } else {
                                val classicId = context.getString(R.string.leaderboard_classic_id)
                                PlayGamesManager.showLeaderboard(
                                    activity = activity,
                                    leaderboardId = classicId,
                                    onNotConfigured = { showConfigDialog = true }
                                )
                            }
                        } else {
                            showConfigDialog = true
                        }
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.SportsEsports,
                            contentDescription = "Play Games",
                            tint = playGamesTint,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Play Games",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = playGamesTint
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (leaderboard.isEmpty()) {
                // Empty state prompt
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = theme.cellEmptyBg,
                    border = BorderStroke(1.dp, theme.cardBorder.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp, horizontal = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No recorded scores yet",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = theme.textColorSecondary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Play your first game to enter the Hall of Fame!",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = theme.textColorSecondary
                        )
                    }
                }
            } else {
                // Display top entries
                val topEntries = leaderboard.take(5)
                topEntries.forEachIndexed { index, entry ->
                    val rank = index + 1
                    LeaderboardRow(
                        rank = rank,
                        entry = entry,
                        theme = theme
                    )
                    if (index < topEntries.size - 1) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun LeaderboardRow(
    rank: Int,
    entry: LeaderboardEntry,
    theme: GameTheme
) {
    val rankBadgeColor = when (rank) {
        1 -> Color(0xFFFFD700) // Gold
        2 -> Color(0xFFC0C0C0) // Silver
        3 -> Color(0xFFCD7F32) // Bronze
        else -> theme.cardBorder
    }

    val rankTextColor = when (rank) {
        1, 2, 3 -> Color(0xFF1E293B)
        else -> theme.textColorSecondary
    }

    val formattedDate = entry.dateString.ifEmpty { "Recent" }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = if (entry.isPersonalBest) theme.accentColor.copy(alpha = 0.12f) else theme.cellEmptyBg,
        border = BorderStroke(
            1.dp,
            if (entry.isPersonalBest) theme.accentColor.copy(alpha = 0.5f) else theme.cardBorder.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rank and Details
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Rank Circle
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(rankBadgeColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$rank",
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                        color = rankTextColor
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${entry.score}",
                            fontWeight = FontWeight.Black,
                            fontSize = 17.sp,
                            color = if (entry.isPersonalBest) theme.accentColor else theme.textColorPrimary
                        )

                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = entry.playerName,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = theme.textColorSecondary
                        )

                        if (entry.isPersonalBest) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFFF9800),
                            ) {
                                Text(
                                    text = "YOU ⭐",
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF3E1F00)
                                )
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = entry.mode.displayName.uppercase(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = theme.textColorSecondary
                        )
                        Text(
                            text = "•",
                            fontSize = 10.sp,
                            color = theme.textColorSecondary
                        )
                        Text(
                            text = "${entry.linesCleared} lines",
                            fontSize = 10.sp,
                            color = theme.textColorSecondary
                        )
                        if (entry.highestCombo >= 2) {
                            Text(
                                text = "•",
                                fontSize = 10.sp,
                                color = theme.textColorSecondary
                            )
                            Text(
                                text = "${entry.highestCombo}x combo",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFFF9800)
                            )
                        }
                    }
                }
            }

            // Timestamp
            Text(
                text = formattedDate,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = theme.textColorSecondary
            )
        }
    }
}
