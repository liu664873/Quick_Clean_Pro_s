package com.quickcleanpro.phonecleaner.use.app.runtime.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat

internal class PersistentServiceCommandReceiver(
    private val context: Context,
    private val onCommand: (PersistentServiceCommand) -> Unit,
) {
    private val receiver =
        object : BroadcastReceiver() {
            override fun onReceive(receiverContext: Context?, intent: Intent?) {
                if (intent?.`package` != context.packageName) return
                persistentServiceCommand(intent.action)?.let(onCommand)
            }
        }
    private var registered = false

    fun register() {
        if (registered) return
        val filter =
            IntentFilter().apply {
                addAction(PersistentServiceActions.START)
                addAction(PersistentServiceActions.ENABLE_MONITORING)
                addAction(PersistentServiceActions.DISABLE_MONITORING)
                addAction(PersistentServiceActions.APP_FOREGROUND)
                addAction(PersistentServiceActions.APP_BACKGROUND)
                addAction(PersistentServiceActions.RESTORE_NOTIFICATION)
                addAction(PersistentServiceActions.STOP_SERVICE)
                addAction(PersistentServiceActions.PASSWORD_SUCCESS)
                addAction(PersistentServiceActions.LOCK_SCREEN_CANCELLED)
            }
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        registered = true
    }

    fun unregister() {
        if (!registered) return
        runCatching { context.unregisterReceiver(receiver) }
        registered = false
    }
}
