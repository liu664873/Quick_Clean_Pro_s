package com.quickcleanpro.phonecleaner.common.permission

import com.quickcleanpro.phonecleaner.common.permission.PermissionType

data class PermissionPromptRequest(
    val target: PermissionRequestTarget,
    val missingPermission: PermissionType?,
)
