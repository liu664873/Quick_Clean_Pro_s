package com.quickcleanpro.phonecleaner.use.skin.files.common.detail

internal fun fileDetailTitle(
    selectedCount: Int,
    totalCount: Int,
): String = "${selectedCount.coerceAtLeast(0)}/${totalCount.coerceAtLeast(0)}"
