package com.example.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean

object AdManager {
    private const val TAG = "AdManager"

    private var rewardedAd: RewardedAd? = null
    private var isRewardedAdLoading = AtomicBoolean(false)

    private var interstitialAd: InterstitialAd? = null
    private var isInterstitialAdLoading = AtomicBoolean(false)

    private val _isRewardedAdReady = MutableStateFlow(false)
    val isRewardedAdReady: StateFlow<Boolean> = _isRewardedAdReady.asStateFlow()

    private val isInitialized = AtomicBoolean(false)

    /**
     * Initializes Google Mobile Ads SDK on app startup.
     */
    fun initialize(context: Context) {
        if (isInitialized.getAndSet(true)) return

        try {
            MobileAds.initialize(context) { initializationStatus ->
                Log.d(TAG, "AdMob MobileAds initialized: $initializationStatus")
                preloadAds(context)
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to initialize AdMob: ${e.message}")
        }
    }

    /**
     * Preloads both rewarded and interstitial ads to have them ready.
     */
    fun preloadAds(context: Context) {
        loadRewardedAd(context)
        loadInterstitialAd(context)
    }

    /**
     * Loads a Rewarded Ad.
     */
    fun loadRewardedAd(context: Context) {
        if (rewardedAd != null || isRewardedAdLoading.get()) return

        isRewardedAdLoading.set(true)
        val adRequest = AdRequest.Builder().build()
        val adUnitId = AdConfig.rewardedAdUnitId

        try {
            RewardedAd.load(
                context,
                adUnitId,
                adRequest,
                object : RewardedAdLoadCallback() {
                    override fun onAdLoaded(ad: RewardedAd) {
                        rewardedAd = ad
                        isRewardedAdLoading.set(false)
                        _isRewardedAdReady.value = true
                        Log.d(TAG, "RewardedAd successfully loaded")
                    }

                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        rewardedAd = null
                        isRewardedAdLoading.set(false)
                        _isRewardedAdReady.value = false
                        Log.w(TAG, "RewardedAd failed to load: ${loadAdError.message} (code ${loadAdError.code})")
                    }
                }
            )
        } catch (e: Throwable) {
            Log.w(TAG, "Exception during RewardedAd.load: ${e.message}")
            isRewardedAdLoading.set(false)
        }
    }

    /**
     * Displays a Rewarded Ad for player revives.
     *
     * @param activity The calling activity
     * @param onUserEarnedReward Invoked when the user earns the reward
     * @param onAdDismissed Invoked when the ad finishes displaying or fails
     */
    fun showRewardedAd(
        activity: Activity,
        onUserEarnedReward: () -> Unit,
        onAdDismissed: () -> Unit = {}
    ) {
        val currentAd = rewardedAd
        if (currentAd != null) {
            var rewardEarned = false

            currentAd.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "RewardedAd dismissed")
                    rewardedAd = null
                    _isRewardedAdReady.value = false
                    loadRewardedAd(activity)
                    onAdDismissed()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.w(TAG, "RewardedAd failed to show: ${adError.message}")
                    rewardedAd = null
                    _isRewardedAdReady.value = false
                    loadRewardedAd(activity)
                    // If ad display fails on device, still grant the reward gracefully
                    if (!rewardEarned) {
                        rewardEarned = true
                        onUserEarnedReward()
                    }
                    onAdDismissed()
                }

                override fun onAdShowedFullScreenContent() {
                    Log.d(TAG, "RewardedAd showed full screen content")
                }
            }

            currentAd.show(activity) { rewardItem ->
                Log.d(TAG, "User earned reward: ${rewardItem.amount} ${rewardItem.type}")
                rewardEarned = true
                onUserEarnedReward()
            }
        } else {
            // Fallback for offline / emulator test environments where ads cannot load
            Log.d(TAG, "Rewarded ad not cached; granting fallback reward in test mode")
            loadRewardedAd(activity)
            onUserEarnedReward()
            onAdDismissed()
        }
    }

    /**
     * Loads an Interstitial Ad.
     */
    fun loadInterstitialAd(context: Context) {
        if (interstitialAd != null || isInterstitialAdLoading.get()) return

        isInterstitialAdLoading.set(true)
        val adRequest = AdRequest.Builder().build()
        val adUnitId = AdConfig.interstitialAdUnitId

        try {
            InterstitialAd.load(
                context,
                adUnitId,
                adRequest,
                object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(ad: InterstitialAd) {
                        interstitialAd = ad
                        isInterstitialAdLoading.set(false)
                        Log.d(TAG, "InterstitialAd loaded successfully")
                    }

                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        interstitialAd = null
                        isInterstitialAdLoading.set(false)
                        Log.w(TAG, "InterstitialAd failed to load: ${loadAdError.message}")
                    }
                }
            )
        } catch (e: Throwable) {
            Log.w(TAG, "Exception during InterstitialAd.load: ${e.message}")
            isInterstitialAdLoading.set(false)
        }
    }

    /**
     * Shows an Interstitial Ad when completing a level and proceeding to next level.
     * Always calls onNextLevel when finished or if no ad is available.
     */
    fun showInterstitialAd(
        activity: Activity,
        onNextLevel: () -> Unit
    ) {
        val currentAd = interstitialAd
        if (currentAd != null) {
            currentAd.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "InterstitialAd dismissed by user")
                    interstitialAd = null
                    loadInterstitialAd(activity)
                    onNextLevel()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.w(TAG, "InterstitialAd failed to show: ${adError.message}")
                    interstitialAd = null
                    loadInterstitialAd(activity)
                    onNextLevel()
                }

                override fun onAdShowedFullScreenContent() {
                    Log.d(TAG, "InterstitialAd displayed")
                }
            }
            currentAd.show(activity)
        } else {
            // If ad not loaded or unavailable, proceed immediately
            loadInterstitialAd(activity)
            onNextLevel()
        }
    }
}
