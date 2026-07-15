package com.quickcleanpro.phonecleaner.common.ui.components.popups

import androidx.annotation.StringRes
import com.quickcleanpro.phonecleaner.R

data class PermissionCopy(
    @param:StringRes val titleRes: Int,
    @param:StringRes val descriptionRes: Int,
    @param:StringRes val hint1Res: Int,
    @param:StringRes val hint2Res: Int,
    @param:StringRes val allowRes: Int = R.string.allow,
    @param:StringRes val cancelRes: Int = R.string.cancel,
)
