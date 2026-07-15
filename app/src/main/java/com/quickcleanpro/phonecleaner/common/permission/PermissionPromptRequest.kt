package com.quickcleanpro.phonecleaner.common.permission

data class PermissionPromptRequest(
    val target: PermissionTarget,
    val missingPermission: PermissionType?,
)
