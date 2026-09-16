package com.example.domain.model

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val currentProgress: Int,
    val target: Int,
    val rewardCoins: Int,
    val isClaimed: Boolean = false
) {
    val isCompleted: Boolean = currentProgress >= target

    companion object {
        fun defaultList(): List<Achievement> = listOf(
            Achievement("score_1000", "Block Beginner", "Score 1,000 points in any mode", 0, 1000, 50),
            Achievement("score_5000", "Block Master", "Score 5,000 points in any mode", 0, 5000, 100),
            Achievement("score_10000", "Puzzle Legend", "Score 10,000 points in any mode", 0, 10000, 200),
            Achievement("combo_3", "Combo Starter", "Trigger a 3x Combo streak", 0, 3, 30),
            Achievement("combo_5", "Combo Hot Streak", "Trigger a 5x Combo streak", 0, 5, 80),
            Achievement("combo_10", "Combo God", "Trigger a 10x Combo streak", 0, 10, 250),
            Achievement("lines_100", "Line Sweeper", "Clear 100 total lines", 0, 100, 75),
            Achievement("lines_500", "Grid Cleanser", "Clear 500 total lines", 0, 500, 200),
            Achievement("games_10", "Dedicated Player", "Play 10 puzzle games", 0, 10, 50),
            Achievement("games_50", "Color Block Addict", "Play 50 puzzle games", 0, 50, 150),
            Achievement("daily_1", "Day One Hero", "Complete your first Daily Challenge", 0, 1, 60),
            Achievement("daily_7", "Weekly Challenger", "Complete 7 Daily Challenges", 0, 7, 180),
            Achievement("streak_7", "Streak Champion", "Maintain a 7-day daily streak", 0, 7, 300),
            Achievement("theme_1", "Style Upgrade", "Unlock your first custom theme", 0, 1, 100)
        )
    }
}

data class Mission(
    val id: String,
    val title: String,
    val description: String,
    val currentProgress: Int,
    val target: Int,
    val rewardCoins: Int,
    val isClaimed: Boolean = false
) {
    val isCompleted: Boolean = currentProgress >= target

    companion object {
        fun dailyMissions(): List<Mission> = listOf(
            Mission("m_lines_20", "Row Cleaner", "Clear 20 lines today", 0, 20, 40),
            Mission("m_combo_5", "Combo Virtuoso", "Trigger a 4x or higher combo", 0, 4, 50),
            Mission("m_score_3000", "High Scorer", "Score 3,000 points today", 0, 3000, 60),
            Mission("m_blocks_50", "Block Builder", "Place 50 blocks today", 0, 50, 45),
            Mission("m_multi_line", "Double Impact", "Clear 2+ lines in a single move 3 times", 0, 3, 50),
            Mission("m_games_3", "Game Triad", "Play 3 game sessions", 0, 3, 35)
        )
    }
}
