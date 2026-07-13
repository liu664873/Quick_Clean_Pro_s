package com.quickcleanpro.phonecleaner.common.permission.ui

import androidx.compose.runtime.staticCompositionLocalOf
import com.quickcleanpro.phonecleaner.common.permission.CleanXPermissionCoordinator

val LocalPermissionCoordinator =
    staticCompositionLocalOf<CleanXPermissionCoordinator> { error("Permission coordinator is not available") }
