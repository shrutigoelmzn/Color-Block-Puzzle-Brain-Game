package com.example

import com.example.domain.engine.BlockGenerator
import com.example.domain.engine.ComboManager
import com.example.domain.engine.DailyChallengeGenerator
import com.example.domain.engine.LineClearEngine
import com.example.domain.engine.MoveFinder
import com.example.domain.engine.PlacementEngine
import com.example.domain.engine.ScoreCalculator
import com.example.domain.model.BlockShape
import com.example.domain.model.Board
import com.example.domain.model.Cell
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GameEngineTest {

    @Test
    fun shapeFitsOnEmptyBoard() {
        val board = Board.empty()
        val shape = BlockShape.square2()
        assertTrue(PlacementEngine.canPlace(board, shape, 0, 0))
        assertTrue(PlacementEngine.canPlace(board, shape, 6, 6))
    }

    @Test
    fun shapeRejectedOutsideBoard() {
        val board = Board.empty()
        val shape = BlockShape.square2()
        // Out of bounds on right
        assertFalse(PlacementEngine.canPlace(board, shape, 0, 7))
        // Out of bounds on bottom
        assertFalse(PlacementEngine.canPlace(board, shape, 7, 0))
        // Negative coordinates
        assertFalse(PlacementEngine.canPlace(board, shape, -1, 0))
    }

    @Test
    fun shapeRejectedOnOccupiedCells() {
        var board = Board.empty()
        board = board.withCell(2, 2, Cell(colorId = 1))

        val shape = BlockShape.square2()
        // (2,2) is covered if placed at (1,1), (1,2), (2,1), or (2,2)
        assertFalse(PlacementEngine.canPlace(board, shape, 2, 2))
        assertFalse(PlacementEngine.canPlace(board, shape, 1, 1))

        // But should fit at (3,3)
        assertTrue(PlacementEngine.canPlace(board, shape, 3, 3))
    }

    @Test
    fun fullRowDetectionAndClear() {
        var board = Board.empty()
        // Fill row 3
        for (c in 0 until 8) {
            board = board.withCell(3, c, Cell(colorId = 2))
        }

        val completed = LineClearEngine.findCompletedLines(board)
        assertEquals(listOf(3), completed.rows)
        assertTrue(completed.cols.isEmpty())

        val clearedBoard = LineClearEngine.clearLines(board, completed.rows, completed.cols)
        for (c in 0 until 8) {
            assertTrue(clearedBoard.isEmpty(3, c))
        }
    }

    @Test
    fun fullColumnDetectionAndClear() {
        var board = Board.empty()
        // Fill column 5
        for (r in 0 until 8) {
            board = board.withCell(r, 5, Cell(colorId = 3))
        }

        val completed = LineClearEngine.findCompletedLines(board)
        assertTrue(completed.rows.isEmpty())
        assertEquals(listOf(5), completed.cols)

        val clearedBoard = LineClearEngine.clearLines(board, completed.rows, completed.cols)
        for (r in 0 until 8) {
            assertTrue(clearedBoard.isEmpty(r, 5))
        }
    }

    @Test
    fun multipleSimultaneousLineClears() {
        var board = Board.empty()
        // Fill row 2 and col 4 (cross shape)
        for (i in 0 until 8) {
            board = board.withCell(2, i, Cell(colorId = 1))
            board = board.withCell(i, 4, Cell(colorId = 2))
        }

        val completed = LineClearEngine.findCompletedLines(board)
        assertEquals(listOf(2), completed.rows)
        assertEquals(listOf(4), completed.cols)
        assertEquals(2, completed.totalLines)

        val clearedBoard = LineClearEngine.clearLines(board, completed.rows, completed.cols)
        assertTrue(clearedBoard.isEmpty(2, 4))
        assertTrue(clearedBoard.isEmpty(2, 0))
        assertTrue(clearedBoard.isEmpty(0, 4))
    }

    @Test
    fun scoreAndComboCalculation() {
        val shape = BlockShape.square2() // 4 blocks
        assertEquals(40, ScoreCalculator.calculatePlacementScore(shape))

        // Line clears
        assertEquals(100, ScoreCalculator.calculateLineClearScore(linesCount = 1, combo = 1))
        assertEquals(300, ScoreCalculator.calculateLineClearScore(linesCount = 2, combo = 1))
        // Combo 3 with 1 line: 100 * 2.0x multiplier = 200
        assertEquals(200, ScoreCalculator.calculateLineClearScore(linesCount = 1, combo = 3))

        // Combo increment and reset
        assertEquals(1, ComboManager.updateCombo(currentCombo = 0, linesCleared = 1))
        assertEquals(2, ComboManager.updateCombo(currentCombo = 1, linesCleared = 1))
        assertEquals(0, ComboManager.updateCombo(currentCombo = 3, linesCleared = 0))
    }

    @Test
    fun gameOverDetectionAndHintSolver() {
        // Almost completely filled board with no 2x2 holes
        var board = Board.empty()
        for (r in 0 until 8) {
            for (c in 0 until 8) {
                // Checkerboard occupancy
                if ((r + c) % 2 == 0) {
                    board = board.withCell(r, c, Cell(colorId = 0))
                }
            }
        }

        // Square 2x2 cannot fit in checkerboard
        val square = BlockShape.square2()
        assertFalse(MoveFinder.hasAnyLegalMove(board, listOf(square)))

        // Dot 1x1 CAN fit in the empty checkerboard cells
        val dot = BlockShape.dot()
        assertTrue(MoveFinder.hasAnyLegalMove(board, listOf(dot)))

        // Hint finder returns legal move
        val hint = MoveFinder.findHint(board, listOf(square, dot))
        assertNotNull(hint)
        assertEquals(1, hint?.shapeIndex) // The dot at index 1
        assertTrue(PlacementEngine.canPlace(board, dot, hint!!.targetRow, hint.targetCol))
    }

    @Test
    fun seededGeneratorAndDailyChallengeAreDeterministic() {
        val gen1 = BlockGenerator.seeded(12345L)
        val gen2 = BlockGenerator.seeded(12345L)
        val emptyBoard = Board.empty()

        val tray1 = gen1.generateTray(emptyBoard, 0)
        val tray2 = gen2.generateTray(emptyBoard, 0)

        assertEquals(tray1.map { it.id }, tray2.map { it.id })

        val daily1 = DailyChallengeGenerator.generateForDate("2026-09-15")
        val daily2 = DailyChallengeGenerator.generateForDate("2026-09-15")
        assertEquals(daily1.title, daily2.title)
        assertEquals(daily1.targetScore, daily2.targetScore)
        assertEquals(daily1.targetLines, daily2.targetLines)
    }

    @Test
    fun rewardedAdReviveRulesAndClearTwoLines() {
        val timedLevel = com.example.domain.engine.TimedLevelGenerator.getLevel(1) // target: 350
        val stateInitial = com.example.domain.model.GameState(
            mode = com.example.domain.model.GameMode.TIMED,
            currentTimedLevel = timedLevel,
            score = 100, // < 80% of 350 (280)
            revivesUsed = 0
        )
        // 1st revive can be taken at any time
        assertTrue(stateInitial.canTakeReviveWithAd)

        // After 1st revive used, with score 100 (below 80% of 350 = 280), 2nd revive is locked
        val stateAfterFirstReviveLowScore = stateInitial.copy(revivesUsed = 1, score = 200)
        assertFalse(stateAfterFirstReviveLowScore.isNearTargetScore)
        assertFalse(stateAfterFirstReviveLowScore.canTakeReviveWithAd)

        // When score reaches 80%+ (e.g. 290 >= 280), 2nd revive unlocks
        val stateAfterFirstReviveHighScore = stateInitial.copy(revivesUsed = 1, score = 290)
        assertTrue(stateAfterFirstReviveHighScore.isNearTargetScore)
        assertTrue(stateAfterFirstReviveHighScore.canTakeReviveWithAd)

        // Max 2 revives: after 2 revives used, no more revives allowed
        val stateMaxRevives = stateInitial.copy(revivesUsed = 2, score = 300)
        assertFalse(stateMaxRevives.canTakeReviveWithAd)

        // Test clearTwoLines clears occupied lines
        var board = Board.empty()
        for (c in 0 until 8) {
            board = board.withCell(0, c, Cell(1))
            board = board.withCell(1, c, Cell(2))
        }
        assertEquals(16, board.countOccupied())
        val cleared = MoveFinder.clearTwoLines(board)
        assertEquals(0, cleared.countOccupied())

        // Test time extension rewarded ad: available when time expired in TIMED mode without reaching target
        val timedStateExpired = stateInitial.copy(
            isGameOver = true,
            isTimeOutGameOver = true,
            isLevelCompleted = false,
            timeRemainingSeconds = 0,
            timeExtensionsUsed = 0
        )
        assertTrue(timedStateExpired.canTakeTimeExtensionWithAd)

        // After max time extensions used (2), time extension is no longer available
        val timedStateMaxExtensions = timedStateExpired.copy(timeExtensionsUsed = 2)
        assertFalse(timedStateMaxExtensions.canTakeTimeExtensionWithAd)
    }

    @Test
    fun allTrayBlocksFitGridAndAtLeastTwoArePerfectFits() {
        val generator = BlockGenerator()

        // 1. Test on empty board
        val emptyBoard = Board.empty()
        val trayEmpty = generator.generateTray(emptyBoard, 0)
        assertEquals(3, trayEmpty.size)

        // All three blocks must fit in the grid
        for (shape in trayEmpty) {
            assertTrue(
                "Shape ${shape.id} should fit in grid",
                MoveFinder.canPlaceShapeAnywhere(emptyBoard, shape)
            )
        }

        // At least two must be perfect fits
        val perfectFitsOnEmpty = trayEmpty.count { shape ->
            MoveFinder.canClearAnyLine(emptyBoard, shape) ||
                MoveFinder.countValidPlacements(emptyBoard, shape) >= 3 ||
                shape.blockCount <= 3
        }
        assertTrue("At least two shapes should be perfect fits, got $perfectFitsOnEmpty", perfectFitsOnEmpty >= 2)

        // 2. Test on a crowded board (e.g. 50+ occupied cells)
        var crowdedBoard = Board.empty()
        // Fill several rows leaving specific cavities
        for (r in 0 until 6) {
            for (c in 0 until 7) {
                crowdedBoard = crowdedBoard.withCell(r, c, Cell(1))
            }
        }
        val trayCrowded = generator.generateTray(crowdedBoard, 2500)
        assertEquals(3, trayCrowded.size)

        // Check that ALL three blocks fit in the crowded grid
        for (shape in trayCrowded) {
            assertTrue(
                "Shape ${shape.id} must legally fit on crowded board",
                MoveFinder.canPlaceShapeAnywhere(crowdedBoard, shape)
            )
        }

        // And at least two are perfect fits
        val perfectFitsOnCrowded = trayCrowded.count { shape ->
            MoveFinder.canClearAnyLine(crowdedBoard, shape) ||
                MoveFinder.countValidPlacements(crowdedBoard, shape) >= 3 ||
                shape.blockCount <= 3
        }
        assertTrue(
            "At least two shapes must be perfect fits on crowded board, got $perfectFitsOnCrowded",
            perfectFitsOnCrowded >= 2
        )
    }

    @Test
    fun multiLineClearDetectionAndEventProperties() {
        // Test single line clear
        val singleClear = com.example.domain.model.ClearEffectEvent(
            rows = listOf(2),
            cols = emptyList(),
            scoreGained = 100,
            combo = 1,
            centerRow = 2f,
            centerCol = 3.5f
        )
        assertEquals(1, singleClear.totalLines)
        assertFalse(singleClear.isMultiLine)

        // Test double simultaneous line clear
        val doubleClear = com.example.domain.model.ClearEffectEvent(
            rows = listOf(1, 2),
            cols = emptyList(),
            scoreGained = 300,
            combo = 2,
            centerRow = 1.5f,
            centerCol = 3.5f
        )
        assertEquals(2, doubleClear.totalLines)
        assertTrue(doubleClear.isMultiLine)

        // Test cross simultaneous clear (1 row + 1 col = 2 lines)
        val crossClear = com.example.domain.model.ClearEffectEvent(
            rows = listOf(3),
            cols = listOf(4),
            scoreGained = 300,
            combo = 1,
            centerRow = 3f,
            centerCol = 4f
        )
        assertEquals(2, crossClear.totalLines)
        assertTrue(crossClear.isMultiLine)

        // Test triple simultaneous clear
        val tripleClear = com.example.domain.model.ClearEffectEvent(
            rows = listOf(0, 1),
            cols = listOf(2),
            scoreGained = 660,
            combo = 1,
            centerRow = 0.5f,
            centerCol = 2f
        )
        assertEquals(3, tripleClear.totalLines)
        assertTrue(tripleClear.isMultiLine)
    }
}
