package com.example.ads

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.util.DeviceUtils
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView

private const val TAG = "NativeAdCard"

@Composable
fun NativeAdCard(
    modifier: Modifier = Modifier,
    cardBg: Color = MaterialTheme.colorScheme.surfaceVariant,
    borderColor: Color = MaterialTheme.colorScheme.outlineVariant,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    textColorPrimary: Color = MaterialTheme.colorScheme.onSurface,
    textColorSecondary: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    headlineOverride: String? = null,
    testTag: String = "native_ad_card"
) {
    val context = LocalContext.current

    if (DeviceUtils.isEmulator()) {
        SimulatedNativeAdCard(
            modifier = modifier.testTag(testTag),
            cardBg = cardBg,
            borderColor = borderColor,
            accentColor = accentColor,
            textColorPrimary = textColorPrimary,
            textColorSecondary = textColorSecondary,
            headline = headlineOverride ?: "Color Block Master: Top Puzzle"
        )
        return
    }

    var nativeAd by remember { mutableStateOf<NativeAd?>(null) }
    var adFailed by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val adLoader = AdLoader.Builder(context, AdConfig.nativeAdUnitId)
            .forNativeAd { ad ->
                nativeAd = ad
                adFailed = false
                Log.i(TAG, "Native ad loaded successfully: ${ad.headline}")
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.w(TAG, "Native ad failed to load [Code ${loadAdError.code}]: ${loadAdError.message}")
                    adFailed = true

                    if (loadAdError.code == AdRequest.ERROR_CODE_NO_FILL &&
                        AdConfig.isDebugMode &&
                        AdConfig.nativeAdUnitId != AdConfig.TEST_NATIVE_AD_ID
                    ) {
                        Log.i(TAG, "Falling back to Google Test Native Ad Unit in debug mode...")
                        AdLoader.Builder(context, AdConfig.TEST_NATIVE_AD_ID)
                            .forNativeAd { fallbackAd ->
                                nativeAd = fallbackAd
                                adFailed = false
                            }
                            .build()
                            .loadAd(AdRequest.Builder().build())
                    }
                }
            })
            .withNativeAdOptions(
                NativeAdOptions.Builder()
                    .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_RIGHT)
                    .build()
            )
            .build()

        adLoader.loadAd(AdRequest.Builder().build())

        onDispose {
            nativeAd?.destroy()
        }
    }

    if (nativeAd == null) {
        if (!adFailed) {
            SimulatedNativeAdCard(
                modifier = modifier.testTag(testTag),
                cardBg = cardBg,
                borderColor = borderColor,
                accentColor = accentColor,
                textColorPrimary = textColorPrimary,
                textColorSecondary = textColorSecondary,
                headline = headlineOverride ?: "Sponsored Block Game"
            )
        }
        return
    }

    val currentAd = nativeAd ?: return
    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .testTag(testTag),
        factory = { ctx ->
            createNativeAdView(
                context = ctx,
                ad = currentAd,
                cardBg = cardBg,
                borderColor = borderColor,
                accentColor = accentColor,
                textColorPrimary = textColorPrimary,
                textColorSecondary = textColorSecondary
            )
        },
        update = { view ->
            populateNativeAdView(
                nativeAdView = view,
                ad = currentAd,
                accentColor = accentColor,
                textColorPrimary = textColorPrimary,
                textColorSecondary = textColorSecondary
            )
        }
    )
}

