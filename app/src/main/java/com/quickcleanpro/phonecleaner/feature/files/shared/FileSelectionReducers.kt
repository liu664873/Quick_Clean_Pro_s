package com.quickcleanpro.phonecleaner.feature.files.shared

import com.quickcleanpro.phonecleaner.feature.files.shared.*

import com.quickcleanpro.phonecleaner.feature.files.shared.*

internal fun <T> toggleId(selectedIds: Set<T>, id: T): Set<T> =
    if (id in selectedIds) selectedIds - id else selectedIds + id

internal fun <T> toggleIds(selectedIds: Set<T>, ids: Set<T>): Set<T> =
    if (selectedIds.containsAll(ids)) selectedIds - ids else selectedIds + ids

internal fun <T> toggleAllVisible(selectedIds: Set<T>, visibleIds: Set<T>): Set<T> =
    toggleIds(selectedIds, visibleIds)

internal fun openDetailIndex(index: Int?): Int? =
    index?.takeIf { it >= 0 }
