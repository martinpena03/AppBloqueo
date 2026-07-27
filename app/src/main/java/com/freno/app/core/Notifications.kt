package com.freno.app.core

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object Notifications {
    const val CHANNEL_SERVICE = "freno_service"
    const val CHANNEL_ALERTS = "freno_alerts"
    const val SERVICE_NOTIFICATION_ID = 1001
    const val ALERT_NOTIFICATION_ID = 1002

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val mgr = context.getSystemService(NotificationManager::class.java) ?: return

        val service = NotificationChannel(
            CHANNEL_SERVICE,
            "Monitoreo activo",
            NotificationManager.IMPORTANCE_MIN
        ).apply {
            description = "Indica que Freno está monitoreando el uso."
            setShowBadge(false)
        }
        val alerts = NotificationChannel(
            CHANNEL_ALERTS,
            "Avisos",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Avisos como el servicio de accesibilidad desactivado."
            setShowBadge(false)
        }
        mgr.createNotificationChannel(service)
        mgr.createNotificationChannel(alerts)
    }
}
