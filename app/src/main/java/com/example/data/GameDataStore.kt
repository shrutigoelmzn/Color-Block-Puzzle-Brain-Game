package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.model.GameMode
import com.example.domain.model.GameStats
import com.example.domain.model.LeaderboardEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

val Context.gameDataStore: DataStore<Preferences> by preferencesDataStore(name = "color_block_puzzle_prefs")

class GameDataStore(private val context: Context) {

    private val dataStore = context.gameDataStore

    companion object {
        val KEY_BEST_SCORE = intPreferencesKey("best_score")
        val KEY_BEST_TIMED_SCORE = intPreferencesKey("best_timed_score")
        val KEY_COINS = intPreferencesKey("coins")
        val KEY_SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val KEY_HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
        val KEY_ACTIVE_THEME = stringPreferencesKey("active_theme")
        val KEY_UNLOCKED_THEMES = stringSetPreferencesKey("unlocked_themes")
        val KEY_HIGHEST_COMBO = intPreferencesKey("highest_combo")
        val KEY_TOTAL_SCORE = longPreferencesKey("total_score")
        val KEY_TOTAL_LINES = intPreferencesKey("total_lines")
        val KEY_TOTAL_BLOCKS = intPreferencesKey("total_blocks")
        val KEY_GAMES_PLAYED = intPreferencesKey("games_played")
        val KEY_DAILY_COMPLETED_COUNT = intPreferencesKey("daily_completed_count")
        val KEY_CURRENT_STREAK = intPreferencesKey("current_streak")
        val KEY_BEST_STREAK = intPreferencesKey("best_streak")
        val KEY_LAST_STREAK_DATE = stringPreferencesKey("last_streak_date")
        val KEY_LAST_DAILY_DATE = stringPreferencesKey("last_daily_date")
        val KEY_CLAIMED_ACHIEVEMENTS = stringSetPreferencesKey("claimed_achievements")
        val KEY_CLAIMED_MISSIONS = stringSetPreferencesKey("claimed_missions")
        val KEY_MISSIONS_DATE = stringPreferencesKey("missions_date")
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        val KEY_TIMED_LEVEL = intPreferencesKey("timed_level")
        val KEY_LEADERBOARD_JSON = stringPreferencesKey("leaderboard_json")
    }

    val timedLevelFlow: Flow<Int> = dataStore.data.map { it[KEY_TIMED_LEVEL] ?: 1 }
    val themeModeFlow: Flow<String> = dataStore.data.map { it[KEY_THEME_MODE] ?: "system" }
    val bestScoreFlow: Flow<Int> = dataStore.data.map { it[KEY_BEST_SCORE] ?: 0 }
    val bestTimedScoreFlow: Flow<Int> = dataStore.data.map { it[KEY_BEST_TIMED_SCORE] ?: 0 }
    val coinsFlow: Flow<Int> = dataStore.data.map { it[KEY_COINS] ?: 100 }
    val soundEnabledFlow: Flow<Boolean> = dataStore.data.map { it[KEY_SOUND_ENABLED] ?: true }
    val hapticsEnabledFlow: Flow<Boolean> = dataStore.data.map { it[KEY_HAPTICS_ENABLED] ?: true }
    val activeThemeIdFlow: Flow<String> = dataStore.data.map { it[KEY_ACTIVE_THEME] ?: "classic" }
    val unlockedThemeIdsFlow: Flow<Set<String>> = dataStore.data.map {
        it[KEY_UNLOCKED_THEMES] ?: setOf("classic")
    }
    val currentStreakFlow: Flow<Int> = dataStore.data.map { it[KEY_CURRENT_STREAK] ?: 0 }
    val lastStreakDateFlow: Flow<String> = dataStore.data.map { it[KEY_LAST_STREAK_DATE] ?: "" }
    val lastDailyDateFlow: Flow<String> = dataStore.data.map { it[KEY_LAST_DAILY_DATE] ?: "" }
    val claimedAchievementsFlow: Flow<Set<String>> = dataStore.data.map {
        it[KEY_CLAIMED_ACHIEVEMENTS] ?: emptySet()
    }
    val claimedMissionsFlow: Flow<Set<String>> = dataStore.data.map {
        it[KEY_CLAIMED_MISSIONS] ?: emptySet()
    }
    val missionsDateFlow: Flow<String> = dataStore.data.map { it[KEY_MISSIONS_DATE] ?: "" }

