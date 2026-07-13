package com.quickcleanpro.phonecleaner.app.permission

import org.junit.Assert.assertEquals
import org.junit.Test

class PermissionFlowControllerTest {
    @Test
    fun `granted permission completes without launching`() {
        val gateway = FakeGateway(PermissionState.Granted)
        val host = FakeHost()
        val outcomes = mutableListOf<PermissionOutcome>()
        val controller = DefaultPermissionFlowController(gateway, host)

        val result = controller.request(PermissionType.Location, true, outcomes::add)

        assertEquals(PermissionStartResult.Completed(PermissionOutcome.Granted), result)
        assertEquals(listOf(PermissionOutcome.Granted), outcomes)
        assertEquals(emptyList<PermissionRequest>(), host.requests)
    }

    @Test
    fun `runtime request emits one final denied result`() {
        val gateway = FakeGateway(PermissionState.Denied())
        gateway.plan = PermissionRequest.Runtime(PermissionType.Location, listOf("location"))
        val host = FakeHost()
        val outcomes = mutableListOf<PermissionOutcome>()
        val controller = DefaultPermissionFlowController(gateway, host)

        assertEquals(PermissionStartResult.Started, controller.request(PermissionType.Location, true, outcomes::add))
        gateway.current = PermissionState.Denied(permanently = true)
        controller.consumeRuntimeResult(PermissionType.Location, mapOf("location" to false))
        controller.consumeRuntimeResult(PermissionType.Location, mapOf("location" to false))

        assertEquals(listOf(PermissionOutcome.Denied(permanently = true)), outcomes)
    }

    @Test
    fun `second request reports busy without replacing active callback`() {
        val gateway = FakeGateway(PermissionState.Denied())
        gateway.plan = PermissionRequest.Settings(PermissionType.Overlay)
        val controller = DefaultPermissionFlowController(gateway, FakeHost())
        val first = mutableListOf<PermissionOutcome>()
        val second = mutableListOf<PermissionOutcome>()

        controller.request(PermissionType.Overlay, true, first::add)
        val result = controller.request(PermissionType.Location, true, second::add)
        gateway.current = PermissionState.Granted
        controller.consumeSettingsReturn()

        assertEquals(PermissionStartResult.Busy, result)
        assertEquals(listOf(PermissionOutcome.Busy), second)
        assertEquals(listOf(PermissionOutcome.Granted), first)
    }

    private class FakeGateway(var current: PermissionState) : PermissionGateway {
        var plan: PermissionRequest = PermissionRequest.Unavailable

        override fun state(permission: PermissionType): PermissionState = current
        override fun request(permission: PermissionType): PermissionRequest = plan
        override fun consumeRuntimeResult(permission: PermissionType, result: Map<String, Boolean>): PermissionState = current
    }

    private class FakeHost : PermissionHost {
        val requests = mutableListOf<PermissionRequest>()
        override fun launch(request: PermissionRequest): Boolean {
            requests += request
            return true
        }
    }
}
