package com.example.data

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.games.PlayGames
import com.google.android.gms.games.PlayGamesSdk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.lang.ref.WeakReference

object PlayGamesManager {
    private const val TAG = "PlayGamesManager"
    const val RC_LEADERBOARD = 9004

    private var currentActivity: WeakReference<Activity>? = null

    private val _isSignedIn = MutableStateFlow(false)
    val isSignedIn: StateFlow<Boolean> = _isSignedIn.asStateFlow()

    private val _currentGamerTag = MutableStateFlow<String?>(null)
    val currentGamerTag: StateFlow<String?> = _currentGamerTag.asStateFlow()

    fun setActivity(activity: Activity?) {
        currentActivity = if (activity != null) WeakReference(activity) else null
    }

    /**
     * Checks if Google Play Services is available and operational on this device.
     */
    fun isGooglePlayServicesAvailable(context: Context): Boolean {
        return try {
            val availability = GoogleApiAvailability.getInstance()
            val result = availability.isGooglePlayServicesAvailable(context)
            result == ConnectionResult.SUCCESS
        } catch (e: Throwable) {
            Log.w(TAG, "isGooglePlayServicesAvailable check failed: ${e.message}")
            false
        }
    }

    /**
     * Initializes Google Play Games SDK v2.
     * Skips initialization safely if Google Play Services is unavailable.
     */
    fun initialize(context: Context) {
        if (!isGooglePlayServicesAvailable(context)) {
            Log.d(TAG, "Google Play Services not available; skipping PlayGamesSdk.initialize")
            return
        }
        try {
            PlayGamesSdk.initialize(context)
            Log.d(TAG, "PlayGamesSdk initialized successfully")
        } catch (e: Throwable) {
            Log.d(TAG, "PlayGamesSdk init skipped or failed: ${e.message}")
        }
    }

    /**
     * Checks authentication state silently without forcing a sign-in loop.
     */
    fun checkAuthentication(activity: Activity, onComplete: ((Boolean) -> Unit)? = null) {
        setActivity(activity)
        if (!isGooglePlayServicesAvailable(activity)) {
            Log.d(TAG, "Google Play Services not available; skipping checkAuthentication")
            _isSignedIn.value = false
            onComplete?.invoke(false)
            return
        }

        try {
            val gamesSignInClient = PlayGames.getGamesSignInClient(activity)
            gamesSignInClient.isAuthenticated.addOnCompleteListener { isAuthenticatedTask ->
                val isAuthenticated = isAuthenticatedTask.isSuccessful &&
                        (isAuthenticatedTask.result?.isAuthenticated == true)
                _isSignedIn.value = isAuthenticated

                if (isAuthenticated) {
                    fetchPlayerProfile(activity)
                }
                onComplete?.invoke(isAuthenticated)
            }.addOnFailureListener { e ->
                Log.d(TAG, "Play Games auth check unavailable: ${e.message}")
                _isSignedIn.value = false
                onComplete?.invoke(false)
            }
        } catch (e: Throwable) {
            Log.d(TAG, "Play Games authentication error: ${e.message}")
            _isSignedIn.value = false
            onComplete?.invoke(false)
        }
    }

    /**
     * Explicit sign-in triggered by user interaction.
     */
    fun signInExplicitly(activity: Activity, onComplete: ((Boolean) -> Unit)? = null) {
        setActivity(activity)
        if (!isGooglePlayServicesAvailable(activity)) {
            onComplete?.invoke(false)
            return
        }

        try {
            val gamesSignInClient = PlayGames.getGamesSignInClient(activity)
            gamesSignInClient.signIn().addOnCompleteListener { signInTask ->
                val success = signInTask.isSuccessful && (signInTask.result?.isAuthenticated == true)
                _isSignedIn.value = success
                if (success) {
                    fetchPlayerProfile(activity)
                }
                onComplete?.invoke(success)
            }.addOnFailureListener { e ->
                Log.w(TAG, "Explicit signIn failed: ${e.message}")
                _isSignedIn.value = false
                onComplete?.invoke(false)
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Explicit signIn error: ${e.message}")
            _isSignedIn.value = false
            onComplete?.invoke(false)
        }
    }

    private fun fetchPlayerProfile(activity: Activity) {
        if (!isGooglePlayServicesAvailable(activity)) return
        try {
            PlayGames.getPlayersClient(activity).currentPlayer.addOnSuccessListener { player ->
                _currentGamerTag.value = player?.displayName
                Log.d(TAG, "Play Games signed in as: ${player?.displayName}")
            }.addOnFailureListener { e ->
                Log.w(TAG, "Failed to load player profile: ${e.message}")
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Player profile fetch error: ${e.message}")
        }
    }

    /**
     * Submits high score to Google Play Games leaderboard using the active Activity.
     * Safely checks for Google Play Services availability and authentication state first.
     */
    fun submitScore(leaderboardId: String, score: Long) {
        val activity = currentActivity?.get()
        if (activity != null) {
            submitScore(activity, leaderboardId, score)
        }
    }

    /**
     * Submits high score to Google Play Games leaderboard.
     */
    fun submitScore(activity: Activity, leaderboardId: String, score: Long) {
        if (!isGooglePlayServicesAvailable(activity) ||
            !_isSignedIn.value ||
            isPlaceholderId(leaderboardId) ||
            score <= 0
        ) {
            return
        }

        try {
            val leaderboardsClient = PlayGames.getLeaderboardsClient(activity)
            leaderboardsClient.submitScore(leaderboardId, score)
            Log.d(TAG, "Submitted score $score to Play Games leaderboard $leaderboardId")
        } catch (e: Throwable) {
            Log.w(TAG, "Error submitting score to Play Games: ${e.message}")
        }
    }

    /**
     * Launches Google Play Games native Leaderboard UI.
     */
    fun showLeaderboard(
        activity: Activity,
        leaderboardId: String,
        onNotConfigured: () -> Unit
    ) {
        if (!isGooglePlayServicesAvailable(activity) || isPlaceholderId(leaderboardId)) {
            onNotConfigured()
            return
        }

        try {
            val leaderboardsClient = PlayGames.getLeaderboardsClient(activity)
            leaderboardsClient.getLeaderboardIntent(leaderboardId)
                .addOnSuccessListener { intent ->
                    try {
                        @Suppress("DEPRECATION")
                        activity.startActivityForResult(intent, RC_LEADERBOARD)
                    } catch (e: Throwable) {
                        Log.w(TAG, "Failed to start leaderboard intent: ${e.message}")
                        onNotConfigured()
                    }
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Failed to get leaderboard intent: ${e.message}")
                    onNotConfigured()
                }
        } catch (e: Throwable) {
            Log.w(TAG, "Error showing leaderboard: ${e.message}")
            onNotConfigured()
        }
    }

    fun isPlaceholderId(id: String): Boolean {
        return id.isBlank() ||
                id.contains("xxxx", ignoreCase = true) ||
                id.all { it == '0' } ||
                id == "000000000000"
    }
}
