package com.example.data.repository

import android.content.Context
import com.example.data.GameDataStore
import com.example.domain.engine.DailyChallengeGenerator
import com.example.domain.model.Achievement
import com.example.domain.model.DailyChallenge
import com.example.domain.model.DailyStreak
import com.example.domain.model.GameMode
import com.example.domain.model.GameStats
import com.example.domain.model.GameTheme
import com.example.domain.model.LeaderboardEntry
import com.example.domain.model.Mission
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GameRepository(context: Context) {

    private val dataStore = GameDataStore(context)

    fun getTodayDateKey(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(Date())
    }

    val statsFlow: Flow<GameStats> = dataStore.statsFlow
    val bestScoreFlow: Flow<Int> = dataStore.bestScoreFlow
    val bestTimedScoreFlow: Flow<Int> = dataStore.bestTimedScoreFlow
    val coinsFlow: Flow<Int> = dataStore.coinsFlow
    val soundEnabledFlow: Flow<Boolean> = dataStore.soundEnabledFlow
    val hapticsEnabledFlow: Flow<Boolean> = dataStore.hapticsEnabledFlow
    val themeModeFlow: Flow<String> = dataStore.themeModeFlow
    val timedLevelFlow: Flow<Int> = dataStore.timedLevelFlow
    val leaderboardFlow: Flow<List<LeaderboardEntry>> = dataStore.leaderboardFlow

    val activeThemeFlow: Flow<GameTheme> = dataStore.activeThemeIdFlow.map { id ->
        GameTheme.findById(id)
    }

    val unlockedThemesFlow: Flow<Set<String>> = dataStore.unlockedThemeIdsFlow

    val dailyStreakFlow: Flow<DailyStreak> = combine(
        dataStore.currentStreakFlow,
        dataStore.lastStreakDateFlow,
        dataStore.statsFlow.map { it.bestStreak }
    ) { current, lastDate, best ->
        val today = getTodayDateKey()
        val hasClaimedToday = lastDate == today
        DailyStreak(
            currentStreak = current,
            bestStreak = best,
            lastClaimDate = lastDate,
            hasClaimedToday = hasClaimedToday
        )
    }

    val dailyChallengeFlow: Flow<DailyChallenge> = dataStore.lastDailyDateFlow.map { lastDailyDate ->
        val today = getTodayDateKey()
        val challenge = DailyChallengeGenerator.generateForDate(today)
        challenge.copy(isCompleted = lastDailyDate == today)
    }

    val achievementsFlow: Flow<List<Achievement>> = combine(
        dataStore.statsFlow,
        dataStore.claimedAchievementsFlow
    ) { stats, claimedIds ->
        val defaults = Achievement.defaultList()
        defaults.map { a ->
            val progress = when (a.id) {
                "score_1000", "score_5000", "score_10000" -> stats.bestScore
                "combo_3", "combo_5", "combo_10" -> stats.highestCombo
                "lines_100", "lines_500" -> stats.totalLinesCleared
                "games_10", "games_50" -> stats.gamesPlayed
                "daily_1", "daily_7" -> stats.dailyChallengesCompleted
                "streak_7" -> stats.currentStreak
                "theme_1" -> (stats.unlockedThemeIds.size - 1).coerceAtLeast(0)
                else -> 0
            }
            a.copy(
                currentProgress = progress,
                isClaimed = claimedIds.contains(a.id)
            )
        }
    }

    val missionsFlow: Flow<List<Mission>> = combine(
        dataStore.statsFlow,
        dataStore.claimedMissionsFlow
    ) { stats, claimedIds ->
        val list = Mission.dailyMissions()
        list.map { m ->
            val progress = when (m.id) {
                "m_lines_20" -> stats.totalLinesCleared.coerceAtMost(20)
                "m_combo_5" -> stats.highestCombo.coerceAtMost(4)
                "m_score_3000" -> stats.bestScore.coerceAtMost(3000)
                "m_blocks_50" -> stats.totalBlocksPlaced.coerceAtMost(50)
                "m_multi_line" -> (stats.totalLinesCleared / 2).coerceAtMost(3)
                "m_games_3" -> stats.gamesPlayed.coerceAtMost(3)
                else -> 0
            }
            m.copy(
                currentProgress = progress,
                isClaimed = claimedIds.contains(m.id)
            )
        }
    }

    suspend fun recordGameFinished(
        score: Int,
        highestCombo: Int,
        linesCleared: Int,
        blocksPlaced: Int,
        mode: GameMode = GameMode.CLASSIC
    ) {
        dataStore.recordGameFinished(score, highestCombo, linesCleared, blocksPlaced, mode)
    }

    suspend fun updateBestTimedScore(score: Int) {
        dataStore.updateBestTimedScore(score)
    }

    suspend fun claimAchievement(achievement: Achievement) {
        dataStore.claimAchievement(achievement.id, achievement.rewardCoins)
    }

    suspend fun claimMission(mission: Mission) {
        dataStore.claimMission(mission.id, mission.rewardCoins)
    }

    suspend fun claimStreakReward(currentStreak: DailyStreak) {
        val today = getTodayDateKey()
        if (currentStreak.hasClaimedToday) return
        val newStreak = currentStreak.currentStreak + 1
        val reward = currentStreak.getRewardForDay(newStreak)
        dataStore.claimStreakReward(today, newStreak, reward)
    }

    suspend fun completeDailyChallenge(rewardCoins: Int) {
        val today = getTodayDateKey()
        dataStore.markDailyChallengeCompleted(today, rewardCoins)
    }

    suspend fun completeTimedLevel(rewardCoins: Int) {
        dataStore.advanceTimedLevel(rewardCoins)
    }

    suspend fun setTimedLevel(level: Int) {
        dataStore.setTimedLevel(level)
    }

    suspend fun unlockTheme(theme: GameTheme): Boolean {
        val spent = dataStore.spendCoins(theme.price)
        if (spent) {
            dataStore.unlockTheme(theme.id)
            dataStore.setActiveTheme(theme.id)
            return true
        }
        return false
    }

    suspend fun setActiveTheme(themeId: String) {
        dataStore.setActiveTheme(themeId)
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        dataStore.setSoundEnabled(enabled)
    }

    suspend fun setHapticsEnabled(enabled: Boolean) {
        dataStore.setHapticsEnabled(enabled)
    }

    suspend fun setThemeMode(mode: String) {
        dataStore.setThemeMode(mode)
    }

    suspend fun addCoins(amount: Int) {
        dataStore.addCoins(amount)
    }

    suspend fun resetDailyMissionsIfNewDay(todayDateKey: String) {
        dataStore.resetDailyMissionsIfNewDay(todayDateKey)
    }
}
