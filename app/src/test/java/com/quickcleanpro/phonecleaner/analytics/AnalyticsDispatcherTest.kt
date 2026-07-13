package com.quickcleanpro.phonecleaner.analytics

import com.quickcleanpro.phonecleaner.common.analytics.AnalyticsDispatcher
import com.quickcleanpro.phonecleaner.common.analytics.AnalyticsSink
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnalyticsDispatcherTest {
    @Test
    fun `delegates analytics operations without changing event data`() {
        val sink = RecordingSink()
        val dispatcher = AnalyticsDispatcher(sink)
        val properties = mapOf("source" to "push", "count" to 2)

        assertTrue(dispatcher.track("enter_homepage", properties))
        assertTrue(dispatcher.setSuperProperties(mapOf("traffic_source" to "fcm_push")))
        assertEquals("device-123", dispatcher.deviceId())
        assertTrue(dispatcher.setUserOnceProperty("first_open_time", "2026-01-01"))
        assertTrue(dispatcher.setUserProperty("latest_open_time", "2026-01-02"))
        assertTrue(dispatcher.addUserProperty("total_open_num", 1))

        assertEquals("enter_homepage", sink.eventName)
        assertEquals(properties, sink.eventProperties)
        assertEquals(mapOf("traffic_source" to "fcm_push"), sink.recordedSuperProperties)
        assertEquals("first_open_time" to "2026-01-01", sink.onceProperty)
        assertEquals("latest_open_time" to "2026-01-02", sink.userProperty)
        assertEquals("total_open_num" to 1, sink.addedProperty)
    }

    @Test
    fun `isolates a failed sink operation from later events`() {
        val sink = RecordingSink(failFirstTrack = true)
        val failures = mutableListOf<String>()
        val dispatcher = AnalyticsDispatcher(sink) { operation, _ -> failures += operation }

        assertFalse(dispatcher.track("first_event", emptyMap()))
        assertTrue(dispatcher.track("second_event", mapOf("ok" to true)))
        assertTrue(dispatcher.setUserProperty("latest_open_time", "now"))

        assertEquals(listOf("track:first_event"), failures)
        assertEquals("second_event", sink.eventName)
        assertEquals(mapOf("ok" to true), sink.eventProperties)
        assertEquals("latest_open_time" to "now", sink.userProperty)
    }

    @Test
    fun `returns null when device id lookup fails`() {
        val failures = mutableListOf<String>()
        val dispatcher = AnalyticsDispatcher(
            sink = RecordingSink(failDeviceId = true),
            onFailure = { operation, _ -> failures += operation },
        )

        assertEquals(null, dispatcher.deviceId())
        assertEquals(listOf("deviceId"), failures)
    }

    private class RecordingSink(
        private val failFirstTrack: Boolean = false,
        private val failDeviceId: Boolean = false,
    ) : AnalyticsSink {
        var eventName: String? = null
        var eventProperties: Map<String, Any>? = null
        var recordedSuperProperties: Map<String, Any?>? = null
        var onceProperty: Pair<String, String>? = null
        var userProperty: Pair<String, Any>? = null
        var addedProperty: Pair<String, Number>? = null
        private var trackCalls = 0

        override fun track(eventName: String, properties: Map<String, Any>) {
            trackCalls += 1
            if (failFirstTrack && trackCalls == 1) error("track failed")
            this.eventName = eventName
            eventProperties = properties
        }

        override fun setSuperProperties(properties: Map<String, Any?>) {
            recordedSuperProperties = properties
        }

        override fun deviceId(): String {
            if (failDeviceId) error("device id failed")
            return "device-123"
        }

        override fun setUserOnceProperty(key: String, value: String) {
            onceProperty = key to value
        }

        override fun setUserProperty(key: String, value: Any) {
            userProperty = key to value
        }

        override fun addUserProperty(key: String, value: Number) {
            addedProperty = key to value
        }
    }
}
