package com.freno.app.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.freno.app.di.Graph
import java.util.concurrent.TimeUnit

/**
 * Latido periódico (cada 15 min) que actúa como respaldo:
 * - aplica el reinicio diario de forma perezosa (ensureDayState),
 * - aplica los cambios diferidos que ya vencieron,
 * - refresca el widget.
 * El reinicio y los cambios también se aplican al vuelo durante el uso; esto es solo el backstop.
 */
class HeartbeatWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (Graph.isReady) {
            Graph.repository.ensureDayState()
            Graph.repository.applyDuePendingChanges()
            Graph.repository.widgetSnapshot() // fuerza reinicio perezoso + refresco de widget
        }
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "freno_heartbeat"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<HeartbeatWorker>(15, TimeUnit.MINUTES).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
