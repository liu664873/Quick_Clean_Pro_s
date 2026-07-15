package com.quickcleanpro.phonecleaner.app.runtime.permission

import android.content.ActivityNotFoundException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.quickcleanpro.phonecleaner.app.runtime.external.ExternalActivityLauncher
import com.quickcleanpro.phonecleaner.common.permission.PermissionPromptRequest

@Composable
internal fun PermissionPromptHost(
    state: PermissionCoordinator,
    externalActivityLauncher: ExternalActivityLauncher,
    permissionPrompt: @Composable (
        request: PermissionPromptRequest,
        onSubmit: () -> Unit,
        onDismiss: () -> Unit,
    ) -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val settingsReturnGeneration = remember { mutableStateOf(0) }
    val runtimeLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
            state.onRuntimeResult(grants)
        }
    val settingsLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            externalActivityLauncher.markReturn()
            settingsReturnGeneration.value += 1
        }

    DisposableEffect(lifecycleOwner, state) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE,
                Lifecycle.Event.ON_STOP,
                -> state.markSettingsLaunchObservedPause()
                Lifecycle.Event.ON_RESUME -> {
                    externalActivityLauncher.markReturn()
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
                for (intent in launch.intents) {
                    try {
                        externalActivityLauncher.markLaunch()
                        settingsLauncher.launch(intent)
                        launched = true
                        break
                    } catch (_: ActivityNotFoundException) {
                        externalActivityLauncher.cancelLaunch()
                    } catch (_: Exception) {
                        externalActivityLauncher.cancelLaunch()
                    }
                }
                if (!launched) state.dismissUnavailable()
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
            PermissionPromptRequest(session.target, session.missingPermission),
            state::onDialogSubmit,
            state::dismiss,
        )
    }
}
