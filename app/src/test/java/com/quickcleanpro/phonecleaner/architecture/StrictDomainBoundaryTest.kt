package com.quickcleanpro.phonecleaner.architecture

import com.quickcleanpro.phonecleaner.feature.notificationcleaner.logic.NotificationRepository
import com.quickcleanpro.phonecleaner.feature.applock.logic.AppLockRepository
import com.quickcleanpro.phonecleaner.feature.toolbox.logic.AppUsageRepository
import com.quickcleanpro.phonecleaner.feature.toolbox.logic.NetworkRepository
import org.junit.Assert.assertTrue
import org.junit.Test

class StrictDomainBoundaryTest {
    @Test
    fun `toolbox notification and app lock contracts do not expose Android types`() {
        val contracts =
            listOf(
                AppUsageRepository::class.java,
                NetworkRepository::class.java,
                NotificationRepository::class.java,
                AppLockRepository::class.java,
            )

        val androidTypes =
            contracts
                .flatMap { contract -> contract.declaredMethods.toList() }
                .flatMap { method -> listOf(method.genericReturnType) + method.genericParameterTypes }
                .map { type -> type.typeName }
                .filter { typeName -> typeName.contains("android.") }

        assertTrue("Domain contracts expose Android types: $androidTypes", androidTypes.isEmpty())
    }
}
