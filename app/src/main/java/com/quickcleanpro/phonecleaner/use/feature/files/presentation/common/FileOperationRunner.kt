package com.quickcleanpro.phonecleaner.use.feature.files.presentation.common

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class FileOperationRunner(
    private val scope: CoroutineScope,
    private val dispatcher: CoroutineDispatcher,
    private val testLoader: (((suspend () -> Unit)) -> Unit)? = null,
) {
    private var activeOperationJob: Job? = null

    fun launch(block: suspend () -> Unit) {
        cancelActiveOperation()
        val loader = testLoader
        if (loader != null) {
            loader(block)
            return
        }
        activeOperationJob = scope.launch(dispatcher) {
            try {
                block()
            } finally {
                if (activeOperationJob == coroutineContext[Job]) {
                    activeOperationJob = null
                }
            }
        }
    }

    suspend fun delayIfNeeded(millis: Long) {
        if (millis > 0L) delay(millis)
    }

    fun cancelActiveOperation() {
        activeOperationJob?.cancel()
        activeOperationJob = null
    }
}
