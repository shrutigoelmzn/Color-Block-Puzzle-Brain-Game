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
import com.unity3d.ads.IUnityAdsInitializationListener
import com.unity3d.ads.IUnityAdsLoadListener
import com.unity3d.ads.IUnityAdsShowListener
import com.unity3d.ads.UnityAds
import com.unity3d.ads.UnityAdsShowOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean

object AdManager {
    private const val TAG = "AdManager"
    private const val TAG_MEDIATION = "AdMediation"
    private const val TAG_UNITY_DIRECT = "UnityDirect"

    private var rewardedAd: RewardedAd? = null
    private var isRewardedAdLoading = AtomicBoolean(false)

    // Direct Unity Ads fallback state
    private val isUnityDirectRewardedReady = AtomicBoolean(false)
    private val isUnityDirectRewardedLoading = AtomicBoolean(false)
    private val isUnityDirectInterstitialReady = AtomicBoolean(false)
    private val isUnityDirectInterstitialLoading = AtomicBoolean(false)
    private val isUnityDirectInitialized = AtomicBoolean(false)

    private var interstitialAd: InterstitialAd? = null
    private var isInterstitialAdLoading = AtomicBoolean(false)

    private val _isRewardedAdReady = MutableStateFlow(false)
    val isRewardedAdReady: StateFlow<Boolean> = _isRewardedAdReady.asStateFlow()

    private val isInitializing = AtomicBoolean(false)
    private val isMobileAdsInitialized = AtomicBoolean(false)

    /**
     * Initializes Google Mobile Ads SDK on app startup and logs mediation adapter statuses.
     * Also initializes Unity Ads direct SDK as fallback if Unity Game ID is configured.
     */
    fun initialize(context: Context) {
        if (isInitializing.getAndSet(true)) return

        // Initialize Direct Unity Ads SDK if Unity Game ID is provided
        initUnityDirectSdk(context)

        Log.i(TAG_MEDIATION, "Initializing Google Mobile Ads SDK (AdMob)...")
        try {
            MobileAds.initialize(context) { initializationStatus ->
                isMobileAdsInitialized.set(true)
                logInitializationStatus(initializationStatus)
                preloadAds(context)
            }
        } catch (e: Throwable) {
            Log.e(TAG_MEDIATION, "Failed to initialize AdMob: ${e.message}", e)
            CrashReporter.recordException(e)
        }
    }

    /**
     * Initializes Unity Ads SDK directly for backup serving when Google ads are limited.
     */
    private fun initUnityDirectSdk(context: Context) {
        val gameId = AdConfig.UNITY_GAME_ID.trim()
        if (gameId.isEmpty()) {
            Log.d(TAG_UNITY_DIRECT, "Unity Game ID not configured in AdConfig. Unity Direct fallback inactive.")
            return
        }

        Log.i(TAG_UNITY_DIRECT, "Initializing Unity Ads direct SDK (Game ID: $gameId, TestMode: ${AdConfig.UNITY_TEST_MODE})...")
        try {
            UnityAds.initialize(
                context.applicationContext,
                gameId,
                AdConfig.UNITY_TEST_MODE,
                object : IUnityAdsInitializationListener {
                    override fun onInitializationComplete() {
                        isUnityDirectInitialized.set(true)
                        Log.i(TAG_UNITY_DIRECT, "Unity Ads direct SDK initialized successfully!")
                    }

                    override fun onInitializationFailed(error: UnityAds.UnityAdsInitializationError, message: String) {
                        isUnityDirectInitialized.set(false)
                        Log.w(TAG_UNITY_DIRECT, "Unity Ads direct initialization failed: $error - $message")
                    }
                }
            )
        } catch (e: Throwable) {
            Log.w(TAG_UNITY_DIRECT, "Exception during UnityAds.initialize: ${e.message}")
        }
    }

