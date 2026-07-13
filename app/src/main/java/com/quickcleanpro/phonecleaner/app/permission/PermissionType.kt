package com.quickcleanpro.phonecleaner.app.permission

import com.quickcleanpro.phonecleaner.use.core.permission.PermissionFeature

enum class PermissionType(
    override val key: String,
) : PermissionFeature {
    StorageFiles("storage_files"),
    MediaImages("media_images"),
    MediaImagesWithLocation("media_images_with_location"),
    MediaVideo("media_video"),
    MediaAudio("media_audio"),
    Location("location"),
    UsageAccess("usage_access"),
    NotificationListener("notification_listener"),
    Overlay("overlay"),
    PostNotifications("post_notifications"),
    ;

    companion object {
        fun fromKey(key: String): PermissionType? =
            entries.firstOrNull { it.key == key }
    }
}
