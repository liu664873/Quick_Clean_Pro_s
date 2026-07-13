package com.quickcleanpro.phonecleaner.common.analytics

interface AnalyticsSink {
    fun track(eventName: String, properties: Map<String, Any>)

    fun setSuperProperties(properties: Map<String, Any?>)

    fun deviceId(): String

    fun setUserOnceProperty(key: String, value: String)

    fun setUserProperty(key: String, value: Any)

    fun addUserProperty(key: String, value: Number)
}
