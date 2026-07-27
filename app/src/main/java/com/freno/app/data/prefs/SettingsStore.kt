package com.freno.app.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.MessageDigest

private val Context.dataStore by preferencesDataStore(name = "freno_settings")

/** Ajustes ligeros: PIN (hasheado), onboarding, retardo de reflexión, monitoreo global. */
class SettingsStore(private val context: Context) {

    private object Keys {
        val PIN_HASH = stringPreferencesKey("pin_hash")
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val REFLECTION_MIN = intPreferencesKey("reflection_minutes")
        val MONITORING_ENABLED = booleanPreferencesKey("monitoring_enabled")
    }

    val pinHash: Flow<String?> = context.dataStore.data.map { it[Keys.PIN_HASH] }
    val onboardingDone: Flow<Boolean> = context.dataStore.data.map { it[Keys.ONBOARDING_DONE] ?: false }
    val reflectionMinutes: Flow<Int> = context.dataStore.data.map { it[Keys.REFLECTION_MIN] ?: DEFAULT_REFLECTION_MIN }
    val monitoringEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.MONITORING_ENABLED] ?: true }

    suspend fun setPin(pin: String) {
        context.dataStore.edit { it[Keys.PIN_HASH] = hash(pin) }
    }

    suspend fun verifyPin(pin: String): Boolean {
        val stored = context.dataStore.data.first()[Keys.PIN_HASH]
        return stored != null && stored == hash(pin)
    }

    suspend fun isPinSet(): Boolean = context.dataStore.data.first()[Keys.PIN_HASH] != null

    suspend fun setOnboardingDone(done: Boolean) {
        context.dataStore.edit { it[Keys.ONBOARDING_DONE] = done }
    }

    suspend fun setReflectionMinutes(minutes: Int) {
        context.dataStore.edit { it[Keys.REFLECTION_MIN] = minutes.coerceAtLeast(0) }
    }

    suspend fun setMonitoringEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.MONITORING_ENABLED] = enabled }
    }

    private fun hash(pin: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(pin.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    companion object {
        const val DEFAULT_REFLECTION_MIN = 120
    }
}