private fun createNativeAdView(
    context: Context,
    ad: NativeAd,
    cardBg: Color,
    borderColor: Color,
    accentColor: Color,
    textColorPrimary: Color,
    textColorSecondary: Color
): NativeAdView {
    val density = context.resources.displayMetrics.density
    fun dp(value: Int): Int = (value * density).toInt()

    val nativeAdView = NativeAdView(context)
    nativeAdView.layoutParams = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
    )

    val rootLayout = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(14), dp(12), dp(14), dp(12))

        val bgDrawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(16).toFloat()
            setColor(cardBg.toArgb())
            setStroke(dp(1), borderColor.toArgb())
        }
        background = bgDrawable
    }

    val headerLayout = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
    }

    val iconView = ImageView(context).apply {
        layoutParams = LinearLayout.LayoutParams(dp(38), dp(38)).apply {
            marginEnd = dp(10)
        }
        scaleType = ImageView.ScaleType.CENTER_CROP
        val iconDrawable = GradientDrawable().apply {
            cornerRadius = dp(10).toFloat()
        }
        background = iconDrawable
        clipToOutline = true
    }
    headerLayout.addView(iconView)
    nativeAdView.iconView = iconView

    val textCol = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f)
    }

    val headlineRow = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
    }

    val adBadge = TextView(context).apply {
        text = "Ad"
        textSize = 9f
        setTypeface(null, Typeface.BOLD)
        setTextColor(android.graphics.Color.WHITE)
        setPadding(dp(4), dp(1), dp(4), dp(1))
        val badgeBg = GradientDrawable().apply {
            cornerRadius = dp(4).toFloat()
            setColor(android.graphics.Color.parseColor("#F59E0B"))
        }
        background = badgeBg
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            marginEnd = dp(6)
        }
    }
    headlineRow.addView(adBadge)

    val headlineView = TextView(context).apply {
        textSize = 13.5f
        setTypeface(null, Typeface.BOLD)
        setTextColor(textColorPrimary.toArgb())
        maxLines = 1
        ellipsize = android.text.TextUtils.TruncateAt.END
    }
    headlineRow.addView(headlineView)
    nativeAdView.headlineView = headlineView
    textCol.addView(headlineRow)

    val bodyView = TextView(context).apply {
        textSize = 11.5f
        setTextColor(textColorSecondary.toArgb())
        maxLines = 1
        ellipsize = android.text.TextUtils.TruncateAt.END
        setPadding(0, dp(2), 0, 0)
    }
    textCol.addView(bodyView)
    nativeAdView.bodyView = bodyView

    headerLayout.addView(textCol)

    val ctaButton = Button(context).apply {
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            dp(36)
        ).apply {
            marginStart = dp(8)
        }
        textSize = 12f
        setTypeface(null, Typeface.BOLD)
        setTextColor(android.graphics.Color.WHITE)
        setPadding(dp(14), 0, dp(14), 0)
        val btnBg = GradientDrawable().apply {
            cornerRadius = dp(12).toFloat()
            setColor(accentColor.toArgb())
        }
        background = btnBg
    }
    headerLayout.addView(ctaButton)
    nativeAdView.callToActionView = ctaButton

    rootLayout.addView(headerLayout)
    nativeAdView.addView(rootLayout)

    populateNativeAdView(nativeAdView, ad, accentColor, textColorPrimary, textColorSecondary)
    return nativeAdView
}

private fun populateNativeAdView(
    nativeAdView: NativeAdView,
    ad: NativeAd,
    accentColor: Color,
    textColorPrimary: Color,
    textColorSecondary: Color
) {
    (nativeAdView.headlineView as? TextView)?.apply {
        text = ad.headline ?: "Recommended Game"
        setTextColor(textColorPrimary.toArgb())
    }

    (nativeAdView.bodyView as? TextView)?.apply {
        text = ad.body ?: ad.advertiser ?: "Play and challenge your friends!"
        setTextColor(textColorSecondary.toArgb())
    }

    (nativeAdView.callToActionView as? Button)?.apply {
        text = ad.callToAction?.uppercase() ?: "PLAY"
    }

    (nativeAdView.iconView as? ImageView)?.apply {
        val icon = ad.icon
        if (icon != null) {
            setImageDrawable(icon.drawable)
            visibility = View.VISIBLE
        } else {
            visibility = View.GONE
        }
    }

    nativeAdView.setNativeAd(ad)
}

@Composable
fun SimulatedNativeAdCard(
    modifier: Modifier = Modifier,
    cardBg: Color,
    borderColor: Color,
    accentColor: Color,
    textColorPrimary: Color,
    textColorSecondary: Color,
    headline: String = "Color Block Master: Top Puzzle"
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(1.dp, borderColor.copy(alpha = 0.6f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LocalFireDepartment,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFFF59E0B)
                    ) {
                        Text(
                            text = "Ad",
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            fontSize = 9.sp,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = headline,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = textColorPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    repeat(5) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFB300),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "4.9 ★ • Offline Puzzle",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = textColorSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = accentColor,
                shadowElevation = 2.dp
            ) {
                Text(
                    text = "PLAY",
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}