    val leaderboardFlow: Flow<List<LeaderboardEntry>> = dataStore.data.map { prefs ->
        val jsonStr = prefs[KEY_LEADERBOARD_JSON]
        if (jsonStr.isNullOrEmpty()) {
            LeaderboardEntry.defaultGlobalLeaderboard()
        } else {
            try {
                deserializeLeaderboard(jsonStr)
            } catch (e: Exception) {
                LeaderboardEntry.defaultGlobalLeaderboard()
            }
        }
    }

    val statsFlow: Flow<GameStats> = dataStore.data.map { prefs ->
        GameStats(
            bestScore = prefs[KEY_BEST_SCORE] ?: 0,
            bestTimedScore = prefs[KEY_BEST_TIMED_SCORE] ?: 0,
            highestCombo = prefs[KEY_HIGHEST_COMBO] ?: 0,
            totalScore = prefs[KEY_TOTAL_SCORE] ?: 0L,
            totalLinesCleared = prefs[KEY_TOTAL_LINES] ?: 0,
            totalBlocksPlaced = prefs[KEY_TOTAL_BLOCKS] ?: 0,
            gamesPlayed = prefs[KEY_GAMES_PLAYED] ?: 0,
            dailyChallengesCompleted = prefs[KEY_DAILY_COMPLETED_COUNT] ?: 0,
            currentStreak = prefs[KEY_CURRENT_STREAK] ?: 0,
            bestStreak = prefs[KEY_BEST_STREAK] ?: 0,
            coins = prefs[KEY_COINS] ?: 100,
            activeThemeId = prefs[KEY_ACTIVE_THEME] ?: "classic",
            unlockedThemeIds = prefs[KEY_UNLOCKED_THEMES] ?: setOf("classic")
        )
    }

    suspend fun updateBestScore(score: Int) {
        dataStore.edit { prefs ->
            val current = prefs[KEY_BEST_SCORE] ?: 0
            if (score > current) {
                prefs[KEY_BEST_SCORE] = score
            }
        }
    }

    suspend fun updateBestTimedScore(score: Int) {
        dataStore.edit { prefs ->
            val current = prefs[KEY_BEST_TIMED_SCORE] ?: 0
            if (score > current) {
                prefs[KEY_BEST_TIMED_SCORE] = score
            }
        }
    }

    suspend fun addCoins(amount: Int) {
        dataStore.edit { prefs ->
            val current = prefs[KEY_COINS] ?: 100
            prefs[KEY_COINS] = current + amount
        }
    }

    suspend fun spendCoins(amount: Int): Boolean {
        var success = false
        dataStore.edit { prefs ->
            val current = prefs[KEY_COINS] ?: 100
            if (current >= amount) {
                prefs[KEY_COINS] = current - amount
                success = true
            }
        }
        return success
    }

    suspend fun setActiveTheme(themeId: String) {
        dataStore.edit { prefs ->
            prefs[KEY_ACTIVE_THEME] = themeId
        }
    }

