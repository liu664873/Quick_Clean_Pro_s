package com.quickcleanpro.phonecleaner.common.permission

enum class PermissionType(
    val key: String,
) {
    StorageFiles("storage_files"),
    Location("location"),
    UsageAccess("usage_access"),
    NotificationListener("notification_listener"),
    Overlay("overlay"),
    ;

    companion object {
        fun fromKey(key: String): PermissionType? =
            entries.firstOrNull { it.key == key }
    }
}
