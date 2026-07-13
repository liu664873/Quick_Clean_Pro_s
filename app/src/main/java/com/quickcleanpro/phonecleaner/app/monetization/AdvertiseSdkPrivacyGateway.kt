package com.quickcleanpro.phonecleaner.app.monetization

import android.app.Activity

class AdvertiseSdkPrivacyGateway : AdPrivacyGateway {
    override fun isPrivacyOptionsRequired(): Boolean = AdvertiseSdkAdapter.isPrivacyOptionsRequired()

    override fun showPrivacyOptions(activity: Activity) {
        AdvertiseSdkAdapter.showPrivacyOptions(activity)
    }
}
