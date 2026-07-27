package com.freno.app.domain.model

/** Un objetivo monitoreado puede ser una app completa o una función dentro de una app. */
enum class TargetType { APP, FEATURE }

/** Ventana durante la cual el feed queda bloqueado tras agotar la cuota de scroll. */
enum class QuotaWindow { UNTIL_RESET, COOLDOWN }

/** Motivo por el que un objetivo queda bloqueado. */
enum class BlockReason {
    NONE,
    SCHEDULE,
    COOLDOWN,
    SESSION_LIMIT,
    OPEN_LIMIT,
    TIME_LIMIT,
    SCROLL_QUOTA,
    NO_TOKENS,
    INSUFFICIENT_TOKENS
}
