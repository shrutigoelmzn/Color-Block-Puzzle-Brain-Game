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
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.ResponseInfo
import com.google.android.gms.ads.initialization.AdapterStatus
import com.google.android.gms.ads.initialization.InitializationStatus
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.inmobi.sdk.InMobiSdk
import com.inmobi.sdk.InMobiSdk.LogLevel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Manages Google Mobile Ads (AdMob) with Mediation (Unity Ads Bidding & InMobi Waterfall).
 *
 * Flow:
 * App -> Google Mobile Ads SDK -> AdMob Mediation -> Unity Ads Bidding / InMobi Waterfall / AdMob Network.
 *
 * All ad requests route exclusively through the official AdMob Ad Unit IDs.
 * Unity Bidding placements require mediation bidding adMarkup/metadata and are NEVER loaded directly.
 */
object AdManager {
    private const val TAG = "AdManager"
    private const val TAG_MEDIATION = "AdMediation"

    private var rewardedAd: RewardedAd? = null
    private val isRewardedAdLoading = AtomicBoolean(false)

    private var interstitialAd: InterstitialAd? = null
    private val isInterstitialAdLoading = AtomicBoolean(false)

    private val _isRewardedAdReady = MutableStateFlow(false)
    val isRewardedAdReady: StateFlow<Boolean> = _isRewardedAdReady.asStateFlow()

    private val isInitializing = AtomicBoolean(false)
    private val isMobileAdsInitialized = AtomicBoolean(false)

    /**
     * Initializes Google Mobile Ads SDK on app startup and inspects mediation adapter readiness.
     */
    fun initialize(context: Context) {
        if (isInitializing.getAndSet(true)) return

        val appContext = context.applicationContext

        // Set InMobi log level to DEBUG so the device ID and diagnostics are logged for testing registration
        try {
            InMobiSdk.setLogLevel(LogLevel.DEBUG)
            Log.i(TAG_MEDIATION, "InMobiSdk.setLogLevel(LogLevel.DEBUG) configured successfully.")
        } catch (e: Throwable) {
            Log.d(TAG_MEDIATION, "InMobiSdk setLogLevel note: ${e.message}")
        }

        // Log device IDs to logcat for registering test device across platforms (AdMob, InMobi, Unity)
        logTestingDeviceIds(appContext)

        Log.i(TAG_MEDIATION, "Initializing Google Mobile Ads SDK (AdMob)...")
        try {
            MobileAds.initialize(appContext) { initializationStatus ->
                isMobileAdsInitialized.set(true)
                logInitializationStatus(initializationStatus)
                preloadAds(appContext)
            }
        } catch (e: Throwable) {
            Log.d(TAG_MEDIATION, "Failed to initialize AdMob: ${e.message}")
        }
    }

    /**
     * Logs device identifiers to logcat to help register the device as a test device
     * across AdMob, InMobi, and Unity Ads developer consoles.
     */
    private fun logTestingDeviceIds(context: Context) {
        try {
            val androidId = android.provider.Settings.Secure.getString(
                context.contentResolver,
                android.provider.Settings.Secure.ANDROID_ID
            ) ?: "UNKNOWN"

            val hashedAndroidId = try {
                val md = java.security.MessageDigest.getInstance("MD5")
                val digest = md.digest(androidId.toByteArray())
                digest.joinToString("") { "%02X".format(it) }
            } catch (e: Throwable) {
                "N/A"
            }

            val testDeviceIds = mutableListOf<String>()
            testDeviceIds.add(com.google.android.gms.ads.AdRequest.DEVICE_ID_EMULATOR)
            if (hashedAndroidId != "N/A" && hashedAndroidId.isNotBlank()) {
                testDeviceIds.add(hashedAndroidId)
            }
            try {
                val requestConfiguration = RequestConfiguration.Builder()
                    .setTestDeviceIds(testDeviceIds)
                    .build()
                MobileAds.setRequestConfiguration(requestConfiguration)
            } catch (e: Throwable) {
                Log.d(TAG_MEDIATION, "Error setting request configuration: ${e.message}")
            }

            Log.i("TEST_DEVICE_REGISTRATION", "==========================================================================")
            Log.i("TEST_DEVICE_REGISTRATION", "DEVICE IDENTIFIERS FOR TESTING REGISTRATION:")
            Log.i("TEST_DEVICE_REGISTRATION", "1. Android ID (SSAID): $androidId")
            Log.i("TEST_DEVICE_REGISTRATION", "2. Hashed Device ID (AdMob format): $hashedAndroidId")
            Log.i("TEST_DEVICE_REGISTRATION", "3. InMobi SDK: LogLevel.DEBUG active (look for 'InMobi' logcat tag)")
            Log.i("TEST_DEVICE_REGISTRATION", "4. AdMob Test Device: Registered ($testDeviceIds)")
            Log.i("TEST_DEVICE_REGISTRATION", "5. Ad Inspector: Use AdManager.openAdInspector(activity) or shake gesture")
            Log.i("TEST_DEVICE_REGISTRATION", "==========================================================================")
        } catch (e: Throwable) {
            Log.d(TAG, "Error logging test device IDs: ${e.message}")
        }
    }

