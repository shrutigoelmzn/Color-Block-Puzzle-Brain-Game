package com.example.domain.engine

import com.example.domain.model.BlockShape
import com.example.domain.model.Board
import kotlin.random.Random

class BlockGenerator(private val random: Random = Random.Default) {

    // Small / recovery shapes
    private val simpleShapes = listOf(
        BlockShape.dot(0),
        BlockShape.line2H(1),
        BlockShape.line2V(1),
        BlockShape.line3H(2),
        BlockShape.line3V(2),
        BlockShape.corner2TL(2),
        BlockShape.corner2TR(2),
        BlockShape.corner2BL(2),
        BlockShape.corner2BR(2),
        BlockShape.square2(0)
    )

    // Medium shapes
    private val mediumShapes = listOf(
        BlockShape.line3H(2),
        BlockShape.line3V(2),
        BlockShape.square2(0),
        BlockShape.lStandardBL(1),
        BlockShape.lStandardBR(1),
        BlockShape.lStandardTL(1),
        BlockShape.lStandardTR(1),
        BlockShape.tShapeUp(4),
        BlockShape.tShapeDown(4),
        BlockShape.tShapeLeft(4),
        BlockShape.tShapeRight(4),
        BlockShape.zShapeH(5),
        BlockShape.sShapeH(5),
        BlockShape.plus(6)
    )

    // Large / challenging shapes
    private val hardShapes = listOf(
        BlockShape.line4H(3),
        BlockShape.line4V(3),
        BlockShape.line5H(4),
        BlockShape.line5V(4),
        BlockShape.square3(5),
        BlockShape.lShape3TL(3),
        BlockShape.lShape3TR(3),
        BlockShape.lShape3BL(3),
        BlockShape.lShape3BR(3),
        BlockShape.plus(6)
    )

    /**
     * Generates a 3-piece shape tray tailored to current board state and score.
     * Ensures at least one piece in the set can be legally placed on the board!
     */
    fun generateTray(board: Board, score: Int): List<BlockShape> {
        val occupiedCells = board.countOccupied()
        val totalCells = board.size * board.size
        val occupancyRatio = occupiedCells.toFloat() / totalCells

        val pool = selectPool(score, occupancyRatio)

        var candidateSet: List<BlockShape>
        var attempts = 0
        do {
            // Pick 3 shapes from pool with color variety
            val shape1 = pickRandomWithRandomColor(pool)
            val shape2 = pickRandomWithRandomColor(pool)
            val shape3 = pickRandomWithRandomColor(pool)
            candidateSet = listOf(shape1, shape2, shape3)
            attempts++

            // If board is very crowded or attempts > 5, ensure at least one piece has a legal move
            if (MoveFinder.hasAnyLegalMove(board, candidateSet)) {
                return candidateSet
            }
        } while (attempts < 15)

        // Fallback: If no legal moves in random draws, inject at least one small recovery piece that fits
        val recoveryShape = findFittingShape(board) ?: BlockShape.dot(random.nextInt(7))
        return listOf(
            recoveryShape,
            pickRandomWithRandomColor(pool),
            pickRandomWithRandomColor(pool)
        )
    }

    private fun selectPool(score: Int, occupancyRatio: Float): List<BlockShape> {
        // High occupancy -> give more simple/medium shapes to allow clearing
        if (occupancyRatio > 0.65f) {
            return simpleShapes + mediumShapes
        }

        return when {
            // Early game
            score < 1000 -> simpleShapes + simpleShapes + mediumShapes
            // Mid game
            score < 4000 -> simpleShapes + mediumShapes + mediumShapes + hardShapes.take(4)
            // Late game
            else -> mediumShapes + hardShapes + simpleShapes.take(3)
        }
    }

    private fun pickRandomWithRandomColor(pool: List<BlockShape>): BlockShape {
        val template = pool[random.nextInt(pool.size)]
        val colorId = random.nextInt(7)
        return template.copy(colorId = colorId)
    }

    private fun findFittingShape(board: Board): BlockShape? {
        val candidates = simpleShapes.shuffled(random)
        for (shape in candidates) {
            if (MoveFinder.canPlaceShapeAnywhere(board, shape)) {
                return shape.copy(colorId = random.nextInt(7))
            }
        }
        return null
    }

    companion object {
        fun seeded(seed: Long): BlockGenerator = BlockGenerator(Random(seed))
    }
}
