package com.example.ui.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import com.example.domain.engine.MoveFinder
import com.example.domain.engine.PlacementEngine
import com.example.domain.model.BlockShape
import com.example.domain.model.Board
import com.example.domain.model.GameTheme
import com.example.ui.components.BlockRenderUtils
import kotlin.math.roundToInt

@Composable
fun ShapeTrayView(
    shapes: List<BlockShape?>,
    board: Board,
    theme: GameTheme,
    highlightedShapeIndex: Int?,
    draggingShapeIndex: Int?,
    onDragStart: (shapeIndex: Int, shape: BlockShape, localOffset: Offset, slotCoords: LayoutCoordinates) -> Unit,
    onDragMove: (dragAmount: Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        shapes.forEachIndexed { index, shape ->
            ShapeSlot(
                shapeIndex = index,
                shape = shape,
                board = board,
                theme = theme,
                isDraggingThis = draggingShapeIndex == index,
                isHighlighted = highlightedShapeIndex == index,
                onDragStart = onDragStart,
                onDragMove = onDragMove,
                onDragEnd = onDragEnd,
                onDragCancel = onDragCancel
            )
        }
    }
}

@Composable
private fun ShapeSlot(
    shapeIndex: Int,
    shape: BlockShape?,
    board: Board,
    theme: GameTheme,
    isDraggingThis: Boolean,
    isHighlighted: Boolean,
    onDragStart: (shapeIndex: Int, shape: BlockShape, localOffset: Offset, slotCoords: LayoutCoordinates) -> Unit,
    onDragMove: (dragAmount: Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit
) {
    var slotCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val currentOnDragStart by rememberUpdatedState(onDragStart)
    val currentOnDragMove by rememberUpdatedState(onDragMove)
    val currentOnDragEnd by rememberUpdatedState(onDragEnd)
    val currentOnDragCancel by rememberUpdatedState(onDragCancel)

    val canFit = shape != null && MoveFinder.canPlaceShapeAnywhere(board, shape)
    val isPerfectFit = shape != null && (MoveFinder.canClearAnyLine(board, shape) || MoveFinder.countValidPlacements(board, shape) >= 3)

    Box(
        modifier = Modifier
            .size(100.dp, 100.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(theme.boardBg.copy(alpha = if (isHighlighted) 0.95f else 0.5f))
            .border(
                width = if (isHighlighted) 2.dp else if (isPerfectFit && !isDraggingThis) 1.5.dp else 1.dp,
                color = when {
                    isHighlighted -> theme.accentColor
                    isPerfectFit && !isDraggingThis -> Color(0xFF10B981).copy(alpha = 0.5f)
                    else -> theme.cardBorder.copy(alpha = 0.3f)
                },
                shape = RoundedCornerShape(16.dp)
            )
            .onGloballyPositioned { slotCoordinates = it },
        contentAlignment = Alignment.Center
    ) {
        if (shape != null) {
            val slotAlpha = if (isDraggingThis) 0.25f else if (canFit) 1.0f else 0.45f
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(slotAlpha)
                    .pointerInput(shape) {
                        detectDragGestures(
                            onDragStart = { localOffset ->
                                val coords = slotCoordinates
                                if (coords != null && coords.isAttached) {
                                    currentOnDragStart(shapeIndex, shape, localOffset, coords)
                                }
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                currentOnDragMove(dragAmount)
                            },
                            onDragEnd = {
                                currentOnDragEnd()
                            },
                            onDragCancel = {
                                currentOnDragCancel()
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                ShapeCanvas(shape = shape, theme = theme)
            }
        }
    }
}

fun calculateBoardTarget(
    shape: BlockShape,
    touchPosInRoot: Offset,
    rootCoords: LayoutCoordinates?,
    boardCoords: LayoutCoordinates?,
    board: Board,
    liftOffsetPx: Float
): DragPreviewState? {
    if (rootCoords == null || !rootCoords.isAttached) return null
    if (boardCoords == null || !boardCoords.isAttached) return null

    val boardTopLeftInRoot = rootCoords.localPositionOf(boardCoords, Offset.Zero)
    val boardWidth = boardCoords.size.width.toFloat()
    val cellSize = boardWidth / 8f

    // Visual center of dragged shape
    val shapeCenterX = touchPosInRoot.x
    val shapeCenterY = touchPosInRoot.y - liftOffsetPx

    // Relative to board top-left
    val relX = shapeCenterX - boardTopLeftInRoot.x
    val relY = shapeCenterY - boardTopLeftInRoot.y

    // Calculate hover column and row for top-left cell of the shape
    val hoverCol = ((relX / cellSize) - (shape.width / 2f)).roundToInt()
    val hoverRow = ((relY / cellSize) - (shape.height / 2f)).roundToInt()

    // If completely outside board boundary, no preview
    if (hoverRow < -shape.height + 1 || hoverRow >= board.size ||
        hoverCol < -shape.width + 1 || hoverCol >= board.size
    ) {
        return null
    }

    val isValid = PlacementEngine.canPlace(board, shape, hoverRow, hoverCol)
    return DragPreviewState(
        shape = shape,
        hoverRow = hoverRow,
        hoverCol = hoverCol,
        isValid = isValid
    )
}

@Composable
fun ShapeCanvas(
    shape: BlockShape,
    theme: GameTheme,
    modifier: Modifier = Modifier
) {
    val blockColors = theme.getBlockColor(shape.colorId)
    val maxDim = maxOf(shape.width, shape.height, 3)
    val cellSizeDp = (72 / maxDim).dp

    Box(
        modifier = modifier.size(
            width = cellSizeDp * shape.width,
            height = cellSizeDp * shape.height
        )
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val cellPx = size.width / shape.width
            for (r in 0 until shape.height) {
                for (c in 0 until shape.width) {
                    if (shape.isFilled(r, c)) {
                        BlockRenderUtils.drawBlock(
                            drawScope = this,
                            topLeft = Offset(c * cellPx, r * cellPx),
                            size = Size(cellPx, cellPx),
                            colors = blockColors
                        )
                    }
                }
            }
        }
    }
}
