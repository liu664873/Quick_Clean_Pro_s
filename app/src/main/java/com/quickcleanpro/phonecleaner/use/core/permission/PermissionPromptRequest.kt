package com.quickcleanpro.phonecleaner.use.core.permission

import com.quickcleanpro.phonecleaner.app.permission.PermissionType

data class PermissionPromptRequest(
    val target: PermissionRequestTarget,
    val missingPermission: PermissionType?,
)
