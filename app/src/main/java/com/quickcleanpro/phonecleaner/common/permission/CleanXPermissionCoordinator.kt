package com.quickcleanpro.phonecleaner.common.permission

sealed interface PermissionRequestTarget {
    val key: String

    data class Action(
        val action: CleanXProtectedAction,
    ) : PermissionRequestTarget {
        override val key: String = "action:${action.key}"
    }

    data class Item(
        val item: PermissionType,
    ) : PermissionRequestTarget {
        override val key: String = "item:${item.key}"
    }
}

interface CleanXPermissionCoordinator {
    fun isGranted(action: CleanXProtectedAction): Boolean

    fun isGranted(item: PermissionType): Boolean

    fun guard(
        action: CleanXProtectedAction,
        onRejected: () -> Unit = {},
        onGranted: () -> Unit,
    )

    fun guardDirect(
        action: CleanXProtectedAction,
        onRejected: () -> Unit = {},
        onGranted: () -> Unit,
    )

    fun request(
        item: PermissionType,
        onRejected: () -> Unit = {},
        onGranted: () -> Unit = {},
    )

    fun openSettings(
        item: PermissionType,
        onRejected: () -> Unit = {},
        onGranted: () -> Unit = {},
    )
}
