package com.freno.app.data.repo

import android.content.Context
import android.content.Intent
import com.freno.app.data.FeatureCatalog
import com.freno.app.data.TargetJson
import com.freno.app.data.dao.DayStateDao
import com.freno.app.data.dao.FeatureSignatureDao
import com.freno.app.data.dao.PendingChangeDao
import com.freno.app.data.dao.RuntimeDao
import com.freno.app.data.dao.StatDao
import com.freno.app.data.dao.TargetDao
import com.freno.app.data.entity.DayState
import com.freno.app.data.entity.MonitoredTarget
import com.freno.app.data.entity.PendingChange
import com.freno.app.data.entity.TargetDailyStat
import com.freno.app.data.entity.TargetRuntimeState
import com.freno.app.data.prefs.SettingsStore
import com.freno.app.domain.BlockPolicy
import com.freno.app.domain.Restrictiveness
import com.freno.app.domain.model.BlockDecision
import com.freno.app.domain.model.DashboardSnapshot
import com.freno.app.domain.model.QuotaWindow
import com.freno.app.domain.model.TargetType
import com.freno.app.domain.model.TargetUiStatus
import com.freno.app.domain.model.WidgetSnapshot
import com.freno.app.domain.util.TimeUtils
import java.util.concurrent.ConcurrentHashMap

/**
 * Única fuente de verdad. Centraliza tokens, evaluación de bloqueo, cuota de scroll,
 * reinicio diario y la cola de cambios diferidos.
 */
