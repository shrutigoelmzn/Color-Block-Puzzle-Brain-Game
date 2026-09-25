package com.example.ui.game

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import android.app.Activity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.ads.AdManager
import com.example.domain.model.BlockShape
import com.example.domain.model.GameMode
import com.example.ui.components.BlockRenderUtils
import com.example.ui.daily.DailyChallengeCompleteDialog
import com.example.ui.game.LevelCompleteDialog
import com.example.ui.gameover.GameOverDialog
import com.example.ui.theme.LocalThemeIsDark
import kotlin.math.roundToInt

@Composable
fun GameScreen(
    viewModel: GameViewModel,
    onNavigateBack: () -> Unit
) {
    val gameState by viewModel.gameState.collectAsState()
    val isRewardedAdReady by AdManager.isRewardedAdReady.collectAsState()
    val context = LocalContext.current
    val isDark = LocalThemeIsDark.current
    val rawTheme by viewModel.activeTheme.collectAsState()
    val theme = remember(rawTheme, isDark) { rawTheme.forMode(isDark) }
    val dailyChallenge by viewModel.dailyChallenge.collectAsState()

    var rootCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var boardCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var dragPreview by remember { mutableStateOf<DragPreviewState?>(null) }
    var draggingShapeIndex by remember { mutableStateOf<Int?>(null) }
    var draggingShape by remember { mutableStateOf<BlockShape?>(null) }
    var dragTouchPositionInRoot by remember { mutableStateOf<Offset?>(null) }
    var showRewardedUndoDialog by remember { mutableStateOf(false) }
    val density = LocalDensity.current

    // Handle device back button press in GameScreen:
    // If quit confirmation or pause dialog is open, dismiss it and resume play (never auto-quit to main menu).
    // If game is active, request quit confirmation so progress is not accidentally lost.
    BackHandler(enabled = true) {
        when {
            showRewardedUndoDialog -> {
                showRewardedUndoDialog = false
            }
            gameState.isQuitConfirmationVisible -> {
                // Dismiss quit confirmation dialog and stay in the game; NEVER navigate to home
                viewModel.dismissQuitConfirmation()
            }
            gameState.isPaused -> {
                // Dismiss pause dialog and resume gameplay; NEVER navigate to home
                viewModel.resumeGame()
            }
            gameState.isGameOver || gameState.isLevelCompleted || gameState.isDailyChallengeCompleted -> {
                // Game already concluded; safely return to main menu
                onNavigateBack()
            }
            else -> {
                // Active game back press shows the persistent quit confirmation dialog
                viewModel.requestQuitConfirmation()
            }
        }
    }

    // Infinite transition for pulsing combo banner
    val infiniteTransition = rememberInfiniteTransition(label = "combo")
    val comboScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(400),
            repeatMode = RepeatMode.Reverse
        ),
        label = "comboScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { rootCoordinates = it }
            .background(
                Brush.verticalGradient(
                    listOf(theme.bgGradientStart, theme.bgGradientEnd)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // TOP BAR: Navigation, Mode, Score & Best
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.requestQuitConfirmation() },
                        modifier = Modifier.testTag("game_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Quit Game",
                            tint = theme.textColorPrimary
                        )
                    }

                    // Score Display
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "SCORE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = theme.textColorSecondary,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "${gameState.score}",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            color = theme.textColorPrimary
                        )
                    }

                    // Best Score & Pause button
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val bestColor = if (theme.isDark) Color(0xFFFFB300) else Color(0xFFC2410C)
                        Column(
                            horizontalAlignment = Alignment.End,
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(
                                text = "BEST",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = bestColor,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "${gameState.bestScore}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = bestColor
                            )
                        }

                        IconButton(
                            onClick = { viewModel.pauseGame() },
                            modifier = Modifier.testTag("game_pause_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Pause,
                                contentDescription = "Pause",
                                tint = theme.textColorPrimary
                            )
                        }
                    }
                }

                // SUB HEADER: Mode, Timer, or Challenge Objective
                if (gameState.mode == GameMode.TIMED) {
                    val timedLevel = gameState.currentTimedLevel
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        if (timedLevel != null) {
                            val diffColor = Color(timedLevel.difficulty.colorHex)
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = diffColor.copy(alpha = 0.16f),
                                border = BorderStroke(1.dp, diffColor.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = "LVL ${timedLevel.levelNumber} • ${timedLevel.difficulty.label.uppercase()}",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 11.sp,
                                    color = diffColor
                                )
                            }

                            Text(
                                text = "Goal: ${gameState.score}/${timedLevel.targetScore} • Lines: ${gameState.linesClearedInGame}/${timedLevel.targetLines}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = theme.textColorSecondary
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = "Timer",
                                tint = if (gameState.timeRemainingSeconds <= 15) Color(0xFFFF5252) else Color(0xFF00E5FF),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${gameState.timeRemainingSeconds}s",
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                                color = if (gameState.timeRemainingSeconds <= 15) Color(0xFFEF4444) else theme.textColorPrimary
                            )
                        }
                    }
                } else if (gameState.mode == GameMode.CHALLENGE && dailyChallenge != null) {
                    val challenge = dailyChallenge!!
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF2E7D32).copy(alpha = 0.5f))
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (challenge.isCompleted || gameState.isDailyChallengeCompleted) "Daily Challenge Complete! ⭐"
                                else "Goal: ${gameState.score}/${challenge.targetScore} pts | ${gameState.linesClearedInGame}/${challenge.targetLines} lines | ${gameState.highestComboInGame}/${challenge.targetCombos}x combo",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFA5D6A7)
                            )
                        }
                    }
                }

                // COMBO BANNER (Animated pill when combo >= 2)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(34.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (gameState.combo >= 2) {
                        Box(
                            modifier = Modifier
                                .scale(comboScale)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(Color(0xFFFF8F00), Color(0xFFFFD54F))
                                    )
                                )
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "COMBO x${gameState.combo}! 🔥",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp,
                                color = Color(0xFF3E1F00)
                            )
                        }
                    }
                }
            }

            // CENTER: 8x8 Game Board with Stacked Combo & Floating Score Overlays
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                contentAlignment = Alignment.Center
            ) {
                GameBoardCanvas(
                    board = gameState.board,
                    theme = theme,
                    activeHint = gameState.activeHint,
                    activeClearEffect = gameState.activeClearEffect,
                    dragPreview = dragPreview,
                    recentlyPlacedCoords = gameState.recentlyPlacedCoords,
                    clearingCoords = gameState.clearingCoords,
                    onPositioned = { boardCoordinates = it }
                )

                // Floating score popups over board
                val boardW = boardCoordinates?.size?.width?.toFloat() ?: 0f
                val boardH = boardCoordinates?.size?.height?.toFloat() ?: 0f
                if (boardW > 0f && boardH > 0f) {
                    gameState.floatingScores.forEach { fEvent ->
                        FloatingScoreItem(
                            event = fEvent,
                            boardWidthPx = boardW,
                            boardHeightPx = boardH
                        )
                    }
                }

                // Satisfying particle effects and visual pop-ups on the grid for simultaneous multi-line clears
                MultiLineClearOverlay(
                    clearEffect = gameState.activeClearEffect,
                    theme = theme
                )

                // 1-Second Auto-dismissing Combo Banner over board (when not already showing multi-line clear)
                if (gameState.activeClearEffect?.isMultiLine != true) {
                    ComboBoardOverlay(
                        banner = gameState.activeComboBanner
                    )
                }
            }

            // BOTTOM: 3-Shape Tray & Action Buttons
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Tray with 3 blocks
                ShapeTrayView(
                    shapes = gameState.availableShapes,
                    board = gameState.board,
                    theme = theme,
                    highlightedShapeIndex = gameState.activeHint?.shapeIndex,
                    draggingShapeIndex = draggingShapeIndex,
                    onDragStart = { shapeIndex, shape, localOffset, slotCoords ->
                        if (gameState.isPaused || gameState.isGameOver) return@ShapeTrayView
                        val root = rootCoordinates
                        if (root != null && root.isAttached && slotCoords.isAttached) {
                            val touchInRoot = root.localPositionOf(slotCoords, localOffset)
                            draggingShapeIndex = shapeIndex
                            draggingShape = shape
                            dragTouchPositionInRoot = touchInRoot
                            viewModel.userInteracted()

                            val liftOffsetPx = with(density) { 75.dp.toPx() }
                            dragPreview = calculateBoardTarget(
                                shape = shape,
                                touchPosInRoot = touchInRoot,
                                rootCoords = root,
                                boardCoords = boardCoordinates,
                                board = gameState.board,
                                liftOffsetPx = liftOffsetPx
                            )
                        }
                    },
                    onDragMove = { dragAmount ->
                        val currentPos = dragTouchPositionInRoot
                        val currentShape = draggingShape
                        val root = rootCoordinates
                        if (currentPos != null && currentShape != null && root != null && root.isAttached) {
                            val newPos = currentPos + dragAmount
                            dragTouchPositionInRoot = newPos

                            val liftOffsetPx = with(density) { 75.dp.toPx() }
                            dragPreview = calculateBoardTarget(
                                shape = currentShape,
                                touchPosInRoot = newPos,
                                rootCoords = root,
                                boardCoords = boardCoordinates,
                                board = gameState.board,
                                liftOffsetPx = liftOffsetPx
                            )
                        }
                    },
                    onDragEnd = {
                        val preview = dragPreview
                        val shapeIndex = draggingShapeIndex
                        if (preview != null && preview.isValid && shapeIndex != null) {
                            viewModel.placeShape(shapeIndex, preview.hoverRow, preview.hoverCol)
                        }
                        draggingShapeIndex = null
                        draggingShape = null
                        dragTouchPositionInRoot = null
                        dragPreview = null
                    },
                    onDragCancel = {
                        draggingShapeIndex = null
                        draggingShape = null
                        dragTouchPositionInRoot = null
                        dragPreview = null
                    }
                )

                // Bottom Action Bar: Undo, Hint, Coins Chip
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Undo Button (1 Free Undo by default; watch rewarded ad for +2 Undos)
                    // Hidden if free undos are 0 and no rewarded ad is ready
                    if (gameState.freeUndosRemaining > 0 || isRewardedAdReady) {
                        BadgedBox(
                            badge = {
                                if (gameState.freeUndosRemaining > 0) {
                                    Badge(containerColor = Color(0xFF00E676)) {
                                        Text(
                                            text = "${gameState.freeUndosRemaining}",
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                } else {
                                    Badge(containerColor = Color(0xFFFF9800)) {
                                        Text(
                                            text = "+2 AD",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 9.sp
                                        )
                                    }
                                }
                            }
                        ) {
                            FilledTonalButton(
                                onClick = {
                                    if (gameState.freeUndosRemaining > 0) {
                                        viewModel.undo()
                                    } else if (isRewardedAdReady) {
                                        showRewardedUndoDialog = true
                                    }
                                },
                                enabled = !gameState.isPaused && !gameState.isGameOver &&
                                        (gameState.freeUndosRemaining == 0 || gameState.lastUndoSnapshot != null),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Undo,
                                    contentDescription = "Undo",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (gameState.freeUndosRemaining > 0) "Undo" else "+2 Undos",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // Smart Hint Button
                    FilledTonalButton(
                        onClick = { viewModel.requestHint() },
                        enabled = !gameState.isPaused && !gameState.isGameOver,
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = "Hint",
                            tint = Color(0xFFFFD54F),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Hint", fontSize = 13.sp)
                    }
                }
            }
        }

        // FLOATING DRAGGED SHAPE OVERLAY
        if (draggingShape != null && dragTouchPositionInRoot != null) {
            val shape = draggingShape!!
            val touchPos = dragTouchPositionInRoot!!
            val cellSize = if (boardCoordinates != null && boardCoordinates!!.isAttached) {
                boardCoordinates!!.size.width.toFloat() / 8f
            } else {
                with(density) { 42.dp.toPx() }
            }
            val liftOffsetPx = with(density) { 75.dp.toPx() }

            val shapeWidthPx = shape.width * cellSize
            val shapeHeightPx = shape.height * cellSize

            val shapeCenterX = touchPos.x
            val shapeCenterY = touchPos.y - liftOffsetPx

            val topLeftX = shapeCenterX - shapeWidthPx / 2f
            val topLeftY = shapeCenterY - shapeHeightPx / 2f

            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(topLeftX.roundToInt(), topLeftY.roundToInt())
                    }
                    .size(
                        width = with(density) { shapeWidthPx.toDp() },
                        height = with(density) { shapeHeightPx.toDp() }
                    )
                    .graphicsLayer {
                        shadowElevation = 24f
                        scaleX = 1.05f
                        scaleY = 1.05f
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val blockColors = theme.getBlockColor(shape.colorId)
                    for (r in 0 until shape.height) {
                        for (c in 0 until shape.width) {
                            if (shape.isFilled(r, c)) {
                                BlockRenderUtils.drawBlock(
                                    drawScope = this,
                                    topLeft = Offset(c * cellSize, r * cellSize),
                                    size = Size(cellSize, cellSize),
                                    colors = blockColors,
                                    style = theme.blockStyle,
                                    alpha = 0.95f
                                )
                            }
                        }
                    }
                }
            }
        }

        // OVERLAYS & DIALOGS
        if (gameState.isQuitConfirmationVisible) {
            QuitConfirmDialog(
                theme = theme,
                score = gameState.score,
                linesCleared = gameState.linesClearedInGame,
                onDismiss = { viewModel.dismissQuitConfirmation() },
                onConfirmQuit = {
                    viewModel.confirmQuit()
                    onNavigateBack()
                }
            )
        } else if (gameState.isPaused) {
            PauseDialog(
                theme = theme,
                score = gameState.score,
                linesCleared = gameState.linesClearedInGame,
                timeRemaining = if (gameState.mode == GameMode.TIMED) gameState.timeRemainingSeconds else null,
                soundEnabled = viewModel.soundManager.isSoundEnabled,
                hapticsEnabled = viewModel.hapticManager.isHapticsEnabled,
                onResume = { viewModel.resumeGame() },
                onRestart = { viewModel.startNewGame(gameState.mode) },
                onToggleSound = { viewModel.toggleSound() },
                onToggleHaptics = { viewModel.toggleHaptics() },
                onHome = {
                    // Show Quit Confirmation Dialog so progress is never lost by accident
                    viewModel.requestQuitConfirmation()
                }
            )
        }

        if (gameState.isGameOver) {
            GameOverDialog(
                gameState = gameState,
                theme = theme,
                isRewardedAdReady = isRewardedAdReady,
                onRestart = {
                    if (gameState.mode == GameMode.TIMED) {
                        viewModel.retryCurrentTimedLevel()
                    } else {
                        viewModel.startNewGame(gameState.mode)
                    }
                },
                onReviveWithAd = {
                    val activity = context as? Activity
                    if (activity != null) {
                        AdManager.showRewardedAd(
                            activity = activity,
                            onUserEarnedReward = { viewModel.reviveWithRewardedAd() }
                        )
                    }
                },
                onExtendTimeWithAd = {
                    val activity = context as? Activity
                    if (activity != null) {
                        AdManager.showRewardedAd(
                            activity = activity,
                            onUserEarnedReward = { viewModel.extendTimeWithRewardedAd(10) }
                        )
                    }
                },
                onHome = { onNavigateBack() }
            )
        }

        // Timed Rush Level Complete Dialog
        if (gameState.isLevelCompleted && gameState.currentTimedLevel != null) {
            LevelCompleteDialog(
                completedLevel = gameState.currentTimedLevel!!,
                theme = theme,
                score = gameState.score,
                linesCleared = gameState.linesClearedInGame,
                highestCombo = gameState.highestComboInGame,
                rewardCoins = gameState.completedLevelReward,
                onNextLevel = {
                    val activity = context as? Activity
                    if (activity != null) {
                        AdManager.showInterstitialAd(activity) {
                            viewModel.startNextTimedLevel()
                        }
                    } else {
                        viewModel.startNextTimedLevel()
                    }
                },
                onHome = { onNavigateBack() }
            )
        }

        // Daily Challenge Complete Dialog
        if (gameState.isDailyChallengeCompleted && dailyChallenge != null) {
            DailyChallengeCompleteDialog(
                challenge = dailyChallenge!!,
                rewardCoins = gameState.dailyRewardCoins,
                theme = theme,
                onHome = {
                    viewModel.dismissDailyCelebration()
                    onNavigateBack()
                }
            )
        }

        // Rewarded Ad Extra Undos Dialog
        if (showRewardedUndoDialog) {
            AlertDialog(
                onDismissRequest = { showRewardedUndoDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF10B981).copy(alpha = 0.15f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Undo,
                                    contentDescription = null,
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Get 2 Extra Undos",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                text = {
                    Column {
                        Text(
                            text = "You've used your free undo. Watch a short video ad to immediately receive 2 additional Undos.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = theme.textColorSecondary
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF10B981).copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.35f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFF10B981),
                                    modifier = Modifier.padding(end = 8.dp)
                                ) {
                                    Text(
                                        text = "REWARD",
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 9.sp,
                                        color = Color.White
                                    )
                                }
                                Text(
                                    text = "+2 Extra Undos",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color(0xFF10B981)
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    if (isRewardedAdReady) {
                        Button(
                            onClick = {
                                val activity = context as? Activity
                                if (activity != null) {
                                    AdManager.showRewardedAd(
                                        activity = activity,
                                        onUserEarnedReward = {
                                            viewModel.grantRewardedUndos(2)
                                            showRewardedUndoDialog = false
                                        },
                                        onAdDismissed = {
                                            showRewardedUndoDialog = false
                                        }
                                    )
                                } else {
                                    showRewardedUndoDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                        ) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color.White.copy(alpha = 0.25f),
                                modifier = Modifier.padding(end = 6.dp)
                            ) {
                                Text(
                                    text = "AD",
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 9.sp,
                                    color = Color.White
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Watch Ad (+2 Undos)", fontWeight = FontWeight.Bold)
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showRewardedUndoDialog = false }) {
                        Text(if (isRewardedAdReady) "Cancel" else "Close")
                    }
                }
            )
        }
    }
}
