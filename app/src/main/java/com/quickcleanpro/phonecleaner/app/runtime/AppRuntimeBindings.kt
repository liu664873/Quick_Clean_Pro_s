package com.quickcleanpro.phonecleaner.app.runtime

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import com.quickcleanpro.phonecleaner.common.ads.AdRuntime
import com.quickcleanpro.phonecleaner.common.ads.AdRuntimeState
import com.quickcleanpro.phonecleaner.app.runtime.featureflow.DefaultFeatureFlowRuntime
import com.quickcleanpro.phonecleaner.app.runtime.featureflow.FeatureFlowRuntime
import com.quickcleanpro.phonecleaner.app.runtime.external.ExternalActivityLauncher

internal data class AppRuntimeBindings(
    val ads: AdRuntime,
    val featureFlow: FeatureFlowRuntime,
    val externalActivities: ExternalActivityLauncher,
)

@Composable
internal fun rememberAppRuntimeBindings(
    context: Context,
    adRuntimeState: AdRuntimeState,
    sessionCoordinator: AppSessionCoordinator,
): AppRuntimeBindings {
    val activity = context.findActivity()
    val latestAdRuntimeState = rememberUpdatedState(adRuntimeState)
    val adRuntime = remember(activity, sessionCoordinator) {
        AdRuntime(
            activityProvider = { activity },
            stateProvider = { latestAdRuntimeState.value },
            onInterstitialStateChanged = { active ->
                sessionCoordinator.set(RuntimeBusyReason.Interstitial, active)
            },
        )
    }
    val externalActivityLauncher = remember(adRuntime) {
        ExternalActivityLauncher(
            markLaunch = adRuntime::markExternalActivityLaunch,
            cancelLaunch = adRuntime::cancelExternalActivityLaunch,
            markReturn = adRuntime::onHostResumed,
        )
    }
    val featureFlow = remember(adRuntime, sessionCoordinator) {
        DefaultFeatureFlowRuntime(
            interstitialAds = adRuntime,
            onOperationBusyChanged = { active ->
                sessionCoordinator.set(RuntimeBusyReason.FeatureOperation, active)
            },
        )
    }
    return remember(adRuntime, featureFlow, externalActivityLauncher) {
        AppRuntimeBindings(
            ads = adRuntime,
            featureFlow = featureFlow,
            externalActivities = externalActivityLauncher,
        )
    }
}

private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