class AppRepository(
    private val appContext: Context,
    private val targetDao: TargetDao,
    private val statDao: StatDao,
    private val runtimeDao: RuntimeDao,
    private val dayStateDao: DayStateDao,
    private val pendingDao: PendingChangeDao,
    private val signatureDao: FeatureSignatureDao,
    private val settings: SettingsStore
) {
    /** Segundos acumulados por objetivo para el cobro por minuto (en memoria, por sesión). */
    private val secondsAccum = ConcurrentHashMap<String, Int>()

    @Volatile var monitoringEnabled: Boolean = true

    private fun now() = System.currentTimeMillis()

    // ---- Flows expuestos ----
    fun observeTargets() = targetDao.observeAll()
    fun observePending() = pendingDao.observeAll()
    fun observeDayState() = dayStateDao.observe()
    fun observeSignatures() = signatureDao.observeAll()

    suspend fun getTarget(id: String) = targetDao.getById(id)
    suspend fun getSignatures() = signatureDao.getAll()

    // ---- Semilla inicial ----
    suspend fun seedIfNeeded() {
        if (dayStateDao.get() == null) {
            val today = TimeUtils.dateKeyFor(now(), 0, 0)
            dayStateDao.upsert(
                DayState(id = 0, dateKey = today, dailyBudget = DEFAULT_BUDGET, remainingTokens = DEFAULT_BUDGET)
            )
        }
        if (signatureDao.count() == 0) {
            signatureDao.insertAll(FeatureCatalog.defaultSignatures())
        }
    }

    // ---- Día / reinicio ----
    suspend fun ensureDayState(): DayState {
        val existing = dayStateDao.get() ?: run {
            val today = TimeUtils.dateKeyFor(now(), 0, 0)
            val fresh = DayState(id = 0, dateKey = today, dailyBudget = DEFAULT_BUDGET, remainingTokens = DEFAULT_BUDGET)
            dayStateDao.upsert(fresh)
            return fresh
        }
        val today = TimeUtils.dateKeyFor(now(), existing.resetHour, existing.resetMinute)
        return if (existing.dateKey != today) performReset(existing, today) else existing
    }

    private suspend fun performReset(prev: DayState, today: String): DayState {
        val reset = prev.copy(dateKey = today, remainingTokens = prev.dailyBudget)
        dayStateDao.upsert(reset)
        statDao.deleteOtherDays(today)
        runtimeDao.clearAllQuotaBlocks()
        secondsAccum.clear()
        requestWidgetRefresh()
        return reset
    }

    private suspend fun ensureStat(id: String, date: String): TargetDailyStat =
        statDao.get(id, date) ?: TargetDailyStat(targetId = id, dateKey = date).also { statDao.upsert(it) }

    private suspend fun ensureRuntime(id: String): TargetRuntimeState =
        runtimeDao.get(id) ?: TargetRuntimeState(targetId = id).also { runtimeDao.upsert(it) }

    // ---- Resolución de objetivo activo ----
    suspend fun resolveActiveTarget(packageName: String, featureKey: String?): MonitoredTarget? {
        if (!monitoringEnabled) return null
        val list = targetDao.getByPackage(packageName).filter { it.enabled }
        if (featureKey != null) {
            list.firstOrNull { it.type == TargetType.FEATURE && it.featureKey == featureKey }?.let { return it }
        }
        return list.firstOrNull { it.type == TargetType.APP }
    }

    // ---- Eventos de uso ----
    suspend fun onForeground(targetId: String): BlockDecision {
        val day = ensureDayState()
        val target = targetDao.getById(targetId) ?: return BlockDecision.Allow
        if (!target.enabled) return BlockDecision.Allow
        val date = day.dateKey
        val stat = ensureStat(targetId, date)
        val runtime = ensureRuntime(targetId)

        val decision = BlockPolicy.evaluate(target, stat, runtime, day, now(), isOpening = true)
        if (decision is BlockDecision.Block) {
            runtimeDao.upsert(runtime.copy(isForeground = false))
            requestWidgetRefresh()
            return decision
        }

        // Permitido: cobra apertura, +1 apertura, marca en primer plano.
        val newRemaining = (day.remainingTokens - target.openCostTokens).coerceAtLeast(0)
        dayStateDao.upsert(day.copy(remainingTokens = newRemaining))
        statDao.upsert(
            stat.copy(
                opensCount = stat.opensCount + 1,
                tokensSpent = stat.tokensSpent + target.openCostTokens
            )
        )
        runtimeDao.upsert(runtime.copy(isForeground = true))
        secondsAccum[targetId] = 0
        requestWidgetRefresh()
        return BlockDecision.Allow
    }

    suspend fun onUsageTick(targetId: String, elapsedSeconds: Int): BlockDecision {
        val day = ensureDayState()
        val target = targetDao.getById(targetId) ?: return BlockDecision.Allow
        val date = day.dateKey
        val stat = ensureStat(targetId, date)
        val runtime = ensureRuntime(targetId)

        // Cobro por minutos completos acumulados.
        val acc = (secondsAccum[targetId] ?: 0) + elapsedSeconds
        val minutes = acc / 60
        secondsAccum[targetId] = acc % 60

        var remaining = day.remainingTokens
        var spent = 0
        if (minutes > 0 && target.perMinuteCostTokens > 0) {
            spent = minutes * target.perMinuteCostTokens
            remaining = (remaining - spent).coerceAtLeast(0)
        }

        val updatedStat = stat.copy(
            usedSeconds = stat.usedSeconds + elapsedSeconds,
            tokensSpent = stat.tokensSpent + spent
        )
        statDao.upsert(updatedStat)
        val updatedDay = if (spent > 0) day.copy(remainingTokens = remaining) else day
        if (spent > 0) dayStateDao.upsert(updatedDay)

        requestWidgetRefresh()
        return BlockPolicy.evaluate(target, updatedStat, runtime, updatedDay, now(), isOpening = false)
    }

    suspend fun onScroll(targetId: String): BlockDecision {
        val day = ensureDayState()
        val target = targetDao.getById(targetId) ?: return BlockDecision.Allow
        val quota = target.scrollQuota
        if (target.type != TargetType.FEATURE || quota == null) return BlockDecision.Allow
        val date = day.dateKey
        val stat = ensureStat(targetId, date)
        val runtime = ensureRuntime(targetId)

        val newCount = stat.scrollCount + 1
        var remaining = day.remainingTokens
        var spent = 0
        if (target.perScrollCost > 0) {
            spent = target.perScrollCost
            remaining = (remaining - spent).coerceAtLeast(0)
        }
        val updatedStat = stat.copy(scrollCount = newCount, tokensSpent = stat.tokensSpent + spent)
        statDao.upsert(updatedStat)

        var updatedRuntime = runtime
        if (newCount >= quota) {
            val until = when (target.quotaWindow) {
                QuotaWindow.UNTIL_RESET -> TimeUtils.nextResetAt(now(), day.resetHour, day.resetMinute)
                QuotaWindow.COOLDOWN -> now() + target.quotaWindowHours * 3_600_000L
            }
            updatedRuntime = runtime.copy(quotaBlockedUntil = until)
            runtimeDao.upsert(updatedRuntime)
        }

        val updatedDay = if (spent > 0) day.copy(remainingTokens = remaining) else day
        if (spent > 0) dayStateDao.upsert(updatedDay)

        requestWidgetRefresh()
        return BlockPolicy.evaluate(target, updatedStat, updatedRuntime, updatedDay, now(), isOpening = false)
    }

    suspend fun onBackground(targetId: String) {
        val runtime = ensureRuntime(targetId)
        runtimeDao.upsert(runtime.copy(isForeground = false, lastClosedAt = now()))
        secondsAccum.remove(targetId)
        requestWidgetRefresh()
    }

    // ---- Instantáneas para UI / widget ----
    suspend fun dashboardSnapshot(): DashboardSnapshot {
        val day = ensureDayState()
        val date = day.dateKey
        val targets = targetDao.getAllSorted()
        val statuses = targets.map { t ->
            val stat = statDao.get(t.targetId, date) ?: TargetDailyStat(t.targetId, date)
            val rt = runtimeDao.get(t.targetId) ?: TargetRuntimeState(t.targetId)
            val decision = if (t.enabled) {
                BlockPolicy.evaluate(t, stat, rt, day, now(), isOpening = true)
            } else BlockDecision.Allow
            val block = decision as? BlockDecision.Block
            TargetUiStatus(
                targetId = t.targetId,
                displayName = t.displayName,
                type = t.type,
                enabled = t.enabled,
                blocked = block != null,
                reason = block?.reason ?: com.freno.app.domain.model.BlockReason.NONE,
                availableAgainAt = block?.availableAgainAt,
                usedMinutes = stat.usedSeconds / 60,
                opens = stat.opensCount,
                tokensSpent = stat.tokensSpent,
                scrollCount = stat.scrollCount,
                scrollQuota = t.scrollQuota
            )
        }
        return DashboardSnapshot(
            remainingTokens = day.remainingTokens,
            dailyBudget = day.dailyBudget,
            resetAt = TimeUtils.nextResetAt(now(), day.resetHour, day.resetMinute),
            statuses = statuses
        )
    }

    suspend fun widgetSnapshot(): WidgetSnapshot {
        val day = ensureDayState()
        val date = day.dateKey
        val targets = targetDao.getEnabled()
        var blocked = 0
        for (t in targets) {
            val stat = statDao.get(t.targetId, date) ?: TargetDailyStat(t.targetId, date)
            val rt = runtimeDao.get(t.targetId) ?: TargetRuntimeState(t.targetId)
            if (BlockPolicy.evaluate(t, stat, rt, day, now(), isOpening = true) is BlockDecision.Block) blocked++
        }
        return WidgetSnapshot(
            remainingTokens = day.remainingTokens,
            dailyBudget = day.dailyBudget,
            resetAt = TimeUtils.nextResetAt(now(), day.resetHour, day.resetMinute),
            blockedCount = blocked
        )
    }

    // ---- Cambios de configuración (rigidez media) ----

    /** Añadir un objetivo nuevo = apretar => inmediato. */
    suspend fun addTargetImmediate(target: MonitoredTarget) {
        targetDao.upsert(target)
        ensureRuntime(target.targetId)
        requestWidgetRefresh()
    }

    /**
     * Guarda una edición de objetivo. Si es apretar (o igual) se aplica al instante;
     * si afloja algo, se difiere [reflectionMinutes] minutos.
     * @return true si se aplicó ahora, false si quedó en cola.
     */
    suspend fun saveTargetConfig(new: MonitoredTarget, reflectionMinutes: Int): Boolean {
        val old = targetDao.getById(new.targetId)
        if (old == null || Restrictiveness.isTighteningOrEqual(old, new)) {
            targetDao.upsert(new)
            requestWidgetRefresh()
            return true
        }
        pendingDao.insert(
            PendingChange(
                description = "Cambio en ${new.displayName}",
                kind = KIND_SAVE_TARGET,
                targetId = new.targetId,
                payload = TargetJson.encode(new),
                applyAt = now() + reflectionMinutes * 60_000L
            )
        )
        return false
    }

    /** Quitar un objetivo = aflojar => diferido. */
    suspend fun removeTargetDeferred(targetId: String, displayName: String, reflectionMinutes: Int) {
        pendingDao.insert(
            PendingChange(
                description = "Dejar de monitorear $displayName",
                kind = KIND_REMOVE_TARGET,
                targetId = targetId,
                payload = targetId,
                applyAt = now() + reflectionMinutes * 60_000L
            )
        )
    }

    /** Cambia el presupuesto diario. Subirlo = aflojar (diferido); bajarlo = apretar (inmediato). */
    suspend fun changeBudget(newBudget: Int, reflectionMinutes: Int): Boolean {
        val day = ensureDayState()
        if (newBudget <= day.dailyBudget) {
            dayStateDao.upsert(day.copy(dailyBudget = newBudget, remainingTokens = day.remainingTokens.coerceAtMost(newBudget)))
            requestWidgetRefresh()
            return true
        }
        pendingDao.insert(
            PendingChange(
                description = "Subir presupuesto a $newBudget tokens",
                kind = KIND_SET_BUDGET,
                payload = newBudget.toString(),
                applyAt = now() + reflectionMinutes * 60_000L
            )
        )
        return false
    }

    suspend fun setResetTime(hour: Int, minute: Int) {
        val day = ensureDayState()
        dayStateDao.upsert(day.copy(resetHour = hour, resetMinute = minute))
        requestWidgetRefresh()
    }

    suspend fun cancelPending(change: PendingChange) = pendingDao.delete(change)

    suspend fun applyDuePendingChanges() {
        val due = pendingDao.getDue(now())
        for (c in due) {
            when (c.kind) {
                KIND_SAVE_TARGET -> targetDao.upsert(TargetJson.decode(c.payload))
                KIND_REMOVE_TARGET -> {
                    targetDao.deleteById(c.payload)
                }
                KIND_SET_BUDGET -> {
                    val b = c.payload.toIntOrNull()
                    if (b != null) {
                        val day = ensureDayState()
                        dayStateDao.upsert(day.copy(dailyBudget = b))
                    }
                }
                KIND_DISABLE_TARGET -> targetDao.setEnabled(c.payload, false)
            }
            pendingDao.deleteById(c.id)
        }
        if (due.isNotEmpty()) requestWidgetRefresh()
    }

    fun setMonitoringCache(enabled: Boolean) { monitoringEnabled = enabled }

    private fun requestWidgetRefresh() {
        val intent = Intent(appContext, com.freno.app.widget.StatusWidgetProvider::class.java)
            .setAction(ACTION_WIDGET_REFRESH)
        appContext.sendBroadcast(intent)
    }

    companion object {
        const val DEFAULT_BUDGET = 60
        const val ACTION_WIDGET_REFRESH = "com.freno.app.ACTION_WIDGET_REFRESH"

        const val KIND_SAVE_TARGET = "SAVE_TARGET"
        const val KIND_REMOVE_TARGET = "REMOVE_TARGET"
        const val KIND_SET_BUDGET = "SET_BUDGET"
        const val KIND_DISABLE_TARGET = "DISABLE_TARGET"
    }
}
