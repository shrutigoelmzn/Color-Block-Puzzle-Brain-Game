package com.example.ui.themes

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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MonetizationOn
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.GameTheme
import com.example.ui.components.BlockRenderUtils
import com.example.ui.game.GameViewModel
import com.example.ui.theme.LocalThemeIsDark

@Composable
fun ThemesScreen(
    viewModel: GameViewModel,
    onNavigateBack: () -> Unit
) {
    val isDark = LocalThemeIsDark.current
    val rawTheme by viewModel.activeTheme.collectAsState()
    val activeTheme = remember(rawTheme, isDark) { rawTheme.forMode(isDark) }
    val stats by viewModel.stats.collectAsState()
    val unlockedThemeIds = stats.unlockedThemeIds
    val coins = stats.coins

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
                    text = "THEME GALLERY",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = activeTheme.textColorPrimary
                )

                // Coins chip
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = activeTheme.cardBg,
                    border = BorderStroke(1.dp, activeTheme.cardBorder),
                    shadowElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.MonetizationOn,
                            contentDescription = "Coins",
                            tint = Color(0xFFFFB300),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$coins",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = activeTheme.textColorPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Grid of themes
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(GameTheme.ALL_THEMES) { themeItem ->
                    val themeForMode = remember(themeItem, isDark) { themeItem.forMode(isDark) }
                    val isUnlocked = unlockedThemeIds.contains(themeItem.id) || themeItem.price == 0
                    val isEquipped = activeTheme.id == themeItem.id

                    ThemeCard(
                        theme = themeForMode,
                        isUnlocked = isUnlocked,
                        isEquipped = isEquipped,
                        userCoins = coins,
                        onEquip = { viewModel.selectTheme(themeItem.id) },
                        onUnlock = { viewModel.unlockTheme(themeItem) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemeCard(
    theme: GameTheme,
    isUnlocked: Boolean,
    isEquipped: Boolean,
    userCoins: Int,
    onEquip: () -> Unit,
    onUnlock: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = theme.cardBg),
        border = BorderStroke(
            width = if (isEquipped) 2.dp else 1.dp,
            color = if (isEquipped) theme.accentColor else theme.cardBorder
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isEquipped) 4.dp else 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Theme preview palette swatch (5 mini blocks rendered in actual theme box style)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(theme.cellEmptyBg)
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 0 until 5) {
                    val c = theme.getBlockColor(i)
                    Canvas(modifier = Modifier.size(26.dp)) {
                        BlockRenderUtils.drawBlock(
                            drawScope = this,
                            topLeft = Offset.Zero,
                            size = Size(size.width, size.height),
                            colors = c,
                            style = theme.blockStyle
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = theme.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = theme.textColorPrimary
            )

            // Block Style badge (e.g. Wooden Box, Frosted Glass, etc.)
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = theme.accentColor.copy(alpha = 0.12f),
                modifier = Modifier.padding(top = 2.dp)
            ) {
                Text(
                    text = theme.blockStyle.displayName,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = theme.accentColor,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    fontSize = 10.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            when {
                isEquipped -> {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = theme.accentColor.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, theme.accentColor)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = theme.accentColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "ACTIVE",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = theme.accentColor
                            )
                        }
                    }
                }
                isUnlocked -> {
                    Button(
                        onClick = onEquip,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = theme.accentColor,
                            contentColor = theme.onAccentColor
                        )
                    ) {
                        Text(
                            text = "EQUIP",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                else -> {
                    val canAfford = userCoins >= theme.price
                    Button(
                        onClick = onUnlock,
                        enabled = canAfford,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFFB300),
                            contentColor = Color(0xFF3E1F00),
                            disabledContainerColor = theme.cellEmptyBg,
                            disabledContentColor = theme.textColorSecondary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${theme.price}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
