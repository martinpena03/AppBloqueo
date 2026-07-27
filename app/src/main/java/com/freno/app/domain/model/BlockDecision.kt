package com.freno.app.domain.model

/** Resultado de evaluar las reglas sobre un objetivo. */
sealed interface BlockDecision {
    data object Allow : BlockDecision

    data class Block(
        val reason: BlockReason,
        /** Epoch millis en que el objetivo volverá a estar disponible; null = hasta el próximo reinicio. */
        val availableAgainAt: Long?,
        val targetName: String
    ) : BlockDecision
}
