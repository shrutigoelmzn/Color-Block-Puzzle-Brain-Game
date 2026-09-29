package com.example.ads

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.analytics.AnalyticsHelper
import com.example.crashlytics.CrashReporter
import com.example.util.DeviceUtils
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

/**
 * Manages Google Mobile Ads (AdMob) with Mediation.
 *
 * In headless emulator / preview environments, operations are safely simulated to avoid
 * Chromium WebView variations seed crashes and Binder transaction buffer limits (-ENOSPC).
 * On physical devices, standard AdMob production / test ads are requested with automatic preloading
 * and self-healing retry logic.
 */
object AdManager {
    private const val TAG = "AdManager"
    private val mainHandler = Handler(Looper.getMainLooper())

    private var rewardedAd: RewardedAd? = null
    private val isRewardedAdLoading = AtomicBoolean(false)
    private var isUsingFallbackTestAd = false

    private var interstitialAd: InterstitialAd? = null
    private val isInterstitialAdLoading = AtomicBoolean(false)

    private val _isRewardedAdReady = MutableStateFlow(false)
    val isRewardedAdReady: StateFlow<Boolean> = _isRewardedAdReady.asStateFlow()

    private val isInitializing = AtomicBoolean(false)
    private val isMobileAdsInitialized = AtomicBoolean(false)
    private var pendingRewardedLoad = false
    private var pendingInterstitialLoad = false

    private val _isMobileAdsReady = MutableStateFlow(false)
    val isMobileAdsReady: StateFlow<Boolean> = _isMobileAdsReady.asStateFlow()

    /**
     * Initializes Google Mobile Ads SDK on app startup and preloads ads immediately.
     */
    fun initialize(context: Context) {
        if (isInitializing.getAndSet(true)) return

        val appContext = context.applicationContext

        // In virtual/emulator environments, bypass heavy WebView initialization
        // to prevent IPC Binder -ENOSPC and GPU rendernode crashes.
        if (DeviceUtils.isEmulator()) {
            Log.i(TAG, "Running in Android emulator/container: enabling safe simulated ads mode")
            isMobileAdsInitialized.set(true)
            _isMobileAdsReady.value = true
            _isRewardedAdReady.value = true
            return
        }

        try {
            MobileAds.initialize(appContext) { initStatus ->
                isMobileAdsInitialized.set(true)
                _isMobileAdsReady.value = true
                Log.i(TAG, "AdMob MobileAds initialized successfully on device")
                initStatus.adapterStatusMap.forEach { (adapterClass, status) ->
                    Log.i(TAG, "Mediation Adapter: $adapterClass -> State: ${status.initializationState}, Description: ${status.description}")
                }

                // Automatically preload rewarded and interstitial ads immediately on startup
                loadRewardedAd(context)
                loadInterstitialAd(context)
            }
        } catch (e: Throwable) {
            Log.d(TAG, "AdMob initialization note: ${e.message}")
            _isMobileAdsReady.value = true
            _isRewardedAdReady.value = true
        }
    }

    /**
     * Loads a Rewarded Ad with automatic preloading and failure recovery.
     */
    fun loadRewardedAd(context: Context) {
        loadRewardedAdInternal(context, AdConfig.rewardedAdUnitId, isFallback = false)
    }

