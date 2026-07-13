package com.quickcleanpro.phonecleaner.core.monetization.analytics

class AnalyticsDispatcher(
    private val sink: AnalyticsSink,
    private val onFailure: (operation: String, Throwable) -> Unit = { _, _ -> },
) {
    fun track(eventName: String, properties: Map<String, Any>): Boolean =
        execute("track:$eventName") { sink.track(eventName, properties) }

    fun setSuperProperties(properties: Map<String, Any?>): Boolean =
        execute("setSuperProperties") { sink.setSuperProperties(properties) }

    fun deviceId(): String? =
        valueOrNull("deviceId") { sink.deviceId() }

    fun setUserOnceProperty(key: String, value: String): Boolean =
        execute("setUserOnceProperty:$key") { sink.setUserOnceProperty(key, value) }

    fun setUserProperty(key: String, value: Any): Boolean =
        execute("setUserProperty:$key") { sink.setUserProperty(key, value) }

    fun addUserProperty(key: String, value: Number): Boolean =
        execute("addUserProperty:$key") { sink.addUserProperty(key, value) }

    private inline fun execute(operation: String, block: () -> Unit): Boolean =
        try {
            block()
            true
        } catch (throwable: Exception) {
            onFailure(operation, throwable)
            false
        }

    private inline fun <T> valueOrNull(operation: String, block: () -> T): T? =
        try {
            block()
        } catch (throwable: Exception) {
            onFailure(operation, throwable)
            null
        }
}
