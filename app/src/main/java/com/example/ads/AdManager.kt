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
    private const val TAG_MEDIATION = "AdMediation"

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
     * Displays a Rewarded Ad.
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
        val currentAd = rewardedAd
        // Immediately consume the cached ad reference and update ready state so reward buttons hide right away
        rewardedAd = null
        _isRewardedAdReady.value = false

        if (currentAd != null) {
            currentAd.fullScreenContentCallback = object : FullScreenContentCallback() {
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
                    val adapterName = currentAd.responseInfo.mediationAdapterClassName ?: "AdMob"
                    val network = when {
                        adapterName.contains("inmobi", ignoreCase = true) -> "InMobi"
                        adapterName.contains("unity", ignoreCase = true) -> "Unity Ads"
                        else -> "AdMob"
                    }
                    Log.i(TAG_MEDIATION, "RewardedAd showed full screen content (Network: $network, Adapter: $adapterName)")
                    AnalyticsHelper.logAdImpression("rewarded", AdConfig.rewardedAdUnitId, adapterName)
                }
            }

            currentAd.show(activity) { rewardItem ->
                Log.i(TAG, "User earned reward: ${rewardItem.amount} ${rewardItem.type}")
                AnalyticsHelper.logRewardedEarned(rewardItem.type, rewardItem.amount)
                onUserEarnedReward()
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
                    val network = when {
                        adapterName.contains("inmobi", ignoreCase = true) -> "InMobi"
                        adapterName.contains("unity", ignoreCase = true) -> "Unity Ads"
                        else -> "AdMob"
                    }
                    Log.i(TAG_MEDIATION, "InterstitialAd displayed full screen (Network: $network, Adapter: $adapterName)")
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