    /**
     * Logs Google Mobile Ads SDK initialization and inspects mediation adapters (Unity Ads & InMobi).
     */
    private fun logInitializationStatus(status: InitializationStatus) {
        val adapterMap = status.adapterStatusMap
        Log.i(TAG_MEDIATION, "Google Mobile Ads initialization completed. Registered adapters count: ${adapterMap.size}")

        var unityAdapterFound = false
        var inmobiAdapterFound = false

        adapterMap.forEach { (adapterClass, adapterStatus) ->
            val isReady = adapterStatus.initializationState == AdapterStatus.State.READY
            val stateStr = adapterStatus.initializationState.name
            val logMessage = "Adapter: [$adapterClass] -> State: $stateStr, Description: '${adapterStatus.description}', Latency: ${adapterStatus.latency}ms"

            if (adapterClass.contains("inmobi", ignoreCase = true)) {
                inmobiAdapterFound = true
                Log.i(TAG_MEDIATION, "[INMOBI MEDIATION] $logMessage")
            } else if (adapterClass.contains("unity", ignoreCase = true)) {
                unityAdapterFound = true
                Log.i(TAG_MEDIATION, "[UNITY MEDIATION] $logMessage")
            } else {
                Log.d(TAG_MEDIATION, logMessage)
            }
        }

        if (!unityAdapterFound) {
            Log.i(
                TAG_MEDIATION,
                "Unity mediation adapter not in pre-initialization map (AdMob will initialize dynamically upon ad request)."
            )
        }
        if (!inmobiAdapterFound) {
            Log.i(
                TAG_MEDIATION,
                "InMobi mediation adapter not in pre-initialization map (AdMob will initialize InMobi dynamically for Waterfall upon first ad request)."
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

        if (rewardedAd != null || isRewardedAdLoading.get() || isUnityDirectRewardedReady.get()) return

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
                        logLoadError("RewardedAd", loadAdError)

                        // If Google ads are limited or failed, try direct Unity Ads fallback
                        if (AdConfig.UNITY_GAME_ID.isNotBlank()) {
                            Log.i(TAG_UNITY_DIRECT, "AdMob rewarded ad failed (Code ${loadAdError.code}). Trying direct Unity Ads fallback...")
                            loadUnityDirectRewardedAd(context)
                        } else {
                            _isRewardedAdReady.value = false
                        }
                    }
                }
            )
        } catch (e: Throwable) {
            Log.w(TAG, "Exception during RewardedAd.load: ${e.message}")
            CrashReporter.recordException(e)
            isRewardedAdLoading.set(false)
            if (AdConfig.UNITY_GAME_ID.isNotBlank()) {
                loadUnityDirectRewardedAd(context)
            }
        }
    }

    /**
     * Loads Unity Ads directly when AdMob is limited or has no fill.
     */
    fun loadUnityDirectRewardedAd(context: Context) {
        val gameId = AdConfig.UNITY_GAME_ID.trim()
        val placementId = AdConfig.UNITY_REWARDED_PLACEMENT_ID
        if (gameId.isEmpty() || isUnityDirectRewardedLoading.get() || isUnityDirectRewardedReady.get()) return

        if (!isUnityDirectInitialized.get()) {
            initUnityDirectSdk(context)
        }

        isUnityDirectRewardedLoading.set(true)
        Log.i(TAG_UNITY_DIRECT, "Requesting direct Unity Rewarded Ad on placement '$placementId' (Test Mode: ${AdConfig.UNITY_TEST_MODE})...")
        try {
            UnityAds.load(
                placementId,
                object : IUnityAdsLoadListener {
                    override fun onUnityAdsAdLoaded(loadedPlacementId: String) {
                        isUnityDirectRewardedLoading.set(false)
                        isUnityDirectRewardedReady.set(true)
                        _isRewardedAdReady.value = true
                        Log.i(TAG_UNITY_DIRECT, "[SUCCESS] Direct Unity Rewarded Ad ready for placement '$loadedPlacementId'!")
                    }

                    override fun onUnityAdsFailedToLoad(
                        failedPlacementId: String,
                        error: UnityAds.UnityAdsLoadError,
                        message: String
                    ) {
                        isUnityDirectRewardedLoading.set(false)
                        isUnityDirectRewardedReady.set(false)
                        _isRewardedAdReady.value = false
                        Log.w(
                            TAG_UNITY_DIRECT,
                            "[FAILED] Direct Unity Rewarded Ad failed to load ('$failedPlacementId'): $error - $message. " +
                                "Note: For a newly generated Unity Ads account, Test Mode MUST be enabled in the Unity Dashboard!"
                        )
                    }
                }
            )
        } catch (e: Throwable) {
            isUnityDirectRewardedLoading.set(false)
            _isRewardedAdReady.value = false
            Log.w(TAG_UNITY_DIRECT, "Exception during direct UnityAds.load: ${e.message}")
        }
    }

    /**
     * Displays a Rewarded Ad (via AdMob/Mediation if ready, or direct Unity Ads fallback).
     *
     * @param activity The calling activity
     * @param onUserEarnedReward Invoked ONLY when the user earns the reward via rewarded callback
     * @param onAdDismissed Invoked when the ad finishes displaying or fails
     */
    fun showRewardedAd(
        activity: Activity,
        onUserEarnedReward: () -> Unit,
        onAdDismissed: () -> Unit = {}
    ) {
        val currentAdMobAd = rewardedAd
        rewardedAd = null

        val canShowUnityDirect = isUnityDirectRewardedReady.getAndSet(false)

        // Immediately update ready state so reward buttons hide right away
        _isRewardedAdReady.value = false

        if (currentAdMobAd != null) {
            currentAdMobAd.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "RewardedAd dismissed by user")
                    loadRewardedAd(activity)
                    onAdDismissed()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.w(TAG, "RewardedAd failed to show: ${adError.message} (code: ${adError.code})")
                    // Do NOT grant reward on ad show failure
                    loadRewardedAd(activity)
                    onAdDismissed()
                }

                override fun onAdShowedFullScreenContent() {
                    val adapterName = currentAdMobAd.responseInfo.mediationAdapterClassName ?: "AdMob"
                    val network = when {
                        adapterName.contains("inmobi", ignoreCase = true) -> "InMobi"
                        adapterName.contains("unity", ignoreCase = true) -> "Unity Ads"
                        else -> "AdMob"
                    }
                    Log.i(TAG_MEDIATION, "RewardedAd showed full screen content (Network: $network, Adapter: $adapterName)")
                    AnalyticsHelper.logAdImpression("rewarded", AdConfig.rewardedAdUnitId, adapterName)
                }
            }

            currentAdMobAd.show(activity) { rewardItem ->
                Log.i(TAG, "User earned reward: ${rewardItem.amount} ${rewardItem.type}")
                AnalyticsHelper.logRewardedEarned(rewardItem.type, rewardItem.amount)
                onUserEarnedReward()
            }
        } else if (canShowUnityDirect) {
            Log.i(TAG_UNITY_DIRECT, "Displaying direct Unity Rewarded Ad...")
            try {
                UnityAds.show(
                    activity,
                    AdConfig.UNITY_REWARDED_PLACEMENT_ID,
                    UnityAdsShowOptions(),
                    object : IUnityAdsShowListener {
                        override fun onUnityAdsShowFailure(
                            placementId: String,
                            error: UnityAds.UnityAdsShowError,
                            message: String
                        ) {
                            Log.w(TAG_UNITY_DIRECT, "Unity Ads direct show failed: $error - $message")
                            loadRewardedAd(activity)
                            onAdDismissed()
                        }

                        override fun onUnityAdsShowStart(placementId: String) {
                            Log.i(TAG_UNITY_DIRECT, "Unity Ads direct started playing: $placementId")
                            AnalyticsHelper.logAdImpression("rewarded_unity_direct", placementId, "UnityAdsDirect")
                        }

                        override fun onUnityAdsShowClick(placementId: String) {
                            Log.d(TAG_UNITY_DIRECT, "Unity Ads direct ad clicked")
                        }

                        override fun onUnityAdsShowComplete(
                            placementId: String,
                            state: UnityAds.UnityAdsShowCompletionState
                        ) {
                            Log.i(TAG_UNITY_DIRECT, "Unity Ads direct finished with state: $state")
                            if (state == UnityAds.UnityAdsShowCompletionState.COMPLETED) {
                                AnalyticsHelper.logRewardedEarned("unity_direct_reward", 1)
                                onUserEarnedReward()
                            }
                            loadRewardedAd(activity)
                            onAdDismissed()
                        }
                    }
                )
            } catch (e: Throwable) {
                Log.w(TAG_UNITY_DIRECT, "Exception during UnityAds.show: ${e.message}")
                loadRewardedAd(activity)
                onAdDismissed()
            }
        } else {
            // Ad is not loaded / ready. Never grant fallback reward!
            Log.w(TAG, "Rewarded ad requested but not available or loaded")
            loadRewardedAd(activity)
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

        if (interstitialAd != null || isInterstitialAdLoading.get() || isUnityDirectInterstitialReady.get()) return

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

                        // If Google ads are limited or failed, try direct Unity Ads fallback
                        if (AdConfig.UNITY_GAME_ID.isNotBlank()) {
                            Log.i(TAG_UNITY_DIRECT, "AdMob interstitial ad failed (Code ${loadAdError.code}). Trying direct Unity Ads fallback...")
                            loadUnityDirectInterstitialAd(context)
                        }
                    }
                }
            )
        } catch (e: Throwable) {
            Log.w(TAG, "Exception during InterstitialAd.load: ${e.message}")
            CrashReporter.recordException(e)
            isInterstitialAdLoading.set(false)
            if (AdConfig.UNITY_GAME_ID.isNotBlank()) {
                loadUnityDirectInterstitialAd(context)
            }
        }
    }

    /**
     * Loads Unity Interstitial Ad directly when AdMob has no fill or ad limits.
     */
    fun loadUnityDirectInterstitialAd(context: Context) {
        val gameId = AdConfig.UNITY_GAME_ID.trim()
        val placementId = AdConfig.UNITY_INTERSTITIAL_PLACEMENT_ID
        if (gameId.isEmpty() || isUnityDirectInterstitialLoading.get() || isUnityDirectInterstitialReady.get()) return

        if (!isUnityDirectInitialized.get()) {
            initUnityDirectSdk(context)
        }

        isUnityDirectInterstitialLoading.set(true)
        Log.i(TAG_UNITY_DIRECT, "Requesting direct Unity Interstitial Ad on placement '$placementId'...")
        try {
            UnityAds.load(
                placementId,
                object : IUnityAdsLoadListener {
                    override fun onUnityAdsAdLoaded(loadedPlacementId: String) {
                        isUnityDirectInterstitialLoading.set(false)
                        isUnityDirectInterstitialReady.set(true)
                        Log.i(TAG_UNITY_DIRECT, "[SUCCESS] Direct Unity Interstitial ready for placement '$loadedPlacementId'!")
                    }

                    override fun onUnityAdsFailedToLoad(
                        failedPlacementId: String,
                        error: UnityAds.UnityAdsLoadError,
                        message: String
                    ) {
                        isUnityDirectInterstitialLoading.set(false)
                        isUnityDirectInterstitialReady.set(false)
                        Log.w(TAG_UNITY_DIRECT, "[FAILED] Direct Unity Interstitial failed ('$failedPlacementId'): $error - $message")
                    }
                }
            )
        } catch (e: Throwable) {
            isUnityDirectInterstitialLoading.set(false)
            Log.w(TAG_UNITY_DIRECT, "Exception loading direct Unity Interstitial: ${e.message}")
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
        val currentAdMobAd = interstitialAd
        val canShowUnityDirect = isUnityDirectInterstitialReady.getAndSet(false)

        if (currentAdMobAd != null) {
            interstitialAd = null
            currentAdMobAd.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "InterstitialAd dismissed by user")
                    loadInterstitialAd(activity)
                    onNextLevel()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.w(TAG, "InterstitialAd failed to show: ${adError.message} (code: ${adError.code})")
                    loadInterstitialAd(activity)
                    onNextLevel()
                }

                override fun onAdShowedFullScreenContent() {
                    val adapterName = currentAdMobAd.responseInfo.mediationAdapterClassName ?: "AdMob"
                    val network = when {
                        adapterName.contains("inmobi", ignoreCase = true) -> "InMobi"
                        adapterName.contains("unity", ignoreCase = true) -> "Unity Ads"
                        else -> "AdMob"
                    }
                    Log.i(TAG_MEDIATION, "InterstitialAd displayed full screen (Network: $network, Adapter: $adapterName)")
                    AnalyticsHelper.logAdImpression("interstitial", AdConfig.interstitialAdUnitId, adapterName)
                }
            }
            currentAdMobAd.show(activity)
        } else if (canShowUnityDirect) {
            Log.i(TAG_UNITY_DIRECT, "Displaying direct Unity Interstitial Ad...")
            try {
                UnityAds.show(
                    activity,
                    AdConfig.UNITY_INTERSTITIAL_PLACEMENT_ID,
                    UnityAdsShowOptions(),
                    object : IUnityAdsShowListener {
                        override fun onUnityAdsShowFailure(
                            placementId: String,
                            error: UnityAds.UnityAdsShowError,
                            message: String
                        ) {
                            Log.w(TAG_UNITY_DIRECT, "Unity Ads direct interstitial show failed: $error - $message")
                            loadInterstitialAd(activity)
                            onNextLevel()
                        }

                        override fun onUnityAdsShowStart(placementId: String) {
                            Log.i(TAG_UNITY_DIRECT, "Unity Ads direct interstitial started playing: $placementId")
                            AnalyticsHelper.logAdImpression("interstitial_unity_direct", placementId, "UnityAdsDirect")
                        }

                        override fun onUnityAdsShowClick(placementId: String) {
                            Log.d(TAG_UNITY_DIRECT, "Unity Ads direct interstitial clicked")
                        }

                        override fun onUnityAdsShowComplete(
                            placementId: String,
                            state: UnityAds.UnityAdsShowCompletionState
                        ) {
                            Log.i(TAG_UNITY_DIRECT, "Unity Ads direct interstitial completed: $state")
                            loadInterstitialAd(activity)
                            onNextLevel()
                        }
                    }
                )
            } catch (e: Throwable) {
                Log.w(TAG_UNITY_DIRECT, "Exception showing direct Unity Interstitial: ${e.message}")
                loadInterstitialAd(activity)
                onNextLevel()
            }
        } else {
            // If ad not loaded or unavailable, proceed immediately
            loadInterstitialAd(activity)
            onNextLevel()
        }
    }

    /**
     * Helper to log mediation ResponseInfo details under AdMediation tag,
     * including winning network, response ID, adapter responses, latency, and errors.
     */
    private fun logAdResponseInfo(adType: String, responseInfo: ResponseInfo?) {
        if (responseInfo == null) {
            Log.d(TAG_MEDIATION, "[$adType] ResponseInfo is null")
            return
        }

        val winningAdapter = responseInfo.mediationAdapterClassName ?: "AdMob"
        val responseId = responseInfo.responseId ?: "N/A"
        val loadedAdapter = responseInfo.loadedAdapterResponseInfo

        val networkIdentified = when {
            winningAdapter.contains("inmobi", ignoreCase = true) -> "InMobi"
            winningAdapter.contains("unity", ignoreCase = true) -> "Unity Ads"
            winningAdapter.contains("google", ignoreCase = true) || winningAdapter.contains("admob", ignoreCase = true) -> "Google AdMob"
            else -> winningAdapter
        }

        Log.i(TAG_MEDIATION, "==========================================================================")
        Log.i(TAG_MEDIATION, "[SUCCESS] $adType LOADED successfully")
        Log.i(TAG_MEDIATION, "[$adType] SERVED BY NETWORK: >>> $networkIdentified <<<")
        Log.i(TAG_MEDIATION, "[$adType] mediationAdapterClassName: $winningAdapter")
        Log.i(TAG_MEDIATION, "[$adType] responseId: $responseId")

        if (loadedAdapter != null) {
            Log.i(TAG_MEDIATION, "[$adType] Loaded Ad Source Name: '${loadedAdapter.adSourceName}' (ID: ${loadedAdapter.adSourceId})")
            Log.i(TAG_MEDIATION, "[$adType] Loaded Adapter Class: ${loadedAdapter.adapterClassName}")
            Log.i(TAG_MEDIATION, "[$adType] Loaded Latency: ${loadedAdapter.latencyMillis}ms")
        }

        val adapterResponses = responseInfo.adapterResponses
        if (adapterResponses.isNotEmpty()) {
            Log.i(TAG_MEDIATION, "[$adType] Mediation candidate adapter responses (${adapterResponses.size} sources in auction/waterfall):")
            adapterResponses.forEachIndexed { index, info ->
                val errorDesc = info.adError?.let { " -> FAILED: [Code ${it.code}, Domain '${it.domain}'] ${it.message}" } ?: " -> SUCCESS (Served this ad)"
                Log.i(TAG_MEDIATION, "  [$index] Ad Source: '${info.adSourceName}' | Adapter: ${info.adapterClassName} | Latency: ${info.latencyMillis}ms$errorDesc")
            }
        }
        Log.i(TAG_MEDIATION, "==========================================================================")
    }

    /**
     * Helper to log mediation LoadAdError details under AdMediation tag.
     */
    private fun logLoadError(adType: String, loadAdError: LoadAdError) {
        Log.w(TAG_MEDIATION, "==========================================================================")
        Log.w(TAG_MEDIATION, "[FAILURE] $adType FAILED TO LOAD")
        Log.w(TAG_MEDIATION, "[$adType] Error Message: ${loadAdError.message}")
        Log.w(TAG_MEDIATION, "[$adType] Error Code: ${loadAdError.code} | Domain: ${loadAdError.domain}")

        val responseInfo = loadAdError.responseInfo
        if (responseInfo != null) {
            Log.w(TAG_MEDIATION, "[$adType] responseId: ${responseInfo.responseId ?: "N/A"}")
            val adapterResponses = responseInfo.adapterResponses
            if (adapterResponses.isNotEmpty()) {
                Log.w(TAG_MEDIATION, "[$adType] Candidate mediation adapter failures (${adapterResponses.size} sources):")
                adapterResponses.forEachIndexed { index, info ->
                    val err = info.adError
                    Log.w(
                        TAG_MEDIATION,
                        "  [$index] Ad Source: '${info.adSourceName}' | Adapter: ${info.adapterClassName} | Latency: ${info.latencyMillis}ms | Error: [Code ${err?.code}, Domain '${err?.domain}'] ${err?.message}"
                    )
                }
            }
        }
        Log.w(TAG_MEDIATION, "==========================================================================")
    }
}
