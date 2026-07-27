package com.freno.app.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.freno.app.R
import com.freno.app.core.Notifications
import com.freno.app.di.Graph
import com.freno.app.domain.model.BlockDecision
import com.freno.app.ui.BlockActivity
import com.freno.app.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Servicio en primer plano dueño del "tick": mientras un objetivo está activo, descuenta tokens por
 * minuto, acumula tiempo de uso y re-evalúa límites. Si una regla se cumple, lanza la pantalla de bloqueo.
 */
class MonitoringService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var tickJob: Job? = null
    @Volatile private var currentTarget: String? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val id = intent.getStringExtra(EXTRA_TARGET_ID)
                startInForeground()
                if (id != null) startTicking(id) else stopEverything()
            }
            else -> { // ACTION_STOP u otro: satisface la promesa de startForeground y se detiene.
                startInForeground()
                stopEverything()
            }
        }
        return START_NOT_STICKY
    }

    private fun startTicking(id: String) {
        if (currentTarget == id && tickJob?.isActive == true) return
        tickJob?.cancel()
        currentTarget = id
        tickJob = scope.launch {
            while (isActive) {
                delay(TICK_MS)
                if (!Graph.isReady) continue
                val decision = Graph.repository.onUsageTick(id, TICK_SECONDS)
                if (decision is BlockDecision.Block) {
                    BlockActivity.launch(applicationContext, decision)
                    Graph.repository.onBackground(id)
                    stopEverything()
                    break
                }
            }
        }
    }

    private fun stopEverything() {
        tickJob?.cancel()
        tickJob = null
        currentTarget = null
        stopForegroundCompat()
        stopSelf()
    }

    private fun startInForeground() {
        val notif = buildNotification()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(
                Notifications.SERVICE_NOTIFICATION_ID,
                notif,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(Notifications.SERVICE_NOTIFICATION_ID, notif)
        }
    }

    private fun stopForegroundCompat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }

    private fun buildNotification(): Notification {
        val pi = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, Notifications.CHANNEL_SERVICE)
            .setSmallIcon(R.drawable.ic_stat_freno)
            .setContentTitle("Freno activo")
            .setContentText("Monitoreando el uso.")
            .setOngoing(true)
            .setContentIntent(pi)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
    }

    override fun onDestroy() {
        scope.coroutineContext[Job]?.cancel()
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.freno.app.MONITOR_START"
        const val ACTION_STOP = "com.freno.app.MONITOR_STOP"
        const val EXTRA_TARGET_ID = "target_id"
        const val TICK_MS = 5000L
        const val TICK_SECONDS = 5

        fun start(context: Context, targetId: String) {
            val i = Intent(context, MonitoringService::class.java)
                .setAction(ACTION_START)
                .putExtra(EXTRA_TARGET_ID, targetId)
            ContextCompat.startForegroundService(context, i)
        }

        fun stop(context: Context) {
            val i = Intent(context, MonitoringService::class.java).setAction(ACTION_STOP)
            ContextCompat.startForegroundService(context, i)
        }
    }
}
