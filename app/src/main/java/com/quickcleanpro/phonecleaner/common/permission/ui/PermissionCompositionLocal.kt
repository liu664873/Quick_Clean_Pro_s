package com.quickcleanpro.phonecleaner.common.permission.ui

import androidx.compose.runtime.staticCompositionLocalOf
import com.quickcleanpro.phonecleaner.common.permission.AppPermissionCoordinator

val LocalPermissionCoordinator =
    staticCompositionLocalOf<AppPermissionCoordinator> { error("Permission coordinator is not available") }
