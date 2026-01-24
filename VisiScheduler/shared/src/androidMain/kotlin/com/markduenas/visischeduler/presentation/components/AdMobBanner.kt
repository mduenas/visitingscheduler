package com.markduenas.visischeduler.presentation.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.markduenas.visischeduler.config.AdMobConstants
import com.markduenas.visischeduler.data.billing.BillingManager
import org.koin.compose.koinInject

/**
 * AdMob Banner Ad component for Android.
 * Shows a banner ad at the specified location if the user hasn't purchased ad removal.
 */
@Composable
fun AdMobBanner(
    modifier: Modifier = Modifier,
    billingManager: BillingManager = koinInject()
) {
    val hasRemovedAds by billingManager.hasRemovedAds.collectAsState()

    // Don't show ads if user has purchased removal
    if (hasRemovedAds) {
        return
    }

    if (!AdMobConstants.ADMOB_ENABLED) {
        return
    }

    val context = LocalContext.current
    val adUnitId = AdMobConstants.getBannerAdUnitId(isAndroid = true, isDebug = false)

    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .height(AdMobConstants.BANNER_HEIGHT_DP.dp),
        factory = { ctx ->
            AdView(ctx).apply {
                setAdSize(AdSize.BANNER)
                adUnitId = adUnitId
                loadAd(AdRequest.Builder().build())
            }
        },
        update = { adView ->
            // Reload ad if needed
        }
    )
}

/**
 * AdMob Banner Ad that is always visible (ignores purchase state).
 * Use for testing or when you want the banner to always show.
 */
@Composable
fun AdMobBannerAlwaysVisible(
    modifier: Modifier = Modifier,
    useTestAds: Boolean = false
) {
    if (!AdMobConstants.ADMOB_ENABLED) {
        return
    }

    val context = LocalContext.current
    val adUnitId = AdMobConstants.getBannerAdUnitId(isAndroid = true, isDebug = useTestAds)

    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .height(AdMobConstants.BANNER_HEIGHT_DP.dp),
        factory = { ctx ->
            AdView(ctx).apply {
                setAdSize(AdSize.BANNER)
                this.adUnitId = adUnitId
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}