    private fun loadRewardedAdInternal(context: Context, adUnitId: String, isFallback: Boolean) {
        if (DeviceUtils.isEmulator()) {
            _isRewardedAdReady.value = true
            return
        }

        if (!isMobileAdsInitialized.get()) {
            Log.d(TAG, "loadRewardedAd: MobileAds not yet initialized, queuing request")
            pendingRewardedLoad = true
            return
        }

        if (rewardedAd != null) {
            _isRewardedAdReady.value = true
            return
        }

        if (isRewardedAdLoading.getAndSet(true)) {
            return
        }

        isUsingFallbackTestAd = isFallback
        val loadTargetContext = findActivity(context) ?: context
        val adRequest = AdRequest.Builder().build()

        try {
            Log.i(TAG, "Requesting Rewarded Ad with ID: $adUnitId (fallback=$isFallback)")
            RewardedAd.load(
                loadTargetContext,
                adUnitId,
                adRequest,
                object : RewardedAdLoadCallback() {
                    override fun onAdLoaded(ad: RewardedAd) {
                        rewardedAd = ad
                        isRewardedAdLoading.set(false)
                        _isRewardedAdReady.value = true
                        val mediationNetwork = ad.responseInfo.mediationAdapterClassName ?: "AdMob Network"
                        Log.i(TAG, "Rewarded ad loaded successfully from: $mediationNetwork")
                    }

                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        rewardedAd = null
                        isRewardedAdLoading.set(false)
                        _isRewardedAdReady.value = false
                        Log.w(TAG, "Rewarded ad failed to load [Code ${loadAdError.code}]: ${loadAdError.message}")

                        // If release ad unit returned No-Fill in debug build, fallback to official Google Test ID
                        if (loadAdError.code == AdRequest.ERROR_CODE_NO_FILL && AdConfig.isDebugMode && !isFallback) {
                            Log.i(TAG, "Release Ad Unit returned NO_FILL in debug build. Retrying with Google Test Ad Unit...")
                            loadRewardedAdInternal(context, AdConfig.TEST_REWARDED_AD_ID, isFallback = true)
                            return
                        }

                        // Schedule automatic retry with delay so ads self-heal
                        mainHandler.postDelayed({
                            if (rewardedAd == null && !isRewardedAdLoading.get()) {
                                loadRewardedAd(context)
                            }
                        }, 15000L)
                    }
                }
            )
        } catch (e: Throwable) {
            isRewardedAdLoading.set(false)
            _isRewardedAdReady.value = false
            Log.w(TAG, "loadRewardedAd exception: ${e.message}")
        }
    }

    /**
     * Shows a Rewarded Ad to the user.
     * If the ad was still loading or not ready, grants fallback reward so the player is never blocked.
     */
    fun showRewardedAd(
        activity: Activity,
        placement: String = "game_reward",
        onUserEarnedReward: (rewardAmount: Int) -> Unit = {},
        onAdDismissed: () -> Unit = {},
        onFailed: (String) -> Unit = {}
    ) {
        AnalyticsHelper.logAdImpression("rewarded", AdConfig.rewardedAdUnitId, "AdMob")

        if (DeviceUtils.isEmulator()) {
            Log.i(TAG, "Emulator mode: simulated reward granted for placement '$placement'")
            AnalyticsHelper.logRewardedEarned("rewarded", 1)
            onUserEarnedReward(1)
            onAdDismissed()
            return
        }

        val ad = rewardedAd
        if (ad == null) {
            Log.w(TAG, "showRewardedAd: ad not ready yet, initiating load and granting fallback reward for placement '$placement'")
            loadRewardedAd(activity)
            AnalyticsHelper.logRewardedEarned("rewarded", 1)
            onUserEarnedReward(1)
            onAdDismissed()
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                rewardedAd = null
                _isRewardedAdReady.value = false
            }

            override fun onAdDismissedFullScreenContent() {
                loadRewardedAd(activity)
                onAdDismissed()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                rewardedAd = null
                _isRewardedAdReady.value = false
                loadRewardedAd(activity)
                onFailed(adError.message)
            }
        }

        ad.show(activity) { rewardItem ->
            AnalyticsHelper.logRewardedEarned("rewarded", rewardItem.amount)
            onUserEarnedReward(rewardItem.amount)
        }
    }

    /**
     * Loads an Interstitial Ad with automatic preloading and failure recovery.
     */
    fun loadInterstitialAd(context: Context) {
        if (DeviceUtils.isEmulator()) {
            return
        }

        if (!isMobileAdsInitialized.get()) {
            pendingInterstitialLoad = true
            return
        }

        if (interstitialAd != null) {
            return
        }

        if (isInterstitialAdLoading.getAndSet(true)) {
            return
        }

        val loadTargetContext = findActivity(context) ?: context
        val adRequest = AdRequest.Builder().build()
        try {
            InterstitialAd.load(
                loadTargetContext,
                AdConfig.interstitialAdUnitId,
                adRequest,
                object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(ad: InterstitialAd) {
                        interstitialAd = ad
                        isInterstitialAdLoading.set(false)
                        val mediationNetwork = ad.responseInfo.mediationAdapterClassName ?: "AdMob Network"
                        Log.i(TAG, "Interstitial ad loaded successfully from: $mediationNetwork")
                    }

                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        interstitialAd = null
                        isInterstitialAdLoading.set(false)
                        Log.w(TAG, "Interstitial ad failed to load: ${loadAdError.message}")

                        if (loadAdError.code == AdRequest.ERROR_CODE_NO_FILL && AdConfig.isDebugMode) {
                            Log.i(TAG, "Release Interstitial returned NO_FILL in debug build. Retrying with Google Test Unit...")
                            InterstitialAd.load(
                                loadTargetContext,
                                AdConfig.TEST_INTERSTITIAL_AD_ID,
                                AdRequest.Builder().build(),
                                object : InterstitialAdLoadCallback() {
                                    override fun onAdLoaded(ad: InterstitialAd) {
                                        interstitialAd = ad
                                    }
                                    override fun onAdFailedToLoad(error: LoadAdError) {}
                                }
                            )
                        }
                    }
                }
            )
        } catch (e: Throwable) {
            isInterstitialAdLoading.set(false)
            Log.w(TAG, "loadInterstitialAd exception: ${e.message}")
        }
    }

    /**
     * Shows an Interstitial Ad.
     */
    fun showInterstitialAd(
        activity: Activity,
        placement: String = "game_over",
        onDismissed: () -> Unit = {}
    ) {
        AnalyticsHelper.logAdImpression("interstitial", AdConfig.interstitialAdUnitId, "AdMob")

        if (DeviceUtils.isEmulator()) {
            onDismissed()
            return
        }

        val ad = interstitialAd
        if (ad == null) {
            loadInterstitialAd(activity)
            onDismissed()
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                interstitialAd = null
            }

            override fun onAdDismissedFullScreenContent() {
                loadInterstitialAd(activity)
                onDismissed()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                interstitialAd = null
                loadInterstitialAd(activity)
                onDismissed()
            }
        }

        ad.show(activity)
    }

    /**
     * Helper to find an Activity from a given Context.
     */
    fun findActivity(context: Context): Activity? {
        var ctx = context
        while (ctx is ContextWrapper) {
            if (ctx is Activity) return ctx
            ctx = ctx.baseContext
        }
        return null
    }
}
