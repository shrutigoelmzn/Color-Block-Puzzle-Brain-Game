package com.example.domain.engine

import com.example.domain.model.BlockShape
import com.example.domain.model.Board
import com.example.domain.model.HintMove

object MoveFinder {

    /**
     * Checks if any of the given shapes can legally fit anywhere on the board.
     * Game over occurs if none of the non-null shapes can be placed.
     */
    fun hasAnyLegalMove(board: Board, shapes: List<BlockShape?>): Boolean {
        for (shapeIndex in shapes.indices) {
            val shape = shapes[shapeIndex] ?: continue
            if (canPlaceShapeAnywhere(board, shape)) {
                return true
            }
        }
        return false
    }

    fun canPlaceShapeAnywhere(board: Board, shape: BlockShape): Boolean {
        val maxR = board.size - shape.height
        val maxC = board.size - shape.width
        if (maxR < 0 || maxC < 0) return false

        for (r in 0..maxR) {
            for (c in 0..maxC) {
                if (PlacementEngine.canPlace(board, shape, r, c)) {
                    return true
                }
            }
        }
        return false
    }

    /**
     * Finds a legal placement hint for the available shapes.
     * Prioritizes moves that clear lines, otherwise returns the first valid position found.
     */
    fun findHint(board: Board, shapes: List<BlockShape?>): HintMove? {
        var fallbackMove: HintMove? = null

        for (shapeIndex in shapes.indices) {
            val shape = shapes[shapeIndex] ?: continue
            val maxR = board.size - shape.height
            val maxC = board.size - shape.width
            if (maxR < 0 || maxC < 0) continue

            for (r in 0..maxR) {
                for (c in 0..maxC) {
                    if (PlacementEngine.canPlace(board, shape, r, c)) {
                        // Test if this move would clear a line
                        val testBoard = PlacementEngine.place(board, shape, r, c)
                        val clears = LineClearEngine.findCompletedLines(testBoard)
                        if (clears.hasClears) {
                            return HintMove(shapeIndex = shapeIndex, targetRow = r, targetCol = c)
                        }
                        if (fallbackMove == null) {
                            fallbackMove = HintMove(shapeIndex = shapeIndex, targetRow = r, targetCol = c)
                        }
                    }
                }
            }
        }
        return fallbackMove
    }

    /**
     * For Second Chance / Continue:
     * Clears occupied cells from the most congested areas (the 2 rows and 2 columns with highest occupancy)
     * to guarantee ample legal moves for the remaining tray shapes.
     */
    fun createSecondChanceBoard(board: Board): Board {
        // Count occupancy per row and per col
        val rowCounts = IntArray(board.size) { r ->
            (0 until board.size).count { c -> board.isOccupied(r, c) }
        }
        val colCounts = IntArray(board.size) { c ->
            (0 until board.size).count { r -> board.isOccupied(r, c) }
        }

        // Find top 2 most occupied rows and columns
        val topRows = rowCounts.indices.sortedByDescending { rowCounts[it] }.take(2)
        val topCols = colCounts.indices.sortedByDescending { colCounts[it] }.take(2)

        return LineClearEngine.clearLines(board, topRows, topCols)
    }

    /**
     * Clears exactly 2 lines (rows or columns) with the highest block occupancy,
     * fulfilling the rewarded ad life condition.
     */
    fun clearTwoLines(board: Board): Board {
        val rowCounts = IntArray(board.size) { r ->
            (0 until board.size).count { c -> board.isOccupied(r, c) }
        }
        val colCounts = IntArray(board.size) { c ->
            (0 until board.size).count { r -> board.isOccupied(r, c) }
        }

        val bestRows = rowCounts.indices.map { r -> Triple(true, r, rowCounts[r]) }
        val bestCols = colCounts.indices.map { c -> Triple(false, c, colCounts[c]) }

        val top2Lines = (bestRows + bestCols)
            .sortedByDescending { it.third }
            .take(2)

        val rowsToClear = top2Lines.filter { it.first }.map { it.second }
        val colsToClear = top2Lines.filter { !it.first }.map { it.second }

        return LineClearEngine.clearLines(board, rowsToClear, colsToClear)
    }
}
