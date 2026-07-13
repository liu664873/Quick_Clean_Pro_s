package com.quickcleanpro.phonecleaner.advertise

import com.pdffox.adv.AdvertiseSdk
import com.quickcleanpro.phonecleaner.use.core.ads.AppOpenAdSuppression
import com.quickcleanpro.phonecleaner.use.core.ads.AppOpenSuppressionReason
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AppOpenAdSuppressionTest {
    private var originalEnabled = true
    private val releases = mutableListOf<() -> Unit>()

    @Before
    fun rememberSdkState() {
        originalEnabled = AdvertiseSdk.isAppOpenAdEnabled
    }

    @After
    fun restoreSdkState() {
        releases.asReversed().forEach { it() }
        releases.clear()
        AdvertiseSdk.isAppOpenAdEnabled = originalEnabled
    }

    @Test
    fun nestedLeasesKeepAppOpenAdSuppressedUntilLastRelease() {
        AdvertiseSdk.isAppOpenAdEnabled = true
        val firstRelease = acquire()
        val secondRelease = acquire()

        assertFalse(AdvertiseSdk.isAppOpenAdEnabled)

        firstRelease()

        assertFalse(AdvertiseSdk.isAppOpenAdEnabled)

        secondRelease()

        assertTrue(AdvertiseSdk.isAppOpenAdEnabled)
    }

    @Test
    fun finalReleaseRestoresInitiallyDisabledState() {
        AdvertiseSdk.isAppOpenAdEnabled = false
        val firstRelease = acquire()
        val secondRelease = acquire()

        secondRelease()
        firstRelease()

        assertFalse(AdvertiseSdk.isAppOpenAdEnabled)
    }

    @Test
    fun releasingSameLeaseTwiceDoesNotReleaseAnotherLease() {
        AdvertiseSdk.isAppOpenAdEnabled = true
        val firstRelease = acquire()
        val secondRelease = acquire()

        firstRelease()
        firstRelease()

        assertFalse(AdvertiseSdk.isAppOpenAdEnabled)

        secondRelease()

        assertTrue(AdvertiseSdk.isAppOpenAdEnabled)
    }

    private fun acquire(): () -> Unit =
        AppOpenAdSuppression.acquire(AppOpenSuppressionReason.Test).also { releases += it }
}
