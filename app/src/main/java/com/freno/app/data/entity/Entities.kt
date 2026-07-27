package com.freno.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.freno.app.domain.model.QuotaWindow
import com.freno.app.domain.model.TargetType

/**
 * Configuración de un objetivo monitoreado.
 * targetId = packageName para apps; "packageName#featureKey" para funciones (Reels/Shorts).
 * Horarios en minutos desde medianoche. Campos de límite nulos = regla desactivada.
 */
@Entity(tableName = "targets")
data class MonitoredTarget(
    @PrimaryKey val targetId: String,
    val type: TargetType,
    val packageName: String,
    val featureKey: String? = null,
    val displayName: String,
    val enabled: Boolean = true,
    val openCostTokens: Int = 5,
    val perMinuteCostTokens: Int = 1,
    val perScrollCost: Int = 0,
    val dailyTimeLimitMin: Int? = null,
    val dailyOpenLimit: Int? = null,
    val cooldownMin: Int? = null,
    val scheduleStart: Int? = null,
    val scheduleEnd: Int? = null,
    val scrollQuota: Int? = null,
    val quotaWindow: QuotaWindow = QuotaWindow.UNTIL_RESET,
    val quotaWindowHours: Int = 2
)

/** Contadores por objetivo y día. */
@Entity(tableName = "daily_stats", primaryKeys = ["targetId", "dateKey"])
data class TargetDailyStat(
    val targetId: String,
    val dateKey: String,
    val usedSeconds: Long = 0,
    val opensCount: Int = 0,
    val tokensSpent: Int = 0,
    val scrollCount: Int = 0
)

/** Estado volátil por objetivo. */
@Entity(tableName = "runtime_state")
data class TargetRuntimeState(
    @PrimaryKey val targetId: String,
    val lastClosedAt: Long = 0,
    val isForeground: Boolean = false,
    val quotaBlockedUntil: Long = 0
)

/** Estado global del presupuesto de tokens (fila única id=0). */
@Entity(tableName = "day_state")
data class DayState(
    @PrimaryKey val id: Int = 0,
    val dateKey: String,
    val dailyBudget: Int = 60,
    val remainingTokens: Int = 60,
    val resetHour: Int = 0,
    val resetMinute: Int = 0
)

/** Cambio "que afloja" una restricción, en cola por el periodo de reflexión. */
@Entity(tableName = "pending_changes")
data class PendingChange(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val description: String,
    val kind: String,
    val targetId: String? = null,
    val payload: String,
    val applyAt: Long
)

/** Firma de detección de una función dentro de una app (Reels/Shorts). Editable por el usuario. */
@Entity(tableName = "feature_signatures")
data class FeatureSignature(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val featureKey: String,
    val packageName: String,
    val matchType: String,
    val pattern: String
)
