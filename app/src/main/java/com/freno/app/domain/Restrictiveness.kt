package com.freno.app.domain

import com.freno.app.data.entity.MonitoredTarget

/**
 * Clasifica cambios de configuración según la rigidez "media":
 * apretar (más restrictivo o igual) se aplica al instante; aflojar se difiere (periodo de reflexión).
 */
object Restrictiveness {

    /** Minutos bloqueados al día por el horario (0 si no hay horario). */
    private fun scheduleBlockedMinutes(start: Int?, end: Int?): Int {
        if (start == null || end == null || start == end) return 0
        return if (start < end) end - start else (1440 - start) + end
    }

    /** Puntaje de un límite: menor = más restrictivo. null (sin límite) = infinito. */
    private fun limitScore(v: Int?): Long = v?.toLong() ?: Long.MAX_VALUE

    /**
     * true si [new] es al menos tan restrictivo como [old] en TODAS las dimensiones
     * (=> se puede aplicar de inmediato). Si afloja cualquier dimensión, devuelve false (=> diferir).
     */
    fun isTighteningOrEqual(old: MonitoredTarget, new: MonitoredTarget): Boolean {
        // Desactivar un objetivo lo hace menos restrictivo.
        if (old.enabled && !new.enabled) return false

        // Costos: mayor costo = más restrictivo.
        if (new.openCostTokens < old.openCostTokens) return false
        if (new.perMinuteCostTokens < old.perMinuteCostTokens) return false
        if (new.perScrollCost < old.perScrollCost) return false

        // Límites (tiempo, aperturas, cuota de scroll): menor = más restrictivo.
        if (limitScore(new.dailyTimeLimitMin) > limitScore(old.dailyTimeLimitMin)) return false
        if (limitScore(new.dailyOpenLimit) > limitScore(old.dailyOpenLimit)) return false
        if (limitScore(new.scrollQuota) > limitScore(old.scrollQuota)) return false

        // Cooldown: mayor = más restrictivo.
        if ((new.cooldownMin ?: 0) < (old.cooldownMin ?: 0)) return false

        // Horario: más minutos bloqueados = más restrictivo.
        val oldSch = scheduleBlockedMinutes(old.scheduleStart, old.scheduleEnd)
        val newSch = scheduleBlockedMinutes(new.scheduleStart, new.scheduleEnd)
        if (newSch < oldSch) return false

        return true
    }
}
