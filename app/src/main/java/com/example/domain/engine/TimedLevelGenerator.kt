package com.example.domain.engine

import com.example.domain.model.LevelDifficulty
import com.example.domain.model.TimedLevel

object TimedLevelGenerator {

    fun getLevel(levelNumber: Int): TimedLevel {
        val safeLevel = maxOf(1, levelNumber)
        return when (safeLevel) {
            1 -> TimedLevel(
                levelNumber = 1,
                title = "First Steps",
                difficulty = LevelDifficulty.EASY,
                targetScore = 350,
                targetLines = 2,
                targetCombos = 0,
                timeLimitSeconds = 90,
                rewardCoins = 35
            )
            2 -> TimedLevel(
                levelNumber = 2,
                title = "Line Builder",
                difficulty = LevelDifficulty.EASY,
                targetScore = 750,
                targetLines = 4,
                targetCombos = 0,
                timeLimitSeconds = 85,
                rewardCoins = 45
            )
            3 -> TimedLevel(
                levelNumber = 3,
                title = "Gentle Flow",
                difficulty = LevelDifficulty.EASY,
                targetScore = 1200,
                targetLines = 6,
                targetCombos = 1,
                timeLimitSeconds = 80,
                rewardCoins = 60
            )
            4 -> TimedLevel(
                levelNumber = 4,
                title = "Combo Spark",
                difficulty = LevelDifficulty.MEDIUM,
                targetScore = 1800,
                targetLines = 8,
                targetCombos = 1,
                timeLimitSeconds = 75,
                rewardCoins = 75
            )
            5 -> TimedLevel(
                levelNumber = 5,
                title = "Grid Pressure",
                difficulty = LevelDifficulty.MEDIUM,
                targetScore = 2500,
                targetLines = 10,
                targetCombos = 2,
                timeLimitSeconds = 70,
                rewardCoins = 95
            )
            6 -> TimedLevel(
                levelNumber = 6,
                title = "Swift Clears",
                difficulty = LevelDifficulty.MEDIUM,
                targetScore = 3300,
                targetLines = 12,
                targetCombos = 2,
                timeLimitSeconds = 65,
                rewardCoins = 115
            )
            7 -> TimedLevel(
                levelNumber = 7,
                title = "Blitz Strike",
                difficulty = LevelDifficulty.HARD,
                targetScore = 4200,
                targetLines = 14,
                targetCombos = 3,
                timeLimitSeconds = 60,
                rewardCoins = 140
            )
            8 -> TimedLevel(
                levelNumber = 8,
                title = "Furious Pace",
                difficulty = LevelDifficulty.HARD,
                targetScore = 5200,
                targetLines = 16,
                targetCombos = 3,
                timeLimitSeconds = 55,
                rewardCoins = 170
            )
            9 -> TimedLevel(
                levelNumber = 9,
                title = "Inferno Grid",
                difficulty = LevelDifficulty.HARD,
                targetScore = 6400,
                targetLines = 18,
                targetCombos = 4,
                timeLimitSeconds = 50,
                rewardCoins = 210
            )
            else -> {
                val offset = safeLevel - 9
                TimedLevel(
                    levelNumber = safeLevel,
                    title = "Master Grid #$safeLevel",
                    difficulty = LevelDifficulty.MASTER,
                    targetScore = 6400 + (offset * 1200),
                    targetLines = 18 + (offset * 2),
                    targetCombos = minOf(5, 4 + (offset / 3)),
                    timeLimitSeconds = maxOf(45, 50 - (offset * 2)),
                    rewardCoins = 220 + (offset * 30)
                )
            }
        }
    }
}
