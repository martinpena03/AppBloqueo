package com.freno.app.domain.util

import java.time.Instant
import java.time.ZoneId
import java.util.Locale

/** Utilidades de tiempo basadas en la zona horaria local del dispositivo. */
object TimeUtils {

    private fun zone(): ZoneId = ZoneId.systemDefault()

    /**
     * Clave de día ("yyyy-MM-dd") teniendo en cuenta la hora de reinicio.
     * Ej. si el reinicio es a las 04:00, todo lo anterior a las 04:00 cuenta como el día anterior.
     */
    fun dateKeyFor(now: Long, resetHour: Int, resetMinute: Int): String {
        val shifted = Instant.ofEpochMilli(now)
            .atZone(zone())
            .minusMinutes((resetHour * 60 + resetMinute).toLong())
        return shifted.toLocalDate().toString()
    }

    /** Próxima ocurrencia (epoch millis) de la hora de reinicio. */
    fun nextResetAt(now: Long, resetHour: Int, resetMinute: Int): Long =
        nextTimeOfDay(now, resetHour * 60 + resetMinute)

    /** Próxima ocurrencia (epoch millis) de un minuto-del-día dado. */
    fun nextTimeOfDay(now: Long, minutesOfDay: Int): Long {
        val z = zone()
        val nowZ = Instant.ofEpochMilli(now).atZone(z)
        var t = nowZ.toLocalDate()
            .atTime(minutesOfDay / 60, minutesOfDay % 60)
            .atZone(z)
        if (t.toInstant().toEpochMilli() <= now) {
            t = t.plusDays(1)
        }
        return t.toInstant().toEpochMilli()
    }

    /** Minutos transcurridos desde la medianoche local. */
    fun minutesOfDay(now: Long): Int {
        val t = Instant.ofEpochMilli(now).atZone(zone()).toLocalTime()
        return t.hour * 60 + t.minute
    }

    /** Formatea "HH:mm" a partir de minutos-del-día. */
    fun formatHm(minutesOfDay: Int): String =
        String.format(Locale.getDefault(), "%02d:%02d", minutesOfDay / 60, minutesOfDay % 60)

    /** Formatea una cuenta atrás en "Xh Ym" o "Ym" a partir de una duración en millis. */
    fun formatCountdown(millis: Long): String {
        if (millis <= 0) return "0m"
        val totalMin = (millis / 60000).toInt()
        val h = totalMin / 60
        val m = totalMin % 60
        return if (h > 0) "${h}h ${m}m" else "${m}m"
    }

    /** Formatea "HH:mm" a partir de un epoch millis. */
    fun formatClock(epochMillis: Long): String {
        val t = Instant.ofEpochMilli(epochMillis).atZone(zone()).toLocalTime()
        return String.format(Locale.getDefault(), "%02d:%02d", t.hour, t.minute)
    }
}
