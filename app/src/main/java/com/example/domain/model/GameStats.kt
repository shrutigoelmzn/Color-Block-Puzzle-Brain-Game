package com.example.domain.model

data class GameStats(
    val bestScore: Int = 0,
    val bestTimedScore: Int = 0,
    val highestCombo: Int = 0,
    val totalScore: Long = 0L,
    val totalLinesCleared: Int = 0,
    val totalBlocksPlaced: Int = 0,
    val gamesPlayed: Int = 0,
    val dailyChallengesCompleted: Int = 0,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val coins: Int = 100, // Welcome gift
    val activeThemeId: String = "classic",
    val unlockedThemeIds: Set<String> = setOf("classic")
)
