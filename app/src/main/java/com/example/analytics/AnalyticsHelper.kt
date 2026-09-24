package com.example.analytics

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics

/**
 * Reusable helper for logging Firebase Analytics events and linking with AdMob/Google Ads.
 */
object AnalyticsHelper {
    private const val TAG = "AnalyticsHelper"
    private var firebaseAnalytics: FirebaseAnalytics? = null

    fun initialize(context: Context) {
        try {
            firebaseAnalytics = FirebaseAnalytics.getInstance(context)
            Log.i(TAG, "Firebase Analytics initialized successfully.")
        } catch (e: Throwable) {
            Log.w(TAG, "Firebase Analytics initialization error: ${e.message}")
        }
    }

    fun logEvent(eventName: String, params: (Bundle.() -> Unit)? = null) {
        try {
            val bundle = params?.let { Bundle().apply(it) }
            firebaseAnalytics?.logEvent(eventName, bundle)
            Log.d(TAG, "Logged Analytics event '$eventName'")
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to log event '$eventName': ${e.message}")
        }
    }

    fun logGameStarted(mode: String) {
        logEvent("game_started") {
            putString("game_mode", mode)
        }
    }

    fun logGameOver(mode: String, score: Long, linesCleared: Int) {
        logEvent("game_over") {
            putString("game_mode", mode)
            putLong("final_score", score)
            putInt("lines_cleared", linesCleared)
        }
    }

    fun logAdImpression(adFormat: String, adUnitId: String, adapterClassName: String) {
        logEvent("custom_ad_impression") {
            putString("ad_format", adFormat)
            putString("ad_unit_id", adUnitId)
            putString("adapter_class", adapterClassName)
        }
    }

    fun logRewardedEarned(rewardType: String, amount: Int) {
        logEvent("rewarded_ad_earned") {
            putString("reward_type", rewardType)
            putInt("reward_amount", amount)
        }
    }

    fun setUserId(userId: String) {
        try {
            firebaseAnalytics?.setUserId(userId)
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to set user ID: ${e.message}")
        }
    }
}
