package com.example.domain.model

data class UndoSnapshot(
    val board: Board,
    val availableShapes: List<BlockShape?>,
    val score: Int,
    val combo: Int,
    val linesClearedInGame: Int,
    val blocksPlacedInGame: Int
)

data class ClearEffectEvent(
    val rows: List<Int>,
    val cols: List<Int>,
    val scoreGained: Int,
    val combo: Int,
    val centerRow: Float,
    val centerCol: Float
)

data class ComboBannerEvent(
    val id: Long = System.currentTimeMillis(),
    val combo: Int,
    val linesCleared: Int,
    val title: String,
    val subtitle: String,
    val scoreGained: Int
)

data class FloatingScoreEvent(
    val id: Long = System.currentTimeMillis(),
    val text: String,
    val normalizedX: Float = 0.5f,
    val normalizedY: Float = 0.5f,
    val isCombo: Boolean = false
)

data class HintMove(
    val shapeIndex: Int,
    val targetRow: Int,
    val targetCol: Int
)

data class GameState(
    val board: Board = Board.empty(),
    val availableShapes: List<BlockShape?> = com.example.domain.engine.BlockGenerator().generateTray(Board.empty(), 0),
    val score: Int = 0,
    val bestScore: Int = 0,
    val combo: Int = 0,
    val highestComboInGame: Int = 0,
    val linesClearedInGame: Int = 0,
    val blocksPlacedInGame: Int = 0,
    val coins: Int = 0,
    val mode: GameMode = GameMode.CLASSIC,
    val isPaused: Boolean = false,
    val isGameOver: Boolean = false,
    val isNewBestScore: Boolean = false,
    val isNewBestCelebration: Boolean = false,
    val currentTimedLevel: TimedLevel? = null,
    val isLevelCompleted: Boolean = false,
    val completedLevelReward: Int = 0,
    val isDailyChallengeCompleted: Boolean = false,
    val dailyRewardCoins: Int = 0,
    val freeUndosRemaining: Int = 1,
    val hasUsedContinue: Boolean = false,
    val activeHint: HintMove? = null,
    val lastUndoSnapshot: UndoSnapshot? = null,
    val timeRemainingSeconds: Int = 120, // for Timed Mode
    val activeClearEffect: ClearEffectEvent? = null,
    val activeComboBanner: ComboBannerEvent? = null,
    val floatingScores: List<FloatingScoreEvent> = emptyList(),
    val recentlyPlacedCoords: Set<Pair<Int, Int>> = emptySet(),
    val clearingCoords: Set<Pair<Int, Int>> = emptySet()
)
