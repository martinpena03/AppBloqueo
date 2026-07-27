package com.freno.app.work

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.freno.app.data.repo.AppRepository
import com.freno.app.di.Graph
import com.freno.app.widget.StatusWidgetProvider

/** Tras reiniciar el teléfono o actualizar la app: re-programa el heartbeat y refresca el widget. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        Graph.init(context)
        HeartbeatWorker.schedule(context)
        context.sendBroadcast(
            Intent(context, StatusWidgetProvider::class.java).setAction(AppRepository.ACTION_WIDGET_REFRESH)
        )
    }
}
