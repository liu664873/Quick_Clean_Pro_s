package com.quickcleanpro.phonecleaner.app.runtime.notification

enum class NotificationDefaultSource {
    RemoteOrCached,
    LocalFallback,
}

data class NotificationDefaultsResult(
    val configSource: NotificationDefaultSource,
    val contentSource: NotificationDefaultSource,
    val remoteConfigCompleted: Boolean,
)

class NotificationDefaultsInitializer(
    private val awaitRemoteConfig: suspend () -> Boolean,
    private val hasConfig: () -> Boolean,
    private val hasContent: () -> Boolean,
    private val loadLocalConfig: () -> String?,
    private val loadLocalContent: () -> String?,
    private val updateConfig: (String) -> Unit,
    private val updateContent: (String) -> Unit,
) {
    suspend fun initialize(): NotificationDefaultsResult {
        val remoteConfigCompleted = awaitRemoteConfig()
        val configSource =
            ensureDefault(
                name = "notification_config",
                hasValue = hasConfig,
                loadLocal = loadLocalConfig,
                update = updateConfig,
            )
        val contentSource =
            ensureDefault(
                name = "notification_content",
                hasValue = hasContent,
                loadLocal = loadLocalContent,
                update = updateContent,
            )
        return NotificationDefaultsResult(
            configSource = configSource,
            contentSource = contentSource,
            remoteConfigCompleted = remoteConfigCompleted,
        )
    }

    private fun ensureDefault(
        name: String,
        hasValue: () -> Boolean,
        loadLocal: () -> String?,
        update: (String) -> Unit,
    ): NotificationDefaultSource {
        if (hasValue()) return NotificationDefaultSource.RemoteOrCached
        val localValue = loadLocal()?.takeIf(String::isNotBlank)
            ?: error("Local $name fallback is empty")
        update(localValue)
        check(hasValue()) { "SDK rejected local $name fallback" }
        return NotificationDefaultSource.LocalFallback
    }
}
