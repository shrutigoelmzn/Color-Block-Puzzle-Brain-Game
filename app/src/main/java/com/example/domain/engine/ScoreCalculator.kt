package com.example.domain.engine

import com.example.domain.model.BlockShape
import kotlin.math.roundToInt

data class ScoreBreakdown(
    val placementScore: Int,
    val lineBaseScore: Int,
    val simultaneousMultiplier: Float,
    val comboMultiplier: Float,
    val totalMultiplier: Float,
    val totalLineScore: Int,
    val totalEarned: Int,
    val bannerTitle: String?,
    val multiplierText: String?,
    val floatingText: String
)

object ScoreCalculator {

    /**
     * Points for simply placing a block shape on the board:
     * 10 points per cell filled.
     */
    fun calculatePlacementScore(shape: BlockShape): Int {
        return shape.blockCount * 10
    }

    /**
     * Multipliers for clearing multiple lines simultaneously in one drop:
     * 1 line: 1.0x
     * 2 lines: 1.5x (e.g. 200 * 1.5 = 300 pts)
     * 3 lines: 2.2x (e.g. 300 * 2.2 = 660 pts)
     * 4 lines: 3.0x (e.g. 400 * 3.0 = 1,200 pts)
     * 5+ lines: 4.0x+
     */
    fun getSimultaneousMultiplier(linesCount: Int): Float {
        return when (linesCount) {
            0 -> 1.0f
            1 -> 1.0f
            2 -> 1.5f
            3 -> 2.2f
            4 -> 3.0f
            else -> 3.0f + (linesCount - 4) * 0.8f
        }
    }

    /**
     * Exponential combo streak multiplier for clearing lines in rapid succession (consecutive turns):
     * Turn 1 clear (streak 1): 1.0x
     * Turn 2 clear (streak 2): 1.5x
     * Turn 3 clear (streak 3): 2.0x
     * Turn 4 clear (streak 4): 2.6x
     * Turn 5 clear (streak 5): 3.3x
     * Turn 6+ clear: 4.0x + 0.8x per subsequent streak
     */
    fun getComboStreakMultiplier(combo: Int): Float {
        return when {
            combo <= 1 -> 1.0f
            combo == 2 -> 1.5f
            combo == 3 -> 2.0f
            combo == 4 -> 2.6f
            combo == 5 -> 3.3f
            else -> 3.3f + (combo - 5) * 0.8f
        }
    }

    /**
     * Calculates the complete line clear score incorporating simultaneous lines
     * and consecutive combo multipliers, producing exponential rewards.
     */
    fun calculateScoreBreakdown(linesCount: Int, combo: Int): ScoreBreakdown {
        if (linesCount <= 0) {
            return ScoreBreakdown(
                placementScore = 0,
                lineBaseScore = 0,
                simultaneousMultiplier = 1.0f,
                comboMultiplier = 1.0f,
                totalMultiplier = 1.0f,
                totalLineScore = 0,
                totalEarned = 0,
                bannerTitle = null,
                multiplierText = null,
                floatingText = ""
            )
        }

        val basePerLine = 100
        val lineBaseScore = linesCount * basePerLine
        val simultaneousMult = getSimultaneousMultiplier(linesCount)
        val comboMult = getComboStreakMultiplier(combo)

        val totalMultiplier = simultaneousMult * comboMult
        val totalLineScore = (lineBaseScore * totalMultiplier).roundToInt()

        // Banner title like top puzzle games (Block Blast, Woodoku)
        val bannerTitle = when {
            combo >= 5 -> "UNBELIEVABLE!"
            combo == 4 -> "SPECTACULAR!"
            combo == 3 -> "AMAZING!"
            combo == 2 -> "GREAT!"
            linesCount >= 4 -> "QUAD CLEAR!"
            linesCount == 3 -> "TRIPLE CLEAR!"
            linesCount == 2 -> "DOUBLE CLEAR!"
            else -> null
        }

        val multiplierText = when {
            combo >= 2 && linesCount >= 2 -> "${"%.1f".format(totalMultiplier)}x MEGA MULTIPLIER!"
            combo >= 2 -> "${combo}x COMBO STREAK!"
            linesCount >= 2 -> "${"%.1f".format(simultaneousMult)}x MULTI-LINE!"
            else -> null
        }

        val floatingText = when {
            combo >= 2 && linesCount >= 2 -> "+$totalLineScore (${"%.1f".format(totalMultiplier)}x MEGA!)"
            combo >= 2 -> "+$totalLineScore (${combo}x COMBO!)"
            linesCount >= 2 -> "+$totalLineScore (${linesCount} LINES!)"
            else -> "+$totalLineScore"
        }

        return ScoreBreakdown(
            placementScore = 0,
            lineBaseScore = lineBaseScore,
            simultaneousMultiplier = simultaneousMult,
            comboMultiplier = comboMult,
            totalMultiplier = totalMultiplier,
            totalLineScore = totalLineScore,
            totalEarned = totalLineScore,
            bannerTitle = bannerTitle,
            multiplierText = multiplierText,
            floatingText = floatingText
        )
    }

    /**
     * Backward-compatible simple calculation helper
     */
    fun calculateLineClearScore(linesCount: Int, combo: Int): Int {
        return calculateScoreBreakdown(linesCount, combo).totalLineScore
    }
}

object ComboManager {
    /**
     * Increments combo by linesCleared if at least one line was cleared
     * (e.g. clearing 2 lines simultaneously awards +2 to the combo streak);
     * resets to 0 if no line was cleared.
     */
    fun updateCombo(currentCombo: Int, linesCleared: Int): Int {
        return if (linesCleared > 0) {
            currentCombo + linesCleared
        } else {
            0
        }
    }
}