    /**
     * Opens AdMob Ad Inspector.
     * In Ad Inspector:
     * 1. Go to "Single ad source test".
     * 2. Select InMobi or Unity Ads.
     * 3. All subsequent test ad requests will test ONLY that mediation network!
     */
    fun openAdInspector(activity: Activity, onClosed: ((error: String?) -> Unit)? = null) {
        try {
            MobileAds.openAdInspector(activity) { error ->
                if (error != null) {
                    val errMsg = "Ad Inspector error [Code ${error.code}]: ${error.message}"
                    Log.e(TAG_MEDIATION, errMsg)
                    onClosed?.invoke(errMsg)
                } else {
                    Log.i(TAG_MEDIATION, "Ad Inspector closed successfully.")
                    onClosed?.invoke(null)
                }
            }
        } catch (e: Throwable) {
            val errMsg = "Failed to open Ad Inspector: ${e.message}"
            Log.e(TAG_MEDIATION, errMsg, e)
            onClosed?.invoke(errMsg)
        }
    }

    /**
     * Inspects and logs mediation adapter initialization statuses (Unity Ads & InMobi).
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
                "Unity mediation adapter not pre-initialized (AdMob initializes Unity Ads dynamically upon ad request or bidding token generation)."
            )
        }
        if (!inmobiAdapterFound) {
            Log.i(
                TAG_MEDIATION,
                "InMobi mediation adapter not pre-initialized (AdMob initializes InMobi dynamically for Waterfall upon first ad request)."
            )
        }
    }

    /**
     * Preloads both rewarded and interstitial ads.
     */
    fun preloadAds(context: Context) {
        loadRewardedAd(context)
        loadInterstitialAd(context)
    }

