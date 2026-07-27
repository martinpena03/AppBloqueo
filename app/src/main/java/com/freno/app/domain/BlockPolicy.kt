package com.freno.app.domain

import com.freno.app.data.entity.DayState
import com.freno.app.data.entity.MonitoredTarget
import com.freno.app.data.entity.TargetDailyStat
import com.freno.app.data.entity.TargetRuntimeState
import com.freno.app.domain.model.BlockDecision
import com.freno.app.domain.model.BlockReason
import com.freno.app.domain.model.QuotaWindow
import com.freno.app.domain.model.TargetType
import com.freno.app.domain.util.TimeUtils

/**
 * Función pura de decisión. No muta estado.
 * Orden (modelo híbrido): reglas duras primero, luego cuota de scroll, luego tokens globales.
 * @param isOpening true cuando se evalúa al abrir (activa cooldown, límite de aperturas y costo de apertura).
 */
object BlockPolicy {

    fun evaluate(
        t: MonitoredTarget,
        stat: TargetDailyStat,
        rt: TargetRuntimeState,
        day: DayState,
        now: Long,
        isOpening: Boolean
    ): BlockDecision {
        val name = t.displayName
        val nextReset = TimeUtils.nextResetAt(now, day.resetHour, day.resetMinute)

        // 1. Horario (regla dura), soporta cruce de medianoche.
        val start = t.scheduleStart
        val end = t.scheduleEnd
        if (start != null && end != null && start != end) {
            val mins = TimeUtils.minutesOfDay(now)
            val inWindow = if (start < end) mins in start until end
            else (mins >= start || mins < end)
            if (inWindow) {
                return BlockDecision.Block(BlockReason.SCHEDULE, TimeUtils.nextTimeOfDay(now, end), name)
            }
        }

        // 2. Cooldown (regla dura, solo al abrir).
        val cd = t.cooldownMin
        if (isOpening && cd != null && cd > 0 && rt.lastClosedAt > 0) {
            val until = rt.lastClosedAt + cd * 60_000L
            if (now < until) return BlockDecision.Block(BlockReason.COOLDOWN, until, name)
        }

        // 3. Máximo de aperturas por día (solo al abrir).
        val openLimit = t.dailyOpenLimit
        if (isOpening && openLimit != null && stat.opensCount >= openLimit) {
            return BlockDecision.Block(BlockReason.OPEN_LIMIT, nextReset, name)
        }

        // 4. Tiempo total por día.
        val timeLimit = t.dailyTimeLimitMin
        if (timeLimit != null && stat.usedSeconds / 60 >= timeLimit) {
            return BlockDecision.Block(BlockReason.TIME_LIMIT, nextReset, name)
        }

        // 5. Cuota de scroll (Reels/Shorts).
        if (t.type == TargetType.FEATURE) {
            val quota = t.scrollQuota
            if (rt.quotaBlockedUntil > now) {
                return BlockDecision.Block(BlockReason.SCROLL_QUOTA, rt.quotaBlockedUntil, name)
            }
            if (quota != null && stat.scrollCount >= quota) {
                val until = when (t.quotaWindow) {
                    QuotaWindow.UNTIL_RESET -> nextReset
                    QuotaWindow.COOLDOWN -> now + t.quotaWindowHours * 3_600_000L
                }
                return BlockDecision.Block(BlockReason.SCROLL_QUOTA, until, name)
            }
        }

        // 6. Tokens globales.
        if (day.remainingTokens <= 0) {
            return BlockDecision.Block(BlockReason.NO_TOKENS, nextReset, name)
        }
        if (isOpening && day.remainingTokens < t.openCostTokens) {
            return BlockDecision.Block(BlockReason.INSUFFICIENT_TOKENS, nextReset, name)
        }

        return BlockDecision.Allow
    }
}
