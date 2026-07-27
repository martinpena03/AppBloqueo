package com.freno.app

import android.app.Application
import com.freno.app.core.Notifications
import com.freno.app.di.Graph
import com.freno.app.work.HeartbeatWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class FrenoApp : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        Graph.init(this)
        Notifications.createChannels(this)

        appScope.launch {
            Graph.repository.seedIfNeeded()
            Graph.repository.ensureDayState()
            Graph.repository.applyDuePendingChanges()
        }
        appScope.launch {
            Graph.settings.monitoringEnabled.collect { Graph.repository.setMonitoringCache(it) }
        }

        HeartbeatWorker.schedule(this)
    }
}
