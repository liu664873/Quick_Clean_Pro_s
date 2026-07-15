package com.quickcleanpro.phonecleaner.common.permission.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.quickcleanpro.phonecleaner.common.permission.PermissionType

@Composable
fun rememberPermissionGranted(permission: PermissionType): Boolean {
    val lifecycleOwner = LocalLifecycleOwner.current
    val coordinator = LocalPermissionCoordinator.current
    fun checkGranted(): Boolean = coordinator.isGranted(permission)
    var granted by remember(permission, coordinator) { mutableStateOf(checkGranted()) }

    DisposableEffect(lifecycleOwner, permission, coordinator) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) granted = checkGranted()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    return granted
}
