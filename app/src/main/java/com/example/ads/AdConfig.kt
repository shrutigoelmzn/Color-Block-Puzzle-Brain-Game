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
    // TEST ADS TOGGLE:
    // Set to false to use your REAL AdMob IDs (ca-app-pub-3628219835925816/...) and Mediation.
    // Set to true to force Google's sample test ads.
    // -----------------------------------------------------------------------------------------
    const val FORCE_TEST_ADS = false

    // -----------------------------------------------------------------------------------------
    // OFFICIAL GOOGLE ADMOB TEST IDs
    // -----------------------------------------------------------------------------------------
    const val TEST_APP_ID = "ca-app-pub-3940256099942544~3347511713"
    const val TEST_BANNER_AD_ID = "ca-app-pub-3940256099942544/6300978111"
    const val TEST_REWARDED_AD_ID = "ca-app-pub-3940256099942544/5224354917"
    const val TEST_INTERSTITIAL_AD_ID = "ca-app-pub-3940256099942544/1033173712"

    // -----------------------------------------------------------------------------------------
    // PRODUCTION / RELEASE AdMob IDs (REPLACE THESE WITH YOUR REAL ADMOB IDs FOR GOOGLE PLAY)
    // -----------------------------------------------------------------------------------------
    // 1. Your Real AdMob Application ID (Also update in res/values/strings.xml -> admob_app_id)
    const val RELEASE_APP_ID = "ca-app-pub-3628219835925816~9258686107"

    // 2. Your Real Rewarded Ad Unit ID (shown when player needs a revive/life)
    const val RELEASE_REWARDED_AD_ID = "ca-app-pub-3628219835925816/9457996650"

    // 3. Your Real Interstitial Ad Unit ID (shown when player completes level & clicks next)
    const val RELEASE_INTERSTITIAL_AD_ID = "ca-app-pub-3628219835925816/9254816677"

    // 4. Your Real Banner Ad Unit ID (shown in main menu & game screen)
    const val RELEASE_BANNER_AD_ID = ""

    /**
     * Determines whether the app is running in debug mode.
     */
    val isDebugMode: Boolean = BuildConfig.DEBUG

    /**
     * Returns the active Rewarded Ad Unit ID.
     * Uses official test ID only if FORCE_TEST_ADS is true or release ID is not set.
     */
    val rewardedAdUnitId: String
        get() {
            return if (FORCE_TEST_ADS || isPlaceholder(RELEASE_REWARDED_AD_ID)) {
                TEST_REWARDED_AD_ID
            } else {
                RELEASE_REWARDED_AD_ID
            }
        }

    /**
     * Returns the active Interstitial Ad Unit ID.
     * Uses official test ID only if FORCE_TEST_ADS is true or release ID is not set.
     */
    val interstitialAdUnitId: String
        get() {
            return if (FORCE_TEST_ADS || isPlaceholder(RELEASE_INTERSTITIAL_AD_ID)) {
                TEST_INTERSTITIAL_AD_ID
            } else {
                RELEASE_INTERSTITIAL_AD_ID
            }
        }

    /**
     * Returns the active Banner Ad Unit ID.
     * Uses official test ID only if FORCE_TEST_ADS is true or release ID is not set.
     */
    val bannerAdUnitId: String
        get() {
            return if (FORCE_TEST_ADS || isPlaceholder(RELEASE_BANNER_AD_ID)) {
                TEST_BANNER_AD_ID
            } else {
                RELEASE_BANNER_AD_ID
            }
        }

    private fun isPlaceholder(id: String): Boolean {
        return id.isBlank() || id.contains("xxxx", ignoreCase = true)
    }
}
