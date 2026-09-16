package com.example.domain.model

data class LeaderboardEntry(
    val id: String,
    val rank: Int = 0,
    val playerName: String,
    val score: Int,
    val mode: GameMode = GameMode.CLASSIC,
    val linesCleared: Int = 0,
    val highestCombo: Int = 0,
    val dateString: String = "",
    val isUser: Boolean = false,
    val isPersonalBest: Boolean = false
) {
    companion object {
        fun defaultGlobalLeaderboard(): List<LeaderboardEntry> = listOf(
            LeaderboardEntry("g1", 1, "NovaBlade ⚡", 18450, GameMode.CLASSIC, 142, 8, "2026-09-12"),
            LeaderboardEntry("g2", 2, "ZenMaster 🧘", 15200, GameMode.CLASSIC, 118, 7, "2026-09-14"),
            LeaderboardEntry("g3", 3, "PixelHero 🎮", 12800, GameMode.CLASSIC, 96, 6, "2026-09-15"),
            LeaderboardEntry("g4", 4, "BlockSmith 🧱", 9950, GameMode.CLASSIC, 74, 5, "2026-09-10"),
            LeaderboardEntry("g5", 5, "ColorRush 🎨", 7400, GameMode.CLASSIC, 58, 4, "2026-09-13"),
            LeaderboardEntry("g6", 6, "CyberCube 💎", 5600, GameMode.CLASSIC, 42, 4, "2026-09-15"),
            LeaderboardEntry("g7", 7, "ApexPuzzler 🌟", 4200, GameMode.CLASSIC, 32, 3, "2026-09-14"),
            LeaderboardEntry("g8", 8, "GridSeeker 🚀", 2850, GameMode.CLASSIC, 24, 3, "2026-09-16")
        )
    }
}
