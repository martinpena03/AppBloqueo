package com.freno.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.freno.app.R
import com.freno.app.data.repo.AppRepository
import com.freno.app.di.Graph
import com.freno.app.domain.util.TimeUtils
import com.freno.app.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Widget minimalista: tokens restantes, cuenta atrás al reinicio y nº de apps bloqueadas. */
class StatusWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, mgr: AppWidgetManager, ids: IntArray) {
        updateAll(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == AppRepository.ACTION_WIDGET_REFRESH) {
            updateAll(context)
        }
    }

    private fun updateAll(context: Context) {
        if (!Graph.isReady) {
            Graph.init(context)
        }
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val snap = Graph.repository.widgetSnapshot()
                val mgr = AppWidgetManager.getInstance(context)
                val ids = mgr.getAppWidgetIds(ComponentName(context, StatusWidgetProvider::class.java))
                val countdown = TimeUtils.formatCountdown(snap.resetAt - System.currentTimeMillis())
                val blockedText = if (snap.blockedCount > 0) {
                    "${snap.blockedCount} bloqueada(s)"
                } else "Sin bloqueos"

                val tapIntent = PendingIntent.getActivity(
                    context, 0,
                    Intent(context, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE
                )

                for (id in ids) {
                    val views = RemoteViews(context.packageName, R.layout.widget_status)
                    views.setTextViewText(R.id.widget_tokens, "${snap.remainingTokens} / ${snap.dailyBudget}")
                    views.setTextViewText(R.id.widget_reset, "Reinicia en $countdown")
                    views.setTextViewText(R.id.widget_blocked, blockedText)
                    views.setOnClickPendingIntent(R.id.widget_root, tapIntent)
                    mgr.updateAppWidget(id, views)
                }
            } finally {
                pending.finish()
            }
        }
    }
}
