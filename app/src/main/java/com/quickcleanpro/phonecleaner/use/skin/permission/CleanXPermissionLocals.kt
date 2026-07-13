package com.quickcleanpro.phonecleaner.use.skin.permission

import com.quickcleanpro.phonecleaner.app.permission.PermissionType

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.quickcleanpro.phonecleaner.use.core.permission.CleanXPermissionRegistry
import com.quickcleanpro.phonecleaner.use.core.permission.CleanXProtectedAction

@Composable
fun rememberPermissionGranted(action: CleanXProtectedAction): Boolean {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val manager =
        remember(context) {
            CleanXPermissionRegistry.protectedActionPermissionManager(context)
        }

    fun checkGranted(): Boolean =
        manager.status(context, action).granted

    var granted by remember(action, manager) { mutableStateOf(checkGranted()) }

    DisposableEffect(lifecycleOwner, action, manager) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    granted = checkGranted()
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    return granted
}

@Composable
fun rememberPermissionGranted(item: PermissionType): Boolean {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val manager =
        remember(context) {
            CleanXPermissionRegistry.permissionItemManager(context)
        }

    fun checkGranted(): Boolean =
        manager.status(context, item).granted

    var granted by remember(item, manager) { mutableStateOf(checkGranted()) }

    DisposableEffect(lifecycleOwner, item, manager) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    granted = checkGranted()
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    return granted
}
