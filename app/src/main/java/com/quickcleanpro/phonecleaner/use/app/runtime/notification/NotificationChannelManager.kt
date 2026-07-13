package com.quickcleanpro.phonecleaner.use.app.runtime.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import com.quickcleanpro.phonecleaner.R

object NotificationChannelManager {
    const val PERSISTENT_CHANNEL_ID = "quickclean_persistent"

    fun createAllChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(createPersistentChannel(context))
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createPersistentChannel(context: Context): NotificationChannel =
        NotificationChannel(
            PERSISTENT_CHANNEL_ID,
            context.getString(R.string.running_in_background),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = context.getString(R.string.running_in_background)
            setShowBadge(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
}
