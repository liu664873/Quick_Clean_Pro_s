package com.quickcleanpro.phonecleaner.feature.toolbox.appusage


import com.quickcleanpro.phonecleaner.feature.toolbox.appusage.AppUsageInfo
import com.quickcleanpro.phonecleaner.feature.toolbox.appusage.data.AppUsageRepository
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUsageViewModelEffectTest {
    @Test
    fun `open app info emits package name without exposing intent`() = runTest {
        val viewModel = AppUsageViewModel(repository = NoAccessRepository())
        val effect = async(start = CoroutineStart.UNDISPATCHED) { viewModel.effects.first() }

        viewModel.onAction(AppUsageAction.StopApp("com.example.target"))

        assertEquals(AppUsageEffect.OpenAppInfo("com.example.target"), effect.await())
    }

    @Test
    fun `permission rejection exits through domain effect`() = runTest {
        val viewModel = AppUsageViewModel(repository = NoAccessRepository())
        val effect = async(start = CoroutineStart.UNDISPATCHED) { viewModel.effects.first() }

        assertTrue(viewModel.uiState.value.permissionRequestPending)
        viewModel.onAction(AppUsageAction.PermissionRequestConsumed)
        assertFalse(viewModel.uiState.value.permissionRequestPending)
        viewModel.onAction(AppUsageAction.PermissionRejected)

        assertEquals(
            AppUsageEffect.Exit(AppUsageExitReason.PermissionRejected),
            effect.await(),
        )
    }

    private class NoAccessRepository : AppUsageRepository {
        override fun hasAppUsageAccess(): Boolean = false

        override fun resetAppUsagePermissionCache() = Unit

        override suspend fun appUsageBetween(startMillis: Long, endMillis: Long): List<AppUsageInfo> = emptyList()

        override suspend fun runningPackages(packageNames: Set<String>): Set<String> = emptySet()
    }
}
