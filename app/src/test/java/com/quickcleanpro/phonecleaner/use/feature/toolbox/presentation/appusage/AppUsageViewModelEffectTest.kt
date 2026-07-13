package com.quickcleanpro.phonecleaner.use.feature.toolbox.presentation.appusage

import com.quickcleanpro.phonecleaner.use.core.model.toolbox.AppUsageInfo
import com.quickcleanpro.phonecleaner.use.feature.toolbox.domain.AppUsageRepository
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class AppUsageViewModelEffectTest {
    @Test
    fun `open app info emits package name without exposing intent`() = runTest {
        val viewModel = AppUsageViewModel(repository = NoAccessRepository())
        val effect = async(start = CoroutineStart.UNDISPATCHED) { viewModel.effects.first() }

        viewModel.openAppInfo("com.example.target")

        assertEquals(AppUsageEffect.OpenAppInfo("com.example.target"), effect.await())
    }

    @Test
    fun `open usage settings emits platform independent effect`() = runTest {
        val viewModel = AppUsageViewModel(repository = NoAccessRepository())
        val effect = async(start = CoroutineStart.UNDISPATCHED) { viewModel.effects.first() }

        viewModel.openUsageSettings()

        assertEquals(AppUsageEffect.OpenUsageSettings, effect.await())
    }

    private class NoAccessRepository : AppUsageRepository {
        override fun hasAppUsageAccess(): Boolean = false

        override fun resetAppUsagePermissionCache() = Unit

        override suspend fun appUsageBetween(startMillis: Long, endMillis: Long): List<AppUsageInfo> = emptyList()

        override suspend fun runningPackages(packageNames: Set<String>): Set<String> = emptySet()
    }
}
