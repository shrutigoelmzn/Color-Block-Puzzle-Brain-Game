package com.example.ads

import com.example.BuildConfig

/**
 * AdMob Configuration for Debug (Test) and Release (Production) environments.
 *
 * DEBUG: Automatically uses Google's official AdMob test ad unit IDs so you can test safely
 * without risking policy violations or account bans.
 *
 * RELEASE: Update the constants below with your real AdMob App ID and Ad Unit IDs when
 * publishing to Google Play.
 */
object AdConfig {
    // -----------------------------------------------------------------------------------------
    // OFFICIAL GOOGLE ADMOB TEST IDs (Used in DEBUG builds and whenever Release IDs are unset)
    // -----------------------------------------------------------------------------------------
    const val TEST_APP_ID = "ca-app-pub-3940256099942544~3347511713"
    const val TEST_REWARDED_AD_ID = "ca-app-pub-3940256099942544/5224354917"
    const val TEST_INTERSTITIAL_AD_ID = "ca-app-pub-3940256099942544/1033173712"

    // -----------------------------------------------------------------------------------------
    // PRODUCTION / RELEASE AdMob IDs (REPLACE THESE WITH YOUR REAL ADMOB IDs FOR GOOGLE PLAY)
    // -----------------------------------------------------------------------------------------
    // 1. Your Real AdMob Application ID (Also update in res/values/strings.xml -> admob_app_id)
    const val RELEASE_APP_ID = "ca-app-pub-xxxxxxxxxxxxxxxx~yyyyyyyyyy"

    // 2. Your Real Rewarded Ad Unit ID (shown when player needs a revive/life)
    const val RELEASE_REWARDED_AD_ID = "ca-app-pub-xxxxxxxxxxxxxxxx/yyyyyyyyyy"

    // 3. Your Real Interstitial Ad Unit ID (shown when player completes level & clicks next)
    const val RELEASE_INTERSTITIAL_AD_ID = "ca-app-pub-xxxxxxxxxxxxxxxx/yyyyyyyyyy"

    /**
     * Determines whether the app is running in debug mode.
     */
    val isDebugMode: Boolean = BuildConfig.DEBUG

    /**
     * Returns the active Rewarded Ad Unit ID.
     * Uses official test ID in debug builds or when release ID is not yet configured.
     */
    val rewardedAdUnitId: String
        get() {
            return if (isDebugMode || isPlaceholder(RELEASE_REWARDED_AD_ID)) {
                TEST_REWARDED_AD_ID
            } else {
                RELEASE_REWARDED_AD_ID
            }
        }

    /**
     * Returns the active Interstitial Ad Unit ID.
     * Uses official test ID in debug builds or when release ID is not yet configured.
     */
    val interstitialAdUnitId: String
        get() {
            return if (isDebugMode || isPlaceholder(RELEASE_INTERSTITIAL_AD_ID)) {
                TEST_INTERSTITIAL_AD_ID
            } else {
                RELEASE_INTERSTITIAL_AD_ID
            }
        }

    private fun isPlaceholder(id: String): Boolean {
        return id.isBlank() || id.contains("xxxx", ignoreCase = true)
    }
}
