package com.quickcleanpro.phonecleaner.common.permission.ui

import com.quickcleanpro.phonecleaner.common.permission.PermissionType

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
import com.quickcleanpro.phonecleaner.common.permission.CleanXPermissionRegistry
import com.quickcleanpro.phonecleaner.common.permission.PermissionPreferences
import org.koin.compose.koinInject

@Composable
fun rememberPermissionGranted(item: PermissionType): Boolean {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val permissionPreferences = koinInject<PermissionPreferences>()
    val manager =
        remember(permissionPreferences) {
            CleanXPermissionRegistry.permissionItemManager(permissionPreferences)
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
