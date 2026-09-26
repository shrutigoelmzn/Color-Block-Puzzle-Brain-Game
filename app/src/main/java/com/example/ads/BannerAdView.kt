package com.example.ads

import android.content.Context
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

private const val TAG = "BannerAdView"

/**
 * Production-ready Compose Banner Ad component that:
 * 1. Remains completely invisible (0 height, no card borders/backgrounds) until an ad actually loads.
 * 2. Waits for MobileAds initialization and window attachment before requesting ads to avoid Binder -ENOSPC.
 * 3. Properly manages Android View lifecycle, window attachment, and destruction.
 * 4. Adapts dynamically to device width using Google Mobile Ads Anchored Adaptive Banners.
 */
@Composable
fun BannerAdView(
    modifier: Modifier = Modifier,
    adUnitId: String = AdConfig.bannerAdUnitId,
    backgroundColor: Color = Color.Transparent,
    borderColor: Color = Color.Transparent,
    borderWidth: Dp = 0.5.dp,
    shape: Shape = RoundedCornerShape(16.dp),
    shadowElevation: Dp = 0.dp,
    contentPadding: PaddingValues = PaddingValues(vertical = 4.dp),
    topSpacing: Dp = 0.dp,
    bottomSpacing: Dp = 0.dp
) {
    if (LocalInspectionMode.current || com.example.util.DeviceUtils.isEmulator()) {
        return
    }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val isMobileAdsReady by AdManager.isMobileAdsReady.collectAsStateWithLifecycle()

    var isAdLoaded by remember { mutableStateOf(false) }
    var adViewRef by remember { mutableStateOf<AdView?>(null) }
    var hasRequestedLoad by remember { mutableStateOf(false) }

    // Forward lifecycle events to AdView
    DisposableEffect(lifecycleOwner, adViewRef) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    try {
                        adViewRef?.pause()
                    } catch (e: Throwable) {
                        Log.d(TAG, "AdView pause note: ${e.message}")
                    }
                }
                Lifecycle.Event.ON_RESUME -> {
                    try {
                        adViewRef?.resume()
                    } catch (e: Throwable) {
                        Log.d(TAG, "AdView resume note: ${e.message}")
                    }
                }
                Lifecycle.Event.ON_DESTROY -> {
                    try {
                        adViewRef?.destroy()
                        adViewRef = null
                    } catch (e: Throwable) {
                        Log.d(TAG, "AdView destroy note: ${e.message}")
                    }
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            try {
                adViewRef?.destroy()
                adViewRef = null
            } catch (e: Throwable) {
                // Ignored
            }
        }
    }

    // Trigger load only when MobileAds is initialized and AdView is attached to the window
    LaunchedEffect(isMobileAdsReady, adViewRef, hasRequestedLoad) {
        val currentAdView = adViewRef
        if (isMobileAdsReady && currentAdView != null && !hasRequestedLoad) {
            hasRequestedLoad = true
            if (currentAdView.isAttachedToWindow) {
                try {
                    currentAdView.loadAd(AdRequest.Builder().build())
                } catch (e: Throwable) {
                    Log.w(TAG, "loadAd error: ${e.message}")
                }
            } else {
                currentAdView.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
                    override fun onViewAttachedToWindow(v: View) {
                        currentAdView.removeOnAttachStateChangeListener(this)
                        try {
                            currentAdView.loadAd(AdRequest.Builder().build())
                        } catch (e: Throwable) {
                            Log.w(TAG, "loadAd error: ${e.message}")
                        }
                    }
                    override fun onViewDetachedFromWindow(v: View) {}
                })
            }
        }
    }

    BoxWithConstraints(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        val widthDp = if (maxWidth.value.toInt() > 0) maxWidth.value.toInt() else 320
        val adSize = remember(widthDp) {
            getAdaptiveBannerSize(context, widthDp)
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AnimatedVisibility(
                visible = isAdLoaded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                if (topSpacing > 0.dp) {
                    Spacer(modifier = Modifier.height(topSpacing))
                }
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (isAdLoaded) Modifier.wrapContentHeight()
                        else Modifier.height(0.dp)
                    ),
                shape = shape,
                color = if (isAdLoaded) backgroundColor else Color.Transparent,
                border = if (isAdLoaded && borderColor != Color.Transparent) BorderStroke(borderWidth, borderColor) else null,
                shadowElevation = if (isAdLoaded) shadowElevation else 0.dp
            ) {
                AndroidView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (isAdLoaded) Modifier.padding(contentPadding).wrapContentHeight()
                            else Modifier.height(0.dp)
                        ),
                    factory = { ctx ->
                        // Host container attached to window
                        FrameLayout(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT
                            )
                            val adView = AdView(ctx).apply {
                                this.adUnitId = adUnitId
                                setAdSize(adSize)
                                visibility = View.GONE
                                try {
                                    setLayerType(View.LAYER_TYPE_SOFTWARE, null)
                                } catch (e: Throwable) {
                                    // Ignored if unsupported
                                }

                                this.adListener = object : AdListener() {
                                    override fun onAdLoaded() {
                                        super.onAdLoaded()
                                        isAdLoaded = true
                                        visibility = View.VISIBLE
                                        Log.i(TAG, "Banner ad loaded successfully")
                                    }

                                    override fun onAdFailedToLoad(error: LoadAdError) {
                                        super.onAdFailedToLoad(error)
                                        isAdLoaded = false
                                        visibility = View.GONE
                                        Log.w(TAG, "Banner ad failed to load [Code ${error.code}]: ${error.message}")
                                    }
                                }
                            }
                            addView(adView)
                            adViewRef = adView
                        }
                    },
                    update = { container ->
                        val childAdView = if (container.childCount > 0) container.getChildAt(0) as? AdView else null
                        childAdView?.visibility = if (isAdLoaded) View.VISIBLE else View.GONE
                    },
                    onRelease = { container ->
                        try {
                            if (container.childCount > 0) {
                                (container.getChildAt(0) as? AdView)?.destroy()
                            }
                            container.removeAllViews()
                        } catch (e: Throwable) {
                            Log.d(TAG, "onRelease cleanup note: ${e.message}")
                        }
                        adViewRef = null
                    }
                )
            }

            AnimatedVisibility(
                visible = isAdLoaded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                if (bottomSpacing > 0.dp) {
                    Spacer(modifier = Modifier.height(bottomSpacing))
                }
            }
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
        Log.d(TAG, "Adaptive banner calculation fallback: ${e.message}")
        AdSize.BANNER
    }
}
