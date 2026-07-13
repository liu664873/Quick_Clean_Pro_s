package com.quickcleanpro.phonecleaner.use.feature.files.presentation.common

internal object FileManagerReducer {
    fun reduce(
        state: FileManagerState,
        action: FileManagerAction,
    ): FileManagerTransition = when (action) {
        FileManagerAction.ScanStarted -> transition(
            state.copy(
                phase = FileManagerPhase.Scanning,
                selectedKeys = emptySet(),
                detailIndex = null,
                result = null,
                errorMessage = null,
            ),
        )

        is FileManagerAction.ScanFinished -> transition(
            state.copy(
                phase = if (action.hasResults) FileManagerPhase.Browsing else FileManagerPhase.NoResults,
                errorMessage = null,
            ),
        )

        is FileManagerAction.ScanFailed -> transition(
            state.copy(
                phase = FileManagerPhase.NoResults,
                errorMessage = action.message,
            ),
        )

        is FileManagerAction.SetSelection -> transition(state.copy(selectedKeys = action.keys))

        is FileManagerAction.ToggleSelection -> transition(
            state.copy(
                selectedKeys = if (action.key in state.selectedKeys) {
                    state.selectedKeys - action.key
                } else {
                    state.selectedKeys + action.key
                },
            ),
        )

        is FileManagerAction.OpenDetail -> transition(state.copy(detailIndex = action.index.coerceAtLeast(0)))
        FileManagerAction.CloseDetail -> transition(state.copy(detailIndex = null))

        is FileManagerAction.RequestOperation -> transition(
            state.copy(phase = FileManagerPhase.ConfirmDelete),
            FileManagerEffect.ConfirmOperation(action.action),
        )

        FileManagerAction.CancelOperation -> transition(state.copy(phase = FileManagerPhase.Browsing))

        is FileManagerAction.OperationStarted -> transition(
            state.copy(
                phase = FileManagerPhase.Deleting,
                detailIndex = null,
                errorMessage = null,
            ),
        )

        is FileManagerAction.OperationAnimationFinished -> transition(
            state.copy(
                phase = FileManagerPhase.CompleteAnimation,
                selectedKeys = emptySet(),
                result = action.result,
            ),
        )

        is FileManagerAction.OperationFinished -> {
            val effects = if (action.success) {
                listOf(FileManagerEffect.OperationCompleted(action.action))
            } else {
                emptyList()
            }
            FileManagerTransition(state = state, effects = effects)
        }

        is FileManagerAction.OperationFailed -> transition(
            state.copy(
                phase = FileManagerPhase.Browsing,
                errorMessage = action.message,
            ),
        )

        FileManagerAction.CompletionAdDismissed -> transition(
            state.copy(phase = FileManagerPhase.Result),
            FileManagerEffect.ResultShown,
        )

        FileManagerAction.ContinueManaging -> transition(
            state.copy(
                phase = FileManagerPhase.Browsing,
                selectedKeys = emptySet(),
                detailIndex = null,
            ),
        )

        FileManagerAction.ClearError -> transition(state.copy(errorMessage = null))

        FileManagerAction.ActiveOperationCancelled -> transition(
            if (state.phase == FileManagerPhase.Deleting) {
                state.copy(phase = FileManagerPhase.Browsing)
            } else {
                state
            },
        )
    }

    private fun transition(
        state: FileManagerState,
        vararg effects: FileManagerEffect,
    ): FileManagerTransition = FileManagerTransition(state, effects.toList())
}