    suspend fun unlockTheme(themeId: String) {
        dataStore.edit { prefs ->
            val current = (prefs[KEY_UNLOCKED_THEMES] ?: setOf("classic")).toMutableSet()
            current.add(themeId)
            prefs[KEY_UNLOCKED_THEMES] = current
        }
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_SOUND_ENABLED] = enabled
        }
    }

    suspend fun setHapticsEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_HAPTICS_ENABLED] = enabled
        }
    }

    suspend fun recordGameFinished(
        score: Int,
        highestCombo: Int,
        linesCleared: Int,
        blocksPlaced: Int,
        mode: GameMode = GameMode.CLASSIC
    ) {
        dataStore.edit { prefs ->
            val currentBest = prefs[KEY_BEST_SCORE] ?: 0
            if (score > currentBest) {
                prefs[KEY_BEST_SCORE] = score
            }

            val currentHighestCombo = prefs[KEY_HIGHEST_COMBO] ?: 0
            if (highestCombo > currentHighestCombo) {
                prefs[KEY_HIGHEST_COMBO] = highestCombo
            }

            prefs[KEY_TOTAL_SCORE] = (prefs[KEY_TOTAL_SCORE] ?: 0L) + score
            prefs[KEY_TOTAL_LINES] = (prefs[KEY_TOTAL_LINES] ?: 0) + linesCleared
            prefs[KEY_TOTAL_BLOCKS] = (prefs[KEY_TOTAL_BLOCKS] ?: 0) + blocksPlaced
            prefs[KEY_GAMES_PLAYED] = (prefs[KEY_GAMES_PLAYED] ?: 0) + 1

            // Award coins based on performance (e.g. 1 coin per 100 points + 5 per line)
            val earnedCoins = (score / 100) + (linesCleared * 2)
            if (earnedCoins > 0) {
                prefs[KEY_COINS] = (prefs[KEY_COINS] ?: 100) + earnedCoins
            }

            // Update Leaderboard if score > 0
            if (score > 0) {
                val currentList = try {
                    val existingJson = prefs[KEY_LEADERBOARD_JSON]
                    if (existingJson.isNullOrEmpty()) LeaderboardEntry.defaultGlobalLeaderboard()
                    else deserializeLeaderboard(existingJson)
                } catch (e: Exception) {
                    LeaderboardEntry.defaultGlobalLeaderboard()
                }

                val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                val newEntry = LeaderboardEntry(
                    id = "user_${System.currentTimeMillis()}",
                    rank = 0,
                    playerName = "Player (You)",
                    score = score,
                    mode = mode,
                    linesCleared = linesCleared,
                    highestCombo = highestCombo,
                    dateString = today,
                    isUser = true,
                    isPersonalBest = false
                )

                val merged = (currentList + newEntry).sortedByDescending { it.score }
                val userBestScore = merged.filter { it.isUser }.maxOfOrNull { it.score } ?: score

                var assignedPersonalBest = false
                val updatedList = merged.take(25).mapIndexed { index, entry ->
                    val isPB = entry.isUser && entry.score == userBestScore && !assignedPersonalBest
                    if (isPB) assignedPersonalBest = true
                    entry.copy(
                        rank = index + 1,
                        isPersonalBest = isPB
                    )
                }

                prefs[KEY_LEADERBOARD_JSON] = serializeLeaderboard(updatedList)
            }
        }
    }

    suspend fun markDailyChallengeCompleted(dateKey: String, rewardCoins: Int) {
        dataStore.edit { prefs ->
            val lastDate = prefs[KEY_LAST_DAILY_DATE] ?: ""
            if (lastDate != dateKey) {
                prefs[KEY_LAST_DAILY_DATE] = dateKey
                prefs[KEY_DAILY_COMPLETED_COUNT] = (prefs[KEY_DAILY_COMPLETED_COUNT] ?: 0) + 1
                prefs[KEY_COINS] = (prefs[KEY_COINS] ?: 100) + rewardCoins
            }
        }
    }

    suspend fun advanceTimedLevel(rewardCoins: Int) {
        dataStore.edit { prefs ->
            val current = prefs[KEY_TIMED_LEVEL] ?: 1
            prefs[KEY_TIMED_LEVEL] = current + 1
            prefs[KEY_COINS] = (prefs[KEY_COINS] ?: 100) + rewardCoins
        }
    }

    suspend fun setTimedLevel(level: Int) {
        dataStore.edit { prefs ->
            prefs[KEY_TIMED_LEVEL] = maxOf(1, level)
        }
    }

    suspend fun claimStreakReward(todayDateKey: String, newStreakCount: Int, rewardCoins: Int) {
        dataStore.edit { prefs ->
            prefs[KEY_LAST_STREAK_DATE] = todayDateKey
            prefs[KEY_CURRENT_STREAK] = newStreakCount
            val best = prefs[KEY_BEST_STREAK] ?: 0
            if (newStreakCount > best) {
                prefs[KEY_BEST_STREAK] = newStreakCount
            }
            prefs[KEY_COINS] = (prefs[KEY_COINS] ?: 100) + rewardCoins
        }
    }

    suspend fun claimAchievement(achievementId: String, rewardCoins: Int) {
        dataStore.edit { prefs ->
            val current = (prefs[KEY_CLAIMED_ACHIEVEMENTS] ?: emptySet()).toMutableSet()
            if (!current.contains(achievementId)) {
                current.add(achievementId)
                prefs[KEY_CLAIMED_ACHIEVEMENTS] = current
                prefs[KEY_COINS] = (prefs[KEY_COINS] ?: 100) + rewardCoins
            }
        }
    }

    suspend fun claimMission(missionId: String, rewardCoins: Int) {
        dataStore.edit { prefs ->
            val current = (prefs[KEY_CLAIMED_MISSIONS] ?: emptySet()).toMutableSet()
            if (!current.contains(missionId)) {
                current.add(missionId)
                prefs[KEY_CLAIMED_MISSIONS] = current
                prefs[KEY_COINS] = (prefs[KEY_COINS] ?: 100) + rewardCoins
            }
        }
    }

    suspend fun resetDailyMissionsIfNewDay(todayDateKey: String) {
        dataStore.edit { prefs ->
            val lastDate = prefs[KEY_MISSIONS_DATE] ?: ""
            if (lastDate != todayDateKey) {
                prefs[KEY_MISSIONS_DATE] = todayDateKey
                prefs[KEY_CLAIMED_MISSIONS] = emptySet()
            }
        }
    }

    suspend fun setThemeMode(mode: String) {
        dataStore.edit { prefs ->
            prefs[KEY_THEME_MODE] = mode
        }
    }

    private fun serializeLeaderboard(entries: List<LeaderboardEntry>): String {
        val array = JSONArray()
        for (e in entries) {
            val obj = JSONObject().apply {
                put("id", e.id)
                put("rank", e.rank)
                put("playerName", e.playerName)
                put("score", e.score)
                put("mode", e.mode.name)
                put("linesCleared", e.linesCleared)
                put("highestCombo", e.highestCombo)
                put("dateString", e.dateString)
                put("isUser", e.isUser)
                put("isPersonalBest", e.isPersonalBest)
            }
            array.put(obj)
        }
        return array.toString()
    }

    private fun deserializeLeaderboard(jsonStr: String): List<LeaderboardEntry> {
        val array = JSONArray(jsonStr)
        val list = mutableListOf<LeaderboardEntry>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val modeName = obj.optString("mode", GameMode.CLASSIC.name)
            val mode = try {
                GameMode.valueOf(modeName)
            } catch (e: Exception) {
                GameMode.CLASSIC
            }
            list.add(
                LeaderboardEntry(
                    id = obj.optString("id", "entry_$i"),
                    rank = obj.optInt("rank", i + 1),
                    playerName = obj.optString("playerName", "Player"),
                    score = obj.optInt("score", 0),
                    mode = mode,
                    linesCleared = obj.optInt("linesCleared", 0),
                    highestCombo = obj.optInt("highestCombo", 0),
                    dateString = obj.optString("dateString", ""),
                    isUser = obj.optBoolean("isUser", false),
                    isPersonalBest = obj.optBoolean("isPersonalBest", false)
                )
            )
        }
        return list
    }
}