    /**
     * Loads a Rewarded Ad via AdMob Mediation (Unity Bidding / InMobi Waterfall).
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

        Log.d(TAG, "Requesting Rewarded Ad via AdMob with Unit ID: $adUnitId")
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
            _isRewardedAdReady.value = false
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
        // Immediately consume cached ad reference and update ready state so reward buttons hide right away
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
                    loadRewardedAd(activity)
                    onAdDismissed()
                }

                override fun onAdShowedFullScreenContent() {
                    val adapterName = currentAd.responseInfo.mediationAdapterClassName ?: "AdMob"
                    val network = when {
                        adapterName.contains("inmobi", ignoreCase = true) -> "InMobi"
                        adapterName.contains("unity", ignoreCase = true) -> "Unity Ads"
                        else -> "Google AdMob"
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
            Log.w(TAG, "Rewarded ad requested but not ready or available")
            loadRewardedAd(activity)
            onAdDismissed()
        }
    }

    /**
     * Loads an Interstitial Ad via AdMob Mediation (Unity Bidding / InMobi Waterfall).
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

        Log.d(TAG, "Requesting Interstitial Ad via AdMob with Unit ID: $adUnitId")
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
            interstitialAd = null
            currentAd.fullScreenContentCallback = object : FullScreenContentCallback() {
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
                    val adapterName = currentAd.responseInfo.mediationAdapterClassName ?: "AdMob"
                    val network = when {
                        adapterName.contains("inmobi", ignoreCase = true) -> "InMobi"
                        adapterName.contains("unity", ignoreCase = true) -> "Unity Ads"
                        else -> "Google AdMob"
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
     * including winning network, response ID, adapter responses, latency, and mediation diagnostics.
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
            winningAdapter.contains("inmobi", ignoreCase = true) -> "InMobi Waterfall"
            winningAdapter.contains("unity", ignoreCase = true) -> "Unity Ads Bidding"
            winningAdapter.contains("google", ignoreCase = true) || winningAdapter.contains("admob", ignoreCase = true) -> "Google AdMob"
            else -> winningAdapter
        }

        Log.i(TAG_MEDIATION, "==========================================================================")
        Log.i(TAG_MEDIATION, "[SUCCESS] $adType LOADED successfully")
        Log.i(TAG_MEDIATION, "[$adType] SERVED BY NETWORK: >>> $networkIdentified <<<")
        Log.i(TAG_MEDIATION, "[$adType] Winning Adapter Class: $winningAdapter")
        Log.i(TAG_MEDIATION, "[$adType] Response ID: $responseId")

        if (loadedAdapter != null) {
            Log.i(
                TAG_MEDIATION,
                "[$adType] Loaded Ad Source: '${loadedAdapter.adSourceName}' (ID: ${loadedAdapter.adSourceId}) | Class: ${loadedAdapter.adapterClassName} | Latency: ${loadedAdapter.latencyMillis}ms"
            )
        }

        val adapterResponses = responseInfo.adapterResponses
        var unityParticipated = false
        var inmobiParticipated = false

        if (adapterResponses.isNotEmpty()) {
            Log.i(TAG_MEDIATION, "[$adType] Mediation auction/waterfall participants (${adapterResponses.size} sources):")
            adapterResponses.forEachIndexed { index, info ->
                val isUnity = info.adapterClassName.contains("unity", ignoreCase = true)
                val isInMobi = info.adapterClassName.contains("inmobi", ignoreCase = true)
                if (isUnity) unityParticipated = true
                if (isInMobi) inmobiParticipated = true

                val tag = when {
                    isUnity -> "[UNITY BIDDING]"
                    isInMobi -> "[INMOBI WATERFALL]"
                    else -> "[SOURCE]"
                }

                val resultDesc = if (info.adError == null) {
                    "-> WON / SERVED"
                } else {
                    "-> FAILED: [Code ${info.adError?.code}, Domain '${info.adError?.domain}'] ${info.adError?.message}"
                }
                Log.i(
                    TAG_MEDIATION,
                    "  [$index] $tag Source: '${info.adSourceName}' | Adapter: ${info.adapterClassName} | Latency: ${info.latencyMillis}ms $resultDesc"
                )
            }
        }

        Log.i(TAG_MEDIATION, "[$adType] Summary -> Unity Bidding participated: $unityParticipated | InMobi Waterfall participated: $inmobiParticipated")
        Log.i(TAG_MEDIATION, "==========================================================================")
    }

    /**
     * Helper to log mediation LoadAdError details under AdMediation tag,
     * including error codes, response ID, and individual candidate adapter errors (Unity Bidding & InMobi Waterfall).
     */
    private fun logLoadError(adType: String, loadAdError: LoadAdError) {
        Log.w(TAG_MEDIATION, "==========================================================================")
        Log.w(TAG_MEDIATION, "[FAILURE] $adType FAILED TO LOAD")
        Log.w(TAG_MEDIATION, "[$adType] Error Message: ${loadAdError.message}")
        Log.w(TAG_MEDIATION, "[$adType] Error Code: ${loadAdError.code} | Domain: ${loadAdError.domain}")

        val responseInfo = loadAdError.responseInfo
        if (responseInfo != null) {
            val responseId = responseInfo.responseId ?: "N/A"
            Log.w(TAG_MEDIATION, "[$adType] Response ID: $responseId")

            val adapterResponses = responseInfo.adapterResponses
            var unityParticipated = false
            var inmobiParticipated = false

            if (adapterResponses.isNotEmpty()) {
                Log.w(TAG_MEDIATION, "[$adType] Candidate mediation adapter failures (${adapterResponses.size} sources):")
                adapterResponses.forEachIndexed { index, info ->
                    val isUnity = info.adapterClassName.contains("unity", ignoreCase = true)
                    val isInMobi = info.adapterClassName.contains("inmobi", ignoreCase = true)
                    if (isUnity) unityParticipated = true
                    if (isInMobi) inmobiParticipated = true

                    val tag = when {
                        isUnity -> "[UNITY BIDDING]"
                        isInMobi -> "[INMOBI WATERFALL]"
                        else -> "[SOURCE]"
                    }

                    val err = info.adError
                    val errDetails = if (err != null) {
                        "Error: [Code ${err.code}, Domain '${err.domain}'] ${err.message}"
                    } else {
                        "No specific adapter error reported"
                    }

                    Log.w(
                        TAG_MEDIATION,
                        "  [$index] $tag Source: '${info.adSourceName}' | Adapter: ${info.adapterClassName} | Latency: ${info.latencyMillis}ms | $errDetails"
                    )
                }
            } else {
                Log.w(TAG_MEDIATION, "[$adType] No mediation adapter responses recorded by AdMob for this request.")
            }

            Log.w(TAG_MEDIATION, "[$adType] Summary -> Unity Bidding participated: $unityParticipated | InMobi Waterfall participated: $inmobiParticipated")
        }
        Log.w(TAG_MEDIATION, "==========================================================================")
    }
}
