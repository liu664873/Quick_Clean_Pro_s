package com.quickcleanpro.phonecleaner.app.runtime.notification

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationDefaultsInitializerTest {
    @Test
    fun `remote or cached defaults are not overwritten`() = runTest {
        var configUpdated = false
        var contentUpdated = false

        val result =
            initializer(
                hasConfig = { true },
                hasContent = { true },
                updateConfig = { configUpdated = true },
                updateContent = { contentUpdated = true },
            ).initialize()

        assertEquals(NotificationDefaultSource.RemoteOrCached, result.configSource)
        assertEquals(NotificationDefaultSource.RemoteOrCached, result.contentSource)
        assertTrue(result.remoteConfigCompleted)
        assertFalse(configUpdated)
        assertFalse(contentUpdated)
    }

    @Test
    fun `only missing config uses local fallback`() = runTest {
        var hasConfig = false
        var configValue: String? = null

        val result =
            initializer(
                hasConfig = { hasConfig },
                hasContent = { true },
                updateConfig = {
                    configValue = it
                    hasConfig = true
                },
            ).initialize()

        assertEquals(NotificationDefaultSource.LocalFallback, result.configSource)
        assertEquals(NotificationDefaultSource.RemoteOrCached, result.contentSource)
        assertEquals(LOCAL_CONFIG, configValue)
    }

    @Test
    fun `only missing content uses local fallback`() = runTest {
        var hasContent = false
        var contentValue: String? = null

        val result =
            initializer(
                hasConfig = { true },
                hasContent = { hasContent },
                updateContent = {
                    contentValue = it
                    hasContent = true
                },
            ).initialize()

        assertEquals(NotificationDefaultSource.RemoteOrCached, result.configSource)
        assertEquals(NotificationDefaultSource.LocalFallback, result.contentSource)
        assertEquals(LOCAL_CONTENT, contentValue)
    }

    @Test
    fun `remote timeout falls back for both missing defaults`() = runTest {
        var hasConfig = false
        var hasContent = false

        val result =
            initializer(
                remoteConfigCompleted = false,
                hasConfig = { hasConfig },
                hasContent = { hasContent },
                updateConfig = { hasConfig = true },
                updateContent = { hasContent = true },
            ).initialize()

        assertEquals(NotificationDefaultSource.LocalFallback, result.configSource)
        assertEquals(NotificationDefaultSource.LocalFallback, result.contentSource)
        assertFalse(result.remoteConfigCompleted)
    }

    @Test
    fun `blank local fallback fails initialization`() = runTest {
        val error =
            runCatching {
                initializer(
                    hasConfig = { false },
                    hasContent = { true },
                    loadLocalConfig = { " " },
                ).initialize()
            }.exceptionOrNull()

        assertTrue(error is IllegalStateException)
        assertTrue(error?.message.orEmpty().contains("notification_config"))
    }

    @Test
    fun `sdk rejection of local fallback fails initialization`() = runTest {
        val error =
            runCatching {
                initializer(
                    hasConfig = { false },
                    hasContent = { true },
                ).initialize()
            }.exceptionOrNull()

        assertTrue(error is IllegalStateException)
        assertTrue(error?.message.orEmpty().contains("rejected"))
    }

    private fun initializer(
        remoteConfigCompleted: Boolean = true,
        hasConfig: () -> Boolean,
        hasContent: () -> Boolean,
        loadLocalConfig: () -> String? = { LOCAL_CONFIG },
        loadLocalContent: () -> String? = { LOCAL_CONTENT },
        updateConfig: (String) -> Unit = {},
        updateContent: (String) -> Unit = {},
    ): NotificationDefaultsInitializer =
        NotificationDefaultsInitializer(
            awaitRemoteConfig = { remoteConfigCompleted },
            hasConfig = hasConfig,
            hasContent = hasContent,
            loadLocalConfig = loadLocalConfig,
            loadLocalContent = loadLocalContent,
            updateConfig = updateConfig,
            updateContent = updateContent,
        )

    private companion object {
        const val LOCAL_CONFIG = "{\"config\":true}"
        const val LOCAL_CONTENT = "[{\"content\":true}]"
    }
}
