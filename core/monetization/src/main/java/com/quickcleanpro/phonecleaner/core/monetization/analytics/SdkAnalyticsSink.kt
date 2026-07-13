package com.quickcleanpro.phonecleaner.core.monetization.analytics

import com.pdffox.adv.AdvertiseSdk
import com.pdffox.adv.log.ThinkingAttr

object SdkAnalyticsSink : AnalyticsSink {
    override fun track(eventName: String, properties: Map<String, Any>) {
        AdvertiseSdk.logEvent(eventName, properties)
    }

    override fun setSuperProperties(properties: Map<String, Any?>) {
        AdvertiseSdk.setSuperProperties(properties)
    }

    override fun deviceId(): String = AdvertiseSdk.getThinkingDeviceId()

    override fun setUserOnceProperty(key: String, value: String) {
        AdvertiseSdk.setUserOnceAttr(key, value)
    }

    override fun setUserProperty(key: String, value: Any) {
        AdvertiseSdk.setUserAttr(key, value)
    }

    override fun addUserProperty(key: String, value: Number) {
        ThinkingAttr.setUserAddAttr(key, value)
    }
}
