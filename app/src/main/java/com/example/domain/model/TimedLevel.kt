package com.example.domain.model

enum class LevelDifficulty(val label: String, val colorHex: Long) {
    EASY("Easy", 0xFF10B981),
    MEDIUM("Medium", 0xFFF59E0B),
    HARD("Hard", 0xFFEF4444),
    MASTER("Master", 0xFF8B5CF6)
}

data class TimedLevel(
    val levelNumber: Int,
    val title: String,
    val difficulty: LevelDifficulty,
    val targetScore: Int,
    val targetLines: Int,
    val targetCombos: Int,
    val timeLimitSeconds: Int,
    val rewardCoins: Int
) {
    fun isGoalMet(score: Int, lines: Int, highestCombo: Int): Boolean {
        return score >= targetScore && lines >= targetLines && highestCombo >= targetCombos
    }
}
