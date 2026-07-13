package com.quickcleanpro.phonecleaner.use.skin.permission

import android.content.ActivityNotFoundException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.quickcleanpro.phonecleaner.app.permission.AndroidPermissionGateway
import com.quickcleanpro.phonecleaner.use.core.permission.PermissionPromptRequest
import com.quickcleanpro.phonecleaner.use.core.common.platform.ExternalActivityLaunchHandler

@Composable
internal fun CleanXPermissionPromptHost(
    state: CleanXPermissionCoordinatorState,
    externalActivityLaunchHandler: ExternalActivityLaunchHandler,
    permissionPrompt: @Composable (
        request: PermissionPromptRequest,
        onSubmit: () -> Unit,
        onDismiss: () -> Unit,
    ) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val permissionGateway = remember(context) { AndroidPermissionGateway(context) }
    val settingsReturnGeneration = remember { mutableStateOf(0) }
    val runtimeLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions(),
        ) { grants ->
            val target = state.session?.target ?: return@rememberLauncherForActivityResult
            state.onRuntimeResult(target, grants)
        }
    val settingsLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
        ) {
            externalActivityLaunchHandler.markReturn()
            settingsReturnGeneration.value += 1
        }

    DisposableEffect(lifecycleOwner, state) {
        val observer =
            LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_PAUSE,
                    Lifecycle.Event.ON_STOP,
                    -> state.markSettingsLaunchObservedPause()
                    Lifecycle.Event.ON_RESUME -> {
                        externalActivityLaunchHandler.markReturn()
                        settingsReturnGeneration.value += 1
                    }
                    else -> Unit
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(state.pendingLaunch) {
        when (val launch = state.consumePendingLaunch()) {
            is PermissionLaunch.Runtime -> runtimeLauncher.launch(launch.permissions)
            is PermissionLaunch.Settings -> {
                var launched = false
                val settingsIntents = permissionGateway.settingsIntents(launch.permission)
                for (intent in settingsIntents) {
                    try {
                        externalActivityLaunchHandler.markLaunch()
                        state.markSettingsLaunchPending(launch.target)
                        settingsLauncher.launch(intent)
                        launched = true
                        break
                    } catch (_: ActivityNotFoundException) {
                        externalActivityLaunchHandler.cancelLaunch()
                    } catch (_: Exception) {
                        externalActivityLaunchHandler.cancelLaunch()
                    }
                }
                if (!launched) {
                    state.dismissUnavailable()
                }
            }
            null -> Unit
        }
    }

    LaunchedEffect(settingsReturnGeneration.value) {
        state.onSettingsReturnIfReady()
    }

    val session = state.session
    if (session?.showDialog == true) {
        permissionPrompt(
            PermissionPromptRequest(
                target = session.target,
                missingPermission = session.missingPermission,
            ),
            state::onDialogSubmit,
            state::dismiss,
        )
    }
}
