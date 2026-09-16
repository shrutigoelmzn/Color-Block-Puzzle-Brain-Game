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
     *
     * Strict rules:
     * 1. Check all 3 suggestion blocks whether they fit in the grid.
     * 2. ONLY valid blocks that can fit on the current board are shown as options.
     * 3. AT LEAST TWO blocks are guaranteed to be a "perfect fit" (plentiful placement
     *    options, line-clearing capabilities, or compact/easy fit).
     */
    fun generateTray(board: Board, score: Int): List<BlockShape> {
        val occupiedCells = board.countOccupied()
        val totalCells = board.size * board.size
        val occupancyRatio = occupiedCells.toFloat() / totalCells

        // 1. Select base pool according to game progress and board congestion
        val pool = selectPool(score, occupancyRatio)

        // 2. Check all candidate blocks against the grid: ONLY valid blocks that can fit are kept
        val validInPool = pool.filter { MoveFinder.canPlaceShapeAnywhere(board, it) }
        val allValidShapes = if (validInPool.isNotEmpty()) {
            validInPool
        } else {
            val fallback = BlockShape.ALL_SHAPES.filter { MoveFinder.canPlaceShapeAnywhere(board, it) }
            if (fallback.isNotEmpty()) fallback else simpleShapes
        }

        // If no shape can fit at all (board 100% full), fallback to smallest shapes
        if (allValidShapes.isEmpty()) {
            return listOf(
                BlockShape.dot(random.nextInt(7)),
                BlockShape.dot(random.nextInt(7)),
                BlockShape.dot(random.nextInt(7))
            )
        }

        // 3. Identify "perfect fit" blocks:
        // - Shapes that can clear at least one line immediately on the board
        // - OR shapes with high placement freedom (>= 3 valid positions)
        // - OR small/compact shapes (<= 3 cells) that fit easily
        val lineClearingShapes = allValidShapes.filter { MoveFinder.canClearAnyLine(board, it) }
        val highPlacementShapes = allValidShapes.filter { MoveFinder.countValidPlacements(board, it) >= 3 }
        val compactShapes = allValidShapes.filter { it.blockCount <= 3 }

        val perfectFitPool = (lineClearingShapes + highPlacementShapes + compactShapes)
            .distinctBy { it.id }
            .ifEmpty { allValidShapes }

        // 4. Select at least two "perfect fit" blocks, and ensure the 3rd block is also valid
        val chosen = mutableListOf<BlockShape>()
        val usedIds = mutableSetOf<String>()

        // 1st block: Guaranteed Perfect Fit
        val p1 = pickUniqueWithRandomColor(perfectFitPool, usedIds)
        chosen.add(p1)
        usedIds.add(p1.id)

        // 2nd block: Guaranteed Perfect Fit (At least two perfect fits!)
        val p2 = pickUniqueWithRandomColor(perfectFitPool, usedIds)
        chosen.add(p2)
        usedIds.add(p2.id)

        // 3rd block: Valid block that fits the grid
        val p3 = pickUniqueWithRandomColor(allValidShapes, usedIds)
        chosen.add(p3)

        return chosen
    }

    /**
     * Finds a single valid shape that is guaranteed to fit on the current board.
     * Prefers a "perfect fit" (line clearing or high placement freedom).
     */
    fun findFittingShape(board: Board): BlockShape {
        val allValid = BlockShape.ALL_SHAPES.filter { MoveFinder.canPlaceShapeAnywhere(board, it) }
        if (allValid.isEmpty()) {
            return BlockShape.dot(random.nextInt(7))
        }

        val perfect = allValid.filter {
            MoveFinder.canClearAnyLine(board, it) || MoveFinder.countValidPlacements(board, it) >= 3
        }

        val pool = if (perfect.isNotEmpty()) perfect else allValid
        val template = pool[random.nextInt(pool.size)]
        return template.copy(colorId = random.nextInt(7))
    }

    private fun pickUniqueWithRandomColor(
        pool: List<BlockShape>,
        alreadyUsedIds: Set<String>
    ): BlockShape {
        val unused = pool.filterNot { alreadyUsedIds.contains(it.id) }
        val template = if (unused.isNotEmpty()) {
            unused[random.nextInt(unused.size)]
        } else {
            pool[random.nextInt(pool.size)]
        }
        val colorId = random.nextInt(7)
        return template.copy(colorId = colorId)
    }

    private fun selectPool(score: Int, occupancyRatio: Float): List<BlockShape> {
        // High occupancy -> give more simple/medium shapes to allow clearing
        if (occupancyRatio > 0.60f) {
            return simpleShapes + simpleShapes + mediumShapes
        }

        return when {
            // Early game
            score < 1000 -> simpleShapes + simpleShapes + mediumShapes
            // Mid game
            score < 4000 -> simpleShapes + mediumShapes + mediumShapes + hardShapes.take(3)
            // Late game
            else -> simpleShapes + mediumShapes + hardShapes
        }
    }

    companion object {
        fun seeded(seed: Long): BlockGenerator = BlockGenerator(Random(seed))
    }
}
