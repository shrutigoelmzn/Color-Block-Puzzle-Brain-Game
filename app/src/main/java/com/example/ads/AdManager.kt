package com.example.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.example.analytics.AnalyticsHelper
import com.example.crashlytics.CrashReporter
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.ResponseInfo
import com.google.android.gms.ads.initialization.AdapterStatus
import com.google.android.gms.ads.initialization.InitializationStatus
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

    private val isInitializing = AtomicBoolean(false)
    private val isMobileAdsInitialized = AtomicBoolean(false)

    /**
     * Initializes Google Mobile Ads SDK on app startup and logs mediation adapter statuses.
     */
    fun initialize(context: Context) {
        if (isInitializing.getAndSet(true)) return

        Log.i(TAG, "Initializing Google Mobile Ads SDK (AdMob)...")
        try {
            MobileAds.initialize(context) { initializationStatus ->
                isMobileAdsInitialized.set(true)
                logInitializationStatus(initializationStatus)
                preloadAds(context)
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to initialize AdMob: ${e.message}", e)
            CrashReporter.recordException(e)
        }
    }

    /**
     * Logs Google Mobile Ads SDK initialization and inspects mediation adapters (specifically Unity Ads).
     */
    private fun logInitializationStatus(status: InitializationStatus) {
        val adapterMap = status.adapterStatusMap
        Log.i(TAG, "Google Mobile Ads initialization completed. Registered mediation adapters count: ${adapterMap.size}")

        var unityAdapterFound = false
        adapterMap.forEach { (adapterClass, adapterStatus) ->
            val isReady = adapterStatus.initializationState == AdapterStatus.State.READY
            val stateStr = adapterStatus.initializationState.name
            val logMessage = "Mediation Adapter: [$adapterClass] -> State: $stateStr, Description: '${adapterStatus.description}', Latency: ${adapterStatus.latency}ms"

            if (adapterClass.contains("unity", ignoreCase = true)) {
                unityAdapterFound = true
                Log.i(TAG, "[UNITY MEDIATION] $logMessage")
            } else {
                Log.d(TAG, logMessage)
            }
        }

        if (!unityAdapterFound) {
            Log.i(
                TAG,
                "Unity mediation adapter not explicitly pre-initialized in adapter map (AdMob may initialize Unity Ads dynamically upon first ad request)."
            )
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
        if (!isMobileAdsInitialized.get()) {
            Log.d(TAG, "MobileAds not yet initialized. Rewarded ad load will trigger after initialization.")
            return
        }

        if (rewardedAd != null || isRewardedAdLoading.get()) return

        isRewardedAdLoading.set(true)
        val adRequest = AdRequest.Builder().build()
        val adUnitId = AdConfig.rewardedAdUnitId

        Log.d(TAG, "Requesting Rewarded Ad with Unit ID: $adUnitId")
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
                        Log.i(TAG, "RewardedAd successfully loaded!")
                        logAdResponseInfo("RewardedAd", ad.responseInfo)
                    }

                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        rewardedAd = null
                        isRewardedAdLoading.set(false)
                        _isRewardedAdReady.value = false
                        logLoadError("RewardedAd", loadAdError)
                    }
                }
            )
        } catch (e: Throwable) {
            Log.w(TAG, "Exception during RewardedAd.load: ${e.message}")
            CrashReporter.recordException(e)
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
                    Log.d(TAG, "RewardedAd dismissed by user")
                    rewardedAd = null
                    _isRewardedAdReady.value = false
                    loadRewardedAd(activity)
                    onAdDismissed()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.w(TAG, "RewardedAd failed to show: ${adError.message} (code: ${adError.code})")
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
                    val adapterName = currentAd.responseInfo.mediationAdapterClassName ?: "AdMob"
                    Log.i(TAG, "RewardedAd showed full screen content (Served by adapter: $adapterName)")
                    AnalyticsHelper.logAdImpression("rewarded", AdConfig.rewardedAdUnitId, adapterName)
                }
            }

            currentAd.show(activity) { rewardItem ->
                Log.i(TAG, "User earned reward: ${rewardItem.amount} ${rewardItem.type}")
                rewardEarned = true
                AnalyticsHelper.logRewardedEarned(rewardItem.type, rewardItem.amount)
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
        if (!isMobileAdsInitialized.get()) {
            Log.d(TAG, "MobileAds not yet initialized. Interstitial ad load will trigger after initialization.")
            return
        }

        if (interstitialAd != null || isInterstitialAdLoading.get()) return

        isInterstitialAdLoading.set(true)
        val adRequest = AdRequest.Builder().build()
        val adUnitId = AdConfig.interstitialAdUnitId

        Log.d(TAG, "Requesting Interstitial Ad with Unit ID: $adUnitId")
        try {
            InterstitialAd.load(
                context,
                adUnitId,
                adRequest,
                object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(ad: InterstitialAd) {
                        interstitialAd = ad
                        isInterstitialAdLoading.set(false)
                        Log.i(TAG, "InterstitialAd loaded successfully!")
                        logAdResponseInfo("InterstitialAd", ad.responseInfo)
                    }

                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        interstitialAd = null
                        isInterstitialAdLoading.set(false)
                        logLoadError("InterstitialAd", loadAdError)
                    }
                }
            )
        } catch (e: Throwable) {
            Log.w(TAG, "Exception during InterstitialAd.load: ${e.message}")
            CrashReporter.recordException(e)
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
                    Log.w(TAG, "InterstitialAd failed to show: ${adError.message} (code: ${adError.code})")
                    interstitialAd = null
                    loadInterstitialAd(activity)
                    onNextLevel()
                }

                override fun onAdShowedFullScreenContent() {
                    val adapterName = currentAd.responseInfo.mediationAdapterClassName ?: "AdMob"
                    Log.i(TAG, "InterstitialAd displayed full screen (Served by adapter: $adapterName)")
                    AnalyticsHelper.logAdImpression("interstitial", AdConfig.interstitialAdUnitId, adapterName)
                }
            }
            currentAd.show(activity)
        } else {
            // If ad not loaded or unavailable, proceed immediately
            loadInterstitialAd(activity)
            onNextLevel()
        }
    }

    /**
     * Helper to log mediation ResponseInfo details, including winning network and waterfall chain.
     */
    private fun logAdResponseInfo(adType: String, responseInfo: ResponseInfo?) {
        if (responseInfo == null) {
            Log.d(TAG, "[$adType] ResponseInfo is null")
            return
        }

        Log.i(TAG, "[$adType] Winning mediation adapter: ${responseInfo.mediationAdapterClassName}")
        val loadedAdapter = responseInfo.loadedAdapterResponseInfo
        if (loadedAdapter != null) {
            Log.i(
                TAG,
                "[$adType] Served by Ad Source: '${loadedAdapter.adSourceName}' (ID: ${loadedAdapter.adSourceId}, Instance: '${loadedAdapter.adSourceInstanceName}', Adapter Class: ${loadedAdapter.adapterClassName}, Latency: ${loadedAdapter.latencyMillis}ms)"
            )
        }

        val adapterResponses = responseInfo.adapterResponses
        if (adapterResponses.isNotEmpty()) {
            Log.d(TAG, "[$adType] Mediation mediation chain attempted (${adapterResponses.size} sources):")
            adapterResponses.forEachIndexed { index, info ->
                val errorMsg = info.adError?.let { " -> FAILED: ${it.message} (code ${it.code})" } ?: " -> SUCCESS"
                Log.d(TAG, "  [$index] ${info.adSourceName} (${info.adapterClassName})$errorMsg, latency: ${info.latencyMillis}ms")
            }
        }
    }

    /**
     * Helper to log mediation LoadAdError details.
     */
    private fun logLoadError(adType: String, loadAdError: LoadAdError) {
        Log.w(
            TAG,
            "[$adType] Failed to load: ${loadAdError.message} (Code: ${loadAdError.code}, Domain: ${loadAdError.domain})"
        )
        val responseInfo = loadAdError.responseInfo
        if (responseInfo != null) {
            Log.w(TAG, "[$adType] Error Response ID: ${responseInfo.responseId}")
            val adapterResponses = responseInfo.adapterResponses
            if (adapterResponses.isNotEmpty()) {
                Log.w(TAG, "[$adType] Mediation chain failures (${adapterResponses.size} sources):")
                adapterResponses.forEachIndexed { index, info ->
                    Log.w(
                        TAG,
                        "  [$index] Ad Source '${info.adSourceName}' (${info.adapterClassName}) failed: ${info.adError?.message} (code: ${info.adError?.code})"
                    )
                }
            }
        }
    }
}
