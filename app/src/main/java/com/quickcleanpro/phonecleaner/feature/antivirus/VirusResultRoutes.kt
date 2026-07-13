package com.quickcleanpro.phonecleaner.feature.antivirus

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quickcleanpro.phonecleaner.R
import com.quickcleanpro.phonecleaner.app.navigation.AppDestination
import com.quickcleanpro.phonecleaner.app.navigation.AppNavigator
import com.quickcleanpro.phonecleaner.app.navigation.feature.FeatureKey
import com.quickcleanpro.phonecleaner.app.runtime.external.ExternalActivityLauncher
import com.quickcleanpro.phonecleaner.app.runtime.featureflow.FeatureExitReason
import com.quickcleanpro.phonecleaner.app.runtime.featureflow.FeatureFlowRuntime
import com.quickcleanpro.phonecleaner.feature.antivirus.ui.NoVirusResultScreen
import com.quickcleanpro.phonecleaner.feature.antivirus.ui.ScanVirusResultScreen
import java.io.File

sealed interface VirusResultAction {
    data object Back : VirusResultAction
    data object SolveAdbRisk : VirusResultAction
    data class SolveAppThreat(val packageName: String) : VirusResultAction
    data class DeleteFileThreat(val path: String) : VirusResultAction
}

@Composable
fun ScanVirusResultRoute(
    navigator: AppNavigator,
    viewModel: VirusScanViewModel,
    featureFlow: FeatureFlowRuntime,
    externalActivities: ExternalActivityLauncher,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val deletionFailedText = stringResource(R.string.deletion_failed)

    fun exit() {
        featureFlow.exit(FeatureKey.ANTI_VIRUS, FeatureExitReason.Return) { navigator.back() }
    }

    LaunchedEffect(Unit) { viewModel.refreshAdbRisk() }

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshAdbRisk()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(state.effectiveThreatCount) {
        if (state.effectiveThreatCount == 0) navigator.replace(AppDestination.NoVirusResult)
    }

    DisposableEffect(context, viewModel) {
        val receiver = PackageRemovedReceiver(viewModel::removeThreatByPackage)
        val filter = IntentFilter(Intent.ACTION_PACKAGE_REMOVED).apply { addDataScheme("package") }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            context.registerReceiver(receiver, filter)
        }
        onDispose { runCatching { context.unregisterReceiver(receiver) } }
    }

    ScanVirusResultScreen(
        state = state,
        onAction = { action ->
            when (action) {
                VirusResultAction.Back -> exit()
                VirusResultAction.SolveAdbRisk -> openDeveloperSettings(context, externalActivities)
                is VirusResultAction.SolveAppThreat ->
                    openAppSettings(context, action.packageName, externalActivities)
                is VirusResultAction.DeleteFileThreat -> {
                    if (File(action.path).safeDelete(context)) {
                        viewModel.removeThreatByFilePath(action.path)
                    } else {
                        Toast.makeText(context, deletionFailedText, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        },
    )
}

@Composable
fun NoVirusResultRoute(
    navigator: AppNavigator,
    featureFlow: FeatureFlowRuntime,
) {
    NoVirusResultScreen(
        onAction = { action ->
            if (action == VirusResultAction.Back) {
                featureFlow.exit(FeatureKey.ANTI_VIRUS, FeatureExitReason.Return) { navigator.back() }
            }
        },
        onNavigate = navigator::resetTo,
    )
}
