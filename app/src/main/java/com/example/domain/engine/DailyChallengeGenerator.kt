package com.example.domain.engine

import com.example.domain.model.DailyChallenge
import kotlin.math.abs
import kotlin.random.Random

object DailyChallengeGenerator {

    fun generateForDate(dateKey: String): DailyChallenge {
        val seed = dateKey.hashCode().toLong()
        val rng = Random(seed)

        // Varied challenge types based on day
        val targetScore = 1500 + rng.nextInt(5) * 500 // 1500, 2000, 2500, 3000, 3500
        val targetLines = 8 + rng.nextInt(5) * 2     // 8, 10, 12, 14, 16
        val targetCombos = 2 + rng.nextInt(3)        // 2, 3, 4

        val titles = listOf(
            "Cosmic Alignment",
            "Grid Mastery",
            "Color Symphony",
            "Line Blitz",
            "Combo Surge",
            "Block Harmony",
            "Zen Puzzle",
            "Prism Cascade",
            "Quantum Stacker",
            "Eclipse Crunch",
            "Nebula Sweep",
            "Solar Clears"
        )
        val dayIndex = abs(seed.toInt()) % titles.size
        val title = titles[dayIndex]

        val description = "Score $targetScore pts, clear $targetLines lines, and reach a ${targetCombos}x combo!"

        return DailyChallenge(
            dateKey = dateKey,
            title = title,
            description = description,
            targetScore = targetScore,
            targetLines = targetLines,
            targetCombos = targetCombos,
            rewardCoins = 100 + (abs(seed.toInt()) % 3) * 25,
            isCompleted = false
        )
    }
}
