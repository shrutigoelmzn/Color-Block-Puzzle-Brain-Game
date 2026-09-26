package com.example.ads

import android.content.Context
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

/**
 * Reusable Compose Banner Ad component that loads an Anchored Adaptive Banner
 * using AdMob and any configured mediation sources (e.g. InMobi, Unity Ads).
 *
 * Automatically manages AdView lifecycle (pause, resume, destroy) and fits
 * dynamically according to available screen space.
 */
@Composable
fun BannerAdView(
    modifier: Modifier = Modifier,
    adUnitId: String = AdConfig.bannerAdUnitId,
    backgroundColor: Color = Color.Transparent
) {
    if (LocalInspectionMode.current) {
        // Preview mode placeholder
        Box(
            modifier = modifier
                .fillMaxWidth()
                .heightIn(min = 50.dp)
                .background(Color.DarkGray.copy(alpha = 0.3f))
        )
        return
    }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var isAdLoaded by remember { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        val widthDp = maxWidth.value.toInt()
        val calculatedAdSize = remember(widthDp) {
            getAdaptiveBannerSize(context, widthDp)
        }

        val adView = remember(adUnitId, calculatedAdSize) {
            AdView(context).apply {
                this.adUnitId = adUnitId
                setAdSize(calculatedAdSize)
                adListener = object : AdListener() {
                    override fun onAdLoaded() {
                        super.onAdLoaded()
                        isAdLoaded = true
                        val responseInfo = this@apply.responseInfo
                        val adSource = responseInfo?.loadedAdapterResponseInfo?.adSourceName ?: "AdMob"
                        Log.i("AdMediation", "[BannerAd] Successfully loaded from ad source: '$adSource'")
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        super.onAdFailedToLoad(error)
                        isAdLoaded = false
                        Log.w("AdMediation", "[BannerAd] Failed to load [Code ${error.code}]: ${error.message}")
                    }
                }
                loadAd(AdRequest.Builder().build())
            }
        }

        DisposableEffect(adView, lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_PAUSE -> adView.pause()
                    Lifecycle.Event.ON_RESUME -> adView.resume()
                    Lifecycle.Event.ON_DESTROY -> adView.destroy()
                    else -> {}
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
                adView.destroy()
            }
        }

        AnimatedVisibility(
            visible = isAdLoaded,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            AndroidView(
                factory = { adView },
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
            )
        }
    }
}

/**
 * Calculates the anchored adaptive banner size based on device width in dp.
 */
private fun getAdaptiveBannerSize(context: Context, widthDp: Int): AdSize {
    val usableWidth = if (widthDp > 0) widthDp else 320
    return try {
        AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, usableWidth)
    } catch (e: Throwable) {
        Log.d("AdMediation", "Adaptive banner calculation fallback: ${e.message}")
        AdSize.BANNER
    }
}
