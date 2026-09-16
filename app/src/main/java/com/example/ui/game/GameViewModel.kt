package com.example.ui.game

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.HapticFeedbackManager
import com.example.audio.SoundEffectManager
import com.example.data.repository.GameRepository
import com.example.domain.engine.BlockGenerator
import com.example.domain.engine.ComboManager
import com.example.domain.engine.LineClearEngine
import com.example.domain.engine.MoveFinder
import com.example.domain.engine.PlacementEngine
import com.example.domain.engine.ScoreCalculator
import com.example.domain.engine.TimedLevelGenerator
import com.example.domain.model.AppThemeMode
import com.example.domain.model.ClearEffectEvent
import com.example.domain.model.ComboBannerEvent
import com.example.domain.model.FloatingScoreEvent
import com.example.domain.model.GameMode
import com.example.domain.model.GameState
import com.example.domain.model.GameTheme
import com.example.domain.model.HintMove
import com.example.domain.model.LeaderboardEntry
import com.example.domain.model.TimedLevel
import com.example.domain.model.UndoSnapshot
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GameViewModel(application: Application) : AndroidViewModel(application) {

    val repository = GameRepository(application)
    val soundManager = SoundEffectManager()
    val hapticManager = HapticFeedbackManager(application)

    private val blockGenerator = BlockGenerator()

    private val _gameState = MutableStateFlow(GameState())
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    val activeTheme: StateFlow<GameTheme> = repository.activeThemeFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, GameTheme.CLASSIC)

    val themeMode: StateFlow<AppThemeMode> = repository.themeModeFlow
        .map { AppThemeMode.fromId(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppThemeMode.SYSTEM)

    val stats = repository.statsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, com.example.domain.model.GameStats())

    val dailyChallenge = repository.dailyChallengeFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val dailyStreak = repository.dailyStreakFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val achievements = repository.achievementsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val missions = repository.missionsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val timedLevel: StateFlow<Int> = repository.timedLevelFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, 1)

    val leaderboard: StateFlow<List<LeaderboardEntry>> = repository.leaderboardFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, LeaderboardEntry.defaultGlobalLeaderboard())

    private var timerJob: Job? = null
    private var idleHintJob: Job? = null

    init {
        // Sync sound and haptics settings
        viewModelScope.launch {
            repository.soundEnabledFlow.collect { soundManager.isSoundEnabled = it }
        }
        viewModelScope.launch {
            repository.hapticsEnabledFlow.collect { hapticManager.isHapticsEnabled = it }
        }
        viewModelScope.launch {
            repository.resetDailyMissionsIfNewDay(repository.getTodayDateKey())
        }
    }

    fun startNewGame(mode: GameMode = GameMode.CLASSIC) {
        viewModelScope.launch {
            val best = if (mode == GameMode.TIMED) {
                repository.bestTimedScoreFlow.first()
            } else {
                repository.bestScoreFlow.first()
            }
            val coins = repository.coinsFlow.first()
            val initialBoard = com.example.domain.model.Board.empty()
            val initialTray = blockGenerator.generateTray(initialBoard, 0)

            val timedLevelData = if (mode == GameMode.TIMED) {
                val levelNum = repository.timedLevelFlow.first()
                TimedLevelGenerator.getLevel(levelNum)
            } else {
                null
            }

            _gameState.value = GameState(
                board = initialBoard,
                availableShapes = initialTray,
                score = 0,
                bestScore = best,
                combo = 0,
                highestComboInGame = 0,
                linesClearedInGame = 0,
                blocksPlacedInGame = 0,
                coins = coins,
                mode = mode,
                isPaused = false,
                isGameOver = false,
                isNewBestScore = false,
                isNewBestCelebration = false,
                currentTimedLevel = timedLevelData,
                isLevelCompleted = false,
                completedLevelReward = 0,
                isDailyChallengeCompleted = false,
                dailyRewardCoins = 0,
                freeUndosRemaining = 1,
                hasUsedContinue = false,
                revivesUsed = 0,
                timeExtensionsUsed = 0,
                timeRemainingSeconds = timedLevelData?.timeLimitSeconds ?: 0,
                isTimeOutGameOver = false,
                lastUndoSnapshot = null,
                activeHint = null
            )

            startIdleHintTimer()

            if (mode == GameMode.TIMED) {
                startTimedModeCountdown()
            } else {
                timerJob?.cancel()
            }
        }
    }

    private fun startTimedModeCountdown() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_gameState.value.timeRemainingSeconds > 0 && !_gameState.value.isGameOver) {
                delay(1000)
                if (!_gameState.value.isPaused) {
                    _gameState.update { it.copy(timeRemainingSeconds = it.timeRemainingSeconds - 1) }
                    if (_gameState.value.timeRemainingSeconds <= 0) {
                        endGame(isTimeOut = true)
                    }
                }
            }
        }
    }

    fun pauseGame() {
        timerJob?.cancel()
        idleHintJob?.cancel()
        _gameState.update { it.copy(isPaused = true) }
    }

    fun resumeGame() {
        _gameState.update { it.copy(isPaused = false) }
        startIdleHintTimer()
        if (_gameState.value.mode == GameMode.TIMED && !_gameState.value.isGameOver) {
            startTimedModeCountdown()
        }
    }

    fun dismissCelebration() {
        _gameState.update { it.copy(isNewBestCelebration = false, isLevelCompleted = false) }
    }

    fun dismissDailyCelebration() {
        _gameState.update { it.copy(isDailyChallengeCompleted = false) }
    }

    fun userInteracted() {
        _gameState.update { it.copy(activeHint = null) }
        startIdleHintTimer()
    }

    private fun startIdleHintTimer() {
        idleHintJob?.cancel()
        idleHintJob = viewModelScope.launch {
            // Wait 8 seconds of inactivity before showing subtle hint
            delay(8000)
            val state = _gameState.value
            if (!state.isGameOver && !state.isPaused) {
                val hint = MoveFinder.findHint(state.board, state.availableShapes)
                if (hint != null) {
                    _gameState.update { it.copy(activeHint = hint) }
                }
            }
        }
    }

    fun requestHint() {
        val state = _gameState.value
        if (state.isGameOver || state.isPaused) return
        val hint = MoveFinder.findHint(state.board, state.availableShapes)
        if (hint != null) {
            _gameState.update { it.copy(activeHint = hint) }
            soundManager.playClick()
        }
    }

    fun placeShape(shapeIndex: Int, boardRow: Int, boardCol: Int) {
        userInteracted()
        val state = _gameState.value
        if (state.isGameOver || state.isPaused) return

        val shape = state.availableShapes.getOrNull(shapeIndex) ?: return
        if (!PlacementEngine.canPlace(state.board, shape, boardRow, boardCol)) {
            soundManager.playInvalid()
            hapticManager.vibrateInvalid()
            return
        }

        // Save undo snapshot
        val snapshot = UndoSnapshot(
            board = state.board,
            availableShapes = state.availableShapes,
            score = state.score,
            combo = state.combo,
            linesClearedInGame = state.linesClearedInGame,
            blocksPlacedInGame = state.blocksPlacedInGame
        )

        // Place the shape
        val placedCoords = PlacementEngine.getCandidateCoordinates(shape, boardRow, boardCol).toSet()
        val placedBoard = PlacementEngine.place(state.board, shape, boardRow, boardCol)
        soundManager.playPlace(shape.blockCount)
        hapticManager.vibratePlace(shape.blockCount)

        val placementScore = ScoreCalculator.calculatePlacementScore(shape)
        var newScore = state.score + placementScore
        val newBlocksPlaced = state.blocksPlacedInGame + shape.blockCount

        // Consume shape from tray
        val updatedShapes = state.availableShapes.toMutableList()
        updatedShapes[shapeIndex] = null

        // Detect full lines
        val completedLines = LineClearEngine.findCompletedLines(placedBoard)
        val clearedRows = completedLines.rows
        val clearedCols = completedLines.cols
        val totalLinesCleared = completedLines.totalLines

        val newCombo = ComboManager.updateCombo(state.combo, totalLinesCleared)
        val highestCombo = maxOf(state.highestComboInGame, newCombo)
        val newTotalLines = state.linesClearedInGame + totalLinesCleared

        val finalBoard: com.example.domain.model.Board
        var clearEvent: ClearEffectEvent? = null
        var comboBanner: ComboBannerEvent? = null
        var floatingScore: FloatingScoreEvent? = null
        val clearingCoords = mutableSetOf<Pair<Int, Int>>()

        if (completedLines.hasClears) {
            val scoreBreakdown = ScoreCalculator.calculateScoreBreakdown(totalLinesCleared, newCombo)
            val lineScore = scoreBreakdown.totalLineScore
            newScore += lineScore

            // Add time bonus in Timed mode (+5s per line, +3s per combo)
            if (state.mode == GameMode.TIMED) {
                val bonusTime = (totalLinesCleared * 5) + (newCombo * 3)
                _gameState.update { it.copy(timeRemainingSeconds = it.timeRemainingSeconds + bonusTime) }
            }

            finalBoard = LineClearEngine.clearLines(placedBoard, clearedRows, clearedCols)

            for (r in clearedRows) {
                for (c in 0 until 8) clearingCoords.add(r to c)
            }
            for (c in clearedCols) {
                for (r in 0 until 8) clearingCoords.add(r to c)
            }

            clearEvent = ClearEffectEvent(
                rows = clearedRows,
                cols = clearedCols,
                scoreGained = lineScore,
                combo = newCombo,
                centerRow = if (clearedRows.isNotEmpty()) clearedRows.average().toFloat() else boardRow.toFloat(),
                centerCol = if (clearedCols.isNotEmpty()) clearedCols.average().toFloat() else boardCol.toFloat()
            )

            if (scoreBreakdown.bannerTitle != null || newCombo >= 2 || totalLinesCleared >= 2) {
                comboBanner = ComboBannerEvent(
                    id = System.currentTimeMillis(),
                    combo = newCombo,
                    linesCleared = totalLinesCleared,
                    title = scoreBreakdown.bannerTitle ?: "${newCombo}x COMBO!",
                    subtitle = scoreBreakdown.multiplierText ?: "+$lineScore PTS",
                    scoreGained = lineScore
                )
            }

            floatingScore = FloatingScoreEvent(
                id = System.currentTimeMillis(),
                text = scoreBreakdown.floatingText,
                normalizedX = if (clearedCols.isNotEmpty()) (clearedCols.average().toFloat() / 7f).coerceIn(0.15f, 0.85f) else 0.5f,
                normalizedY = if (clearedRows.isNotEmpty()) (clearedRows.average().toFloat() / 7f).coerceIn(0.15f, 0.85f) else 0.5f,
                isCombo = newCombo >= 2
            )

            soundManager.playLineClear(totalLinesCleared)
            if (totalLinesCleared >= 2) {
                hapticManager.vibrateMultiLineClear(totalLinesCleared)
                if (newCombo >= 2) {
                    soundManager.playCombo(newCombo)
                }
            } else if (newCombo >= 2) {
                soundManager.playCombo(newCombo)
                hapticManager.vibrateCombo(newCombo)
            } else {
                hapticManager.vibrateLineClear()
            }
        } else {
            finalBoard = placedBoard
        }

        // High Score tracking (recorded for game over screen without disrupting active play)
        var isNewBest = state.isNewBestScore
        var bestScore = state.bestScore
        if (newScore > bestScore) {
            if (bestScore > 0) {
                isNewBest = true
            }
            bestScore = newScore
        }

        // Check if tray is empty -> generate 3 new shapes
        val isTrayEmpty = updatedShapes.all { it == null }
        val finalTray = if (isTrayEmpty) {
            blockGenerator.generateTray(finalBoard, newScore)
        } else {
            updatedShapes
        }

        // Check game-over condition
        val hasMoves = MoveFinder.hasAnyLegalMove(finalBoard, finalTray)

        _gameState.update {
            it.copy(
                board = finalBoard,
                availableShapes = finalTray,
                score = newScore,
                bestScore = bestScore,
                combo = newCombo,
                highestComboInGame = highestCombo,
                linesClearedInGame = newTotalLines,
                blocksPlacedInGame = newBlocksPlaced,
                isNewBestScore = isNewBest,
                isNewBestCelebration = false,
                lastUndoSnapshot = snapshot,
                isGameOver = !hasMoves,
                activeClearEffect = clearEvent,
                activeComboBanner = comboBanner ?: it.activeComboBanner,
                floatingScores = if (floatingScore != null) it.floatingScores + floatingScore else it.floatingScores,
                recentlyPlacedCoords = placedCoords,
                clearingCoords = clearingCoords
            )
        }

        // Auto-dismiss 1-second combo banner over board
        if (comboBanner != null) {
            val bannerId = comboBanner.id
            viewModelScope.launch {
                delay(1000)
                _gameState.update {
                    if (it.activeComboBanner?.id == bannerId) it.copy(activeComboBanner = null) else it
                }
            }
        }

        // Auto-dismiss floating score animation
        if (floatingScore != null) {
            val scoreId = floatingScore.id
            viewModelScope.launch {
                delay(1200)
                _gameState.update { current ->
                    current.copy(floatingScores = current.floatingScores.filterNot { it.id == scoreId })
                }
            }
        }

        // Clear entry animation state after 300ms
        viewModelScope.launch {
            delay(300)
            _gameState.update { it.copy(recentlyPlacedCoords = emptySet()) }
        }

        // Clear exit animation state after 350ms
        if (clearingCoords.isNotEmpty()) {
            viewModelScope.launch {
                delay(350)
                _gameState.update { it.copy(clearingCoords = emptySet()) }
            }
        }

        // Clear effect event auto-dismiss after particle burst
        if (clearEvent != null) {
            val eventId = clearEvent.id
            viewModelScope.launch {
                delay(1050)
                _gameState.update {
                    if (it.activeClearEffect?.id == eventId) it.copy(activeClearEffect = null) else it
                }
            }
        }

        // Check Timed Rush Level Goal
        if (state.mode == GameMode.TIMED) {
            checkTimedLevelGoal(newScore, newTotalLines, highestCombo)
        }

        // Check Daily Challenge progress
        if (state.mode == GameMode.CHALLENGE) {
            checkDailyChallengeGoal(newScore, newTotalLines, highestCombo)
        }

        if (!hasMoves) {
            endGame()
        }
    }

    private fun checkTimedLevelGoal(score: Int, lines: Int, combo: Int) {
        val state = _gameState.value
        val level = state.currentTimedLevel ?: return
        if (!state.isLevelCompleted && !state.isGameOver && level.isGoalMet(score, lines, combo)) {
            timerJob?.cancel()
            soundManager.playReward()
            hapticManager.vibrateHighScore()
            viewModelScope.launch {
                repository.completeTimedLevel(level.rewardCoins)
            }
            _gameState.update {
                it.copy(
                    isLevelCompleted = true,
                    completedLevelReward = level.rewardCoins
                )
            }
        }
    }

    fun startNextTimedLevel() {
        viewModelScope.launch {
            val currentLevelNum = _gameState.value.currentTimedLevel?.levelNumber ?: 1
            val nextLevel = TimedLevelGenerator.getLevel(currentLevelNum + 1)
            val initialBoard = com.example.domain.model.Board.empty()
            val initialTray = blockGenerator.generateTray(initialBoard, 0)

            _gameState.update {
                it.copy(
                    board = initialBoard,
                    availableShapes = initialTray,
                    score = 0,
                    combo = 0,
                    highestComboInGame = 0,
                    linesClearedInGame = 0,
                    blocksPlacedInGame = 0,
                    currentTimedLevel = nextLevel,
                    isLevelCompleted = false,
                    isGameOver = false,
                    isPaused = false,
                    revivesUsed = 0,
                    timeExtensionsUsed = 0,
                    freeUndosRemaining = maxOf(1, it.freeUndosRemaining),
                    timeRemainingSeconds = nextLevel.timeLimitSeconds,
                    isTimeOutGameOver = false,
                    lastUndoSnapshot = null,
                    activeHint = null
                )
            }
            startTimedModeCountdown()
        }
    }

    fun retryCurrentTimedLevel() {
        viewModelScope.launch {
            val currentLevel = _gameState.value.currentTimedLevel ?: TimedLevelGenerator.getLevel(1)
            val initialBoard = com.example.domain.model.Board.empty()
            val initialTray = blockGenerator.generateTray(initialBoard, 0)

            _gameState.update {
                it.copy(
                    board = initialBoard,
                    availableShapes = initialTray,
                    score = 0,
                    combo = 0,
                    highestComboInGame = 0,
                    linesClearedInGame = 0,
                    blocksPlacedInGame = 0,
                    currentTimedLevel = currentLevel,
                    isLevelCompleted = false,
                    isGameOver = false,
                    isPaused = false,
                    revivesUsed = 0,
                    timeExtensionsUsed = 0,
                    freeUndosRemaining = maxOf(1, it.freeUndosRemaining),
                    timeRemainingSeconds = currentLevel.timeLimitSeconds,
                    isTimeOutGameOver = false,
                    lastUndoSnapshot = null,
                    activeHint = null
                )
            }
            startTimedModeCountdown()
        }
    }

    private fun checkDailyChallengeGoal(score: Int, lines: Int, combo: Int) {
        val challenge = dailyChallenge.value ?: return
        val state = _gameState.value
        if (!challenge.isCompleted && !state.isDailyChallengeCompleted && challenge.isGoalMet(score, lines, combo)) {
            soundManager.playReward()
            hapticManager.vibrateHighScore()
            viewModelScope.launch {
                repository.completeDailyChallenge(challenge.rewardCoins)
            }
            _gameState.update {
                it.copy(
                    isDailyChallengeCompleted = true,
                    dailyRewardCoins = challenge.rewardCoins
                )
            }
        }
    }

    fun undo() {
        val state = _gameState.value
        if (state.freeUndosRemaining <= 0 || state.lastUndoSnapshot == null || state.isGameOver) return

        val snapshot = state.lastUndoSnapshot
        _gameState.update {
            it.copy(
                board = snapshot.board,
                availableShapes = snapshot.availableShapes,
                score = snapshot.score,
                combo = snapshot.combo,
                linesClearedInGame = snapshot.linesClearedInGame,
                blocksPlacedInGame = snapshot.blocksPlacedInGame,
                freeUndosRemaining = it.freeUndosRemaining - 1,
                lastUndoSnapshot = null,
                activeHint = null
            )
        }
        soundManager.playClick()
    }

    /**
     * Grants extra undos when the user watches a rewarded ad (+2 undos per rewarded ad).
     */
    fun grantRewardedUndos(count: Int = 2) {
        soundManager.playReward()
        hapticManager.vibrateHighScore()
        _gameState.update {
            it.copy(freeUndosRemaining = it.freeUndosRemaining + count)
        }
    }

    fun useContinue() {
        reviveWithRewardedAd()
    }

    /**
     * Revives the player by watching a rewarded ad.
     * Clears 2 lines on the board to provide room, restores the tray,
     * and increments revivesUsed.
     */
    fun reviveWithRewardedAd() {
        val state = _gameState.value
        if (!state.canTakeReviveWithAd) return

        // Clear 2 lines as specified: "ask him to view the reward ads and clear two line"
        val clearedBoard = MoveFinder.clearTwoLines(state.board)
        val newTray = blockGenerator.generateTray(clearedBoard, state.score)
        val nextRevivesUsed = state.revivesUsed + 1

        _gameState.update {
            it.copy(
                board = clearedBoard,
                availableShapes = newTray,
                isGameOver = false,
                isPaused = false,
                revivesUsed = nextRevivesUsed,
                hasUsedContinue = nextRevivesUsed >= state.maxRevives,
                timeRemainingSeconds = if (it.mode == GameMode.TIMED) maxOf(45, it.timeRemainingSeconds + 35) else 0
            )
        }

        if (state.mode == GameMode.TIMED) {
            startTimedModeCountdown()
        }
        soundManager.playReward()
        hapticManager.vibrateCombo(2)
    }

    /**
     * Extends time by 10 seconds when the player watches a rewarded ad
     * after time ran out in Timed Rush mode.
     */
    fun extendTimeWithRewardedAd(seconds: Int = 10) {
        val state = _gameState.value
        if (!state.canTakeTimeExtensionWithAd) return

        soundManager.playReward()
        hapticManager.vibrateHighScore()

        _gameState.update {
            it.copy(
                isGameOver = false,
                isPaused = false,
                isTimeOutGameOver = false,
                timeRemainingSeconds = it.timeRemainingSeconds + seconds,
                timeExtensionsUsed = it.timeExtensionsUsed + 1
            )
        }

        startTimedModeCountdown()
    }

    private fun endGame(isTimeOut: Boolean = false) {
        val state = _gameState.value
        _gameState.update {
            it.copy(
                isGameOver = true,
                isTimeOutGameOver = isTimeOut
            )
        }
        soundManager.playGameOver()
        hapticManager.vibrateGameOver()

        viewModelScope.launch {
            if (state.mode == GameMode.TIMED) {
                repository.updateBestTimedScore(state.score)
            }
            repository.recordGameFinished(
                score = state.score,
                highestCombo = state.highestComboInGame,
                linesCleared = state.linesClearedInGame,
                blocksPlaced = state.blocksPlacedInGame,
                mode = state.mode
            )

            // Submit high score to Google Play Games Services
            try {
                val app = getApplication<Application>()
                val leaderboardId = if (state.mode == GameMode.TIMED) {
                    app.getString(com.example.R.string.leaderboard_timed_id)
                } else {
                    app.getString(com.example.R.string.leaderboard_classic_id)
                }
                com.example.data.PlayGamesManager.submitScore(leaderboardId, state.score.toLong())
            } catch (e: Exception) {
                android.util.Log.w("GameViewModel", "Play Games submit error: ${e.message}")
            }
        }
    }

    // Shop, Themes & Daily Streak actions
    fun selectTheme(themeId: String) {
        viewModelScope.launch {
            repository.setActiveTheme(themeId)
            soundManager.playClick()
        }
    }

    fun unlockTheme(theme: GameTheme) {
        viewModelScope.launch {
            val success = repository.unlockTheme(theme)
            if (success) {
                soundManager.playReward()
                hapticManager.vibrateHighScore()
            } else {
                soundManager.playInvalid()
            }
        }
    }

    fun claimStreakReward() {
        val streak = dailyStreak.value ?: return
        if (streak.hasClaimedToday) return
        viewModelScope.launch {
            repository.claimStreakReward(streak)
            soundManager.playReward()
            hapticManager.vibrateHighScore()
        }
    }

    fun claimAchievement(achievement: com.example.domain.model.Achievement) {
        viewModelScope.launch {
            repository.claimAchievement(achievement)
            soundManager.playReward()
        }
    }

    fun claimMission(mission: com.example.domain.model.Mission) {
        viewModelScope.launch {
            repository.claimMission(mission)
            soundManager.playReward()
        }
    }

    fun toggleSound() {
        val current = soundManager.isSoundEnabled
        viewModelScope.launch {
            repository.setSoundEnabled(!current)
        }
    }

    fun toggleHaptics() {
        val current = hapticManager.isHapticsEnabled
        viewModelScope.launch {
            repository.setHapticsEnabled(!current)
        }
    }

    fun setThemeMode(mode: AppThemeMode) {
        viewModelScope.launch {
            repository.setThemeMode(mode.id)
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        idleHintJob?.cancel()
    }
}
