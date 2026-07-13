package com.quickcleanpro.phonecleaner.feature.files.shared.ui

internal sealed interface FileManagerAction {
    data object Back : FileManagerAction
    data object CancelDelete : FileManagerAction
    data object RequestDelete : FileManagerAction
    data object DeleteReady : FileManagerAction
    data object ContinueManaging : FileManagerAction
    data object ToggleVisibleItems : FileManagerAction
    data class SelectTab(val index: Int) : FileManagerAction
    data class ToggleSelection(val id: Int) : FileManagerAction
    data class ToggleVisibleIds(val ids: Set<Int>) : FileManagerAction
    data class ToggleGroupIds(val ids: Set<Int>) : FileManagerAction
    data class OpenDetail(val index: Int) : FileManagerAction
}
