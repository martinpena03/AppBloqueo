package com.freno.app.domain.model

/** Estado calculado de un objetivo para mostrar en el dashboard. */
data class TargetUiStatus(
    val targetId: String,
    val displayName: String,
    val type: TargetType,
    val enabled: Boolean,
    val blocked: Boolean,
    val reason: BlockReason,
    val availableAgainAt: Long?,
    val usedMinutes: Long,
    val opens: Int,
    val tokensSpent: Int,
    val scrollCount: Int,
    val scrollQuota: Int?,
    val sessionMinutes: Long,
    val sessionLimitMin: Int?
)

/** Instantánea global para el dashboard. */
data class DashboardSnapshot(
    val remainingTokens: Int,
    val dailyBudget: Int,
    val resetAt: Long,
    val statuses: List<TargetUiStatus>
)

/** Instantánea reducida para el widget. */
data class WidgetSnapshot(
    val remainingTokens: Int,
    val dailyBudget: Int,
    val resetAt: Long,
    val blockedCount: Int
)
