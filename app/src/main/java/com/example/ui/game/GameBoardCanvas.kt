package com.example.ui.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import com.example.domain.engine.PlacementEngine
import com.example.domain.model.BlockShape
import com.example.domain.model.Board
import com.example.domain.model.ClearEffectEvent
import com.example.domain.model.GameTheme
import com.example.domain.model.HintMove
import com.example.ui.components.BlockRenderUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class DragPreviewState(
    val shape: BlockShape,
    val hoverRow: Int,
    val hoverCol: Int,
    val isValid: Boolean
)

@Composable
fun GameBoardCanvas(
    board: Board,
    theme: GameTheme,
    activeHint: HintMove?,
    activeClearEffect: ClearEffectEvent?,
    dragPreview: DragPreviewState?,
    recentlyPlacedCoords: Set<Pair<Int, Int>> = emptySet(),
    clearingCoords: Set<Pair<Int, Int>> = emptySet(),
    onPositioned: (LayoutCoordinates) -> Unit,
    modifier: Modifier = Modifier
) {
    // Pulse animation for hints
    val hintPulse = remember { Animatable(0.4f) }
    LaunchedEffect(activeHint) {
        if (activeHint != null) {
            hintPulse.animateTo(
                targetValue = 1.0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )
        } else {
            hintPulse.snapTo(0.4f)
        }
    }

    // Flash animation for line clears
    var clearFlashAlpha by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(activeClearEffect) {
        if (activeClearEffect != null) {
            clearFlashAlpha = 1f
            delay(250)
            clearFlashAlpha = 0f
        }
    }

    // Subtle entry spring pop animation for placed blocks
    val entryScale = remember { Animatable(1.0f) }
    LaunchedEffect(recentlyPlacedCoords) {
        if (recentlyPlacedCoords.isNotEmpty()) {
            entryScale.snapTo(0.35f)
            entryScale.animateTo(
                targetValue = 1.0f,
                animationSpec = spring(
                    dampingRatio = 0.65f,
                    stiffness = 500f
                )
            )
        } else {
            entryScale.snapTo(1.0f)
        }
    }

    // Subtle exit shrink and fade animation for cleared blocks
    val exitScale = remember { Animatable(1.0f) }
    val exitAlpha = remember { Animatable(1.0f) }
    LaunchedEffect(clearingCoords) {
        if (clearingCoords.isNotEmpty()) {
            exitScale.snapTo(1.0f)
            exitAlpha.snapTo(1.0f)
            launch {
                exitScale.animateTo(0.1f, tween(240, easing = LinearEasing))
            }
            launch {
                exitAlpha.animateTo(0.0f, tween(240, easing = LinearEasing))
            }
        } else {
            exitScale.snapTo(1.0f)
            exitAlpha.snapTo(1.0f)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .padding(12.dp)
            .shadow(16.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(theme.boardBg)
            .onGloballyPositioned { onPositioned(it) }
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val boardSize = size.width
            val cellSize = boardSize / 8f

            // 1. Draw empty board slots
            for (r in 0 until 8) {
                for (c in 0 until 8) {
                    val topLeft = Offset(c * cellSize, r * cellSize)
                    BlockRenderUtils.drawEmptySlot(
                        drawScope = this,
                        topLeft = topLeft,
                        size = Size(cellSize, cellSize),
                        slotBg = theme.cellEmptyBg,
                        borderColor = theme.gridBorder
                    )
                }
            }

            // 2. Draw placed blocks with entry and exit animations
            for (r in 0 until 8) {
                for (c in 0 until 8) {
                    val cell = board.get(r, c)
                    if (cell != null) {
                        val topLeft = Offset(c * cellSize, r * cellSize)
                        val blockColors = theme.getBlockColor(cell.colorId)
                        val isRecent = recentlyPlacedCoords.contains(r to c)
                        val isClearing = clearingCoords.contains(r to c)

                        val scale = when {
                            isClearing -> exitScale.value
                            isRecent -> entryScale.value
                            else -> 1.0f
                        }
                        val alpha = when {
                            isClearing -> exitAlpha.value
                            else -> 1.0f
                        }

                        BlockRenderUtils.drawBlock(
                            drawScope = this,
                            topLeft = topLeft,
                            size = Size(cellSize, cellSize),
                            colors = blockColors,
                            style = theme.blockStyle,
                            scale = scale,
                            alpha = alpha
                        )
                    }
                }
            }

            // 3. Draw Hint highlight (if active and no drag preview)
            if (activeHint != null && dragPreview == null) {
                val candidateCoords = PlacementEngine.getCandidateCoordinates(
                    shape = BlockShape.ALL_SHAPES.firstOrNull { it.id == activeHint.shapeIndex.toString() }
                        ?: BlockShape.dot(),
                    startRow = activeHint.targetRow,
                    startCol = activeHint.targetCol
                )
                val hintColors = theme.getBlockColor(0)
                for (pair in candidateCoords) {
                    val r = pair.first
                    val c = pair.second
                    if (r in 0 until 8 && c in 0 until 8) {
                        BlockRenderUtils.drawPreviewCell(
                            drawScope = this,
                            topLeft = Offset(c * cellSize, r * cellSize),
                            size = Size(cellSize, cellSize),
                            colors = hintColors,
                            isValid = true,
                            pulseAlpha = hintPulse.value
                        )
                    }
                }
            }

            // 4. Draw Drag Preview
            if (dragPreview != null) {
                val coords = PlacementEngine.getCandidateCoordinates(
                    shape = dragPreview.shape,
                    startRow = dragPreview.hoverRow,
                    startCol = dragPreview.hoverCol
                )
                val previewColors = theme.getBlockColor(dragPreview.shape.colorId)
                for (pair in coords) {
                    val r = pair.first
                    val c = pair.second
                    if (r in 0 until 8 && c in 0 until 8) {
                        BlockRenderUtils.drawPreviewCell(
                            drawScope = this,
                            topLeft = Offset(c * cellSize, r * cellSize),
                            size = Size(cellSize, cellSize),
                            colors = previewColors,
                            isValid = dragPreview.isValid,
                            pulseAlpha = 0.85f
                        )
                    }
                }
            }

            // 5. Draw Line Clear Glow & Flash Effect
            if (activeClearEffect != null && clearFlashAlpha > 0f) {
                // Glow on cleared rows
                for (r in activeClearEffect.rows) {
                    val rowTop = r * cellSize
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = clearFlashAlpha * 0.9f),
                                Color(0xFFFFD54F).copy(alpha = clearFlashAlpha * 0.9f),
                                Color.White.copy(alpha = clearFlashAlpha * 0.9f),
                                Color.Transparent
                            )
                        ),
                        topLeft = Offset(0f, rowTop),
                        size = Size(boardSize, cellSize)
                    )
                }

                // Glow on cleared columns
                for (c in activeClearEffect.cols) {
                    val colLeft = c * cellSize
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = clearFlashAlpha * 0.9f),
                                Color(0xFFFFD54F).copy(alpha = clearFlashAlpha * 0.9f),
                                Color.White.copy(alpha = clearFlashAlpha * 0.9f),
                                Color.Transparent
                            )
                        ),
                        topLeft = Offset(colLeft, 0f),
                        size = Size(cellSize, boardSize)
                    )
                }
            }
        }
    }
}
