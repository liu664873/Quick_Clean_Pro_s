package com.quickcleanpro.phonecleaner.app.monetization

import android.app.Activity

interface AdPrivacyGateway {
    fun isPrivacyOptionsRequired(): Boolean

    fun showPrivacyOptions(activity: Activity)
}
