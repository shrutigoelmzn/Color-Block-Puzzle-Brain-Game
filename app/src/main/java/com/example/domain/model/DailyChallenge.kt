package com.example.domain.model

data class DailyChallenge(
    val dateKey: String, // e.g. "2026-09-15"
    val title: String,
    val description: String,
    val targetScore: Int,
    val targetLines: Int,
    val targetCombos: Int,
    val rewardCoins: Int = 100,
    val isCompleted: Boolean = false
) {
    fun isGoalMet(score: Int, lines: Int, highestCombo: Int): Boolean {
        return score >= targetScore && lines >= targetLines && highestCombo >= targetCombos
    }
}

data class DailyStreak(
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val lastClaimDate: String = "",
    val hasClaimedToday: Boolean = false
) {
    fun getRewardForDay(day: Int): Int {
        return when (day) {
            1 -> 20
            2 -> 30
            3 -> 40
            4 -> 50
            5 -> 60
            6 -> 80
            else -> 120 // Day 7+
        }
    }
}
