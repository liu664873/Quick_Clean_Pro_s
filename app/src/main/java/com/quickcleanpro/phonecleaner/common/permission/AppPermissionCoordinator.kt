package com.quickcleanpro.phonecleaner.common.permission

interface AppPermissionCoordinator {
    fun isGranted(permission: PermissionType): Boolean

    fun ensure(
        action: ProtectedAction,
        mode: PermissionPromptMode = PermissionPromptMode.Explained,
        onDenied: () -> Unit = {},
        onGranted: () -> Unit,
    )

    fun ensure(
        permission: PermissionType,
        mode: PermissionPromptMode = PermissionPromptMode.Explained,
        onDenied: () -> Unit = {},
        onGranted: () -> Unit,
    )

    fun openSettings(
        permission: PermissionType,
        onReturn: () -> Unit = {},
    )
}
