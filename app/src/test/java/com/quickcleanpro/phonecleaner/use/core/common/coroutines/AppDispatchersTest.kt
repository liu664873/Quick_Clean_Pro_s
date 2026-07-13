package com.quickcleanpro.phonecleaner.common.coroutines

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.fail
import org.junit.Test

class AppDispatchersTest {
    @Test
    fun `provided dispatchers remain independently addressable`() {
        val io = StandardTestDispatcher()
        val default = StandardTestDispatcher()
        val main = StandardTestDispatcher()

        val dispatchers = AppDispatchers(io = io, default = default, main = main)

        assertSame(io, dispatchers.io)
        assertSame(default, dispatchers.default)
        assertSame(main, dispatchers.main)
    }

    @Test
    fun `run suspend catching returns ordinary failures`() = runTest {
        val failure = IllegalStateException("read failed")

        val result = runSuspendCatching<Unit> { throw failure }

        assertSame(failure, result.exceptionOrNull())
    }

    @Test
    fun `run suspend catching never converts cancellation into failure`() = runTest {
        try {
            runSuspendCatching<Unit> { throw CancellationException("cancelled") }
            fail("CancellationException must be rethrown")
        } catch (error: CancellationException) {
            assertEquals("cancelled", error.message)
        }
    }
}
