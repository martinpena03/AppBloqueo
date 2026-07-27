package com.freno.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.freno.app.core.PermissionsHelper
import com.freno.app.domain.model.BlockReason
import com.freno.app.domain.model.TargetType
import com.freno.app.domain.model.TargetUiStatus
import com.freno.app.domain.util.TimeUtils
import com.freno.app.ui.MainViewModel
import com.freno.app.ui.components.SectionCard

@Composable
fun DashboardScreen(
    vm: MainViewModel,
    onOpenPicker: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenTarget: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var tick by remember { mutableIntStateOf(0) }
    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) tick++ }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }
    val accessibilityOn = remember(tick) { PermissionsHelper.isAccessibilityEnabled(context) }
    val monitoring by vm.monitoringEnabled.collectAsState()
    val snapshot by vm.dashboard.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Freno",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            OutlinedButton(onClick = onOpenSettings) { Text("Ajustes") }
        }

        val snap = snapshot
        SectionCard {
            Text("Tokens restantes hoy", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = if (snap != null) "${snap.remainingTokens} / ${snap.dailyBudget}" else "—",
                fontFamily = FontFamily.Monospace,
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold
            )
            if (snap != null) {
                val countdown = TimeUtils.formatCountdown(snap.resetAt - System.currentTimeMillis())
                Text(
                    "Reinicia en $countdown  ·  ${TimeUtils.formatClock(snap.resetAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (!accessibilityOn) {
            WarningCard(
                text = "El servicio de accesibilidad está desactivado. El bloqueo no funcionará hasta reactivarlo.",
                actionText = "Reactivar",
                onAction = { context.startActivity(PermissionsHelper.accessibilitySettingsIntent()) }
            )
        }
        if (!monitoring) {
            WarningCard(text = "El monitoreo global está en pausa.", actionText = null, onAction = {})
        }

        Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 8.dp)) {
            Text(
                "Apps y funciones monitoreadas",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f).align(Alignment.CenterVertically)
            )
            Button(onClick = onOpenPicker) { Text("Agregar") }
        }

        val statuses = snap?.statuses ?: emptyList()
        if (statuses.isEmpty()) {
            Text(
                "Aún no monitoreas nada. Toca \"Agregar\" para elegir apps, o Reels/Shorts.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 12.dp)
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) {
                items(statuses, key = { it.targetId }) { st ->
                    TargetStatusRow(st) { onOpenTarget(st.targetId) }
                }
            }
        }
    }
}

@Composable
private fun WarningCard(text: String, actionText: String?, onAction: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
            if (actionText != null) {
                OutlinedButton(onClick = onAction) { Text(actionText) }
            }
        }
    }
}

@Composable
private fun TargetStatusRow(st: TargetUiStatus, onClick: () -> Unit) {
    SectionCard(modifier = Modifier.clickable { onClick() }) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(st.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                val kind = if (st.type == TargetType.FEATURE) "Función (scroll)" else "App"
                Text(kind, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            val stateText = when {
                !st.enabled -> "Pausada"
                st.blocked -> "Bloqueada"
                else -> "Disponible"
            }
            Text(stateText, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        }
        val details = buildString {
            append("Uso: ${st.usedMinutes} min  ·  Aperturas: ${st.opens}  ·  Tokens: ${st.tokensSpent}")
            if (st.type == TargetType.FEATURE && st.scrollQuota != null) {
                append("\nScroll: ${st.scrollCount} / ${st.scrollQuota}")
            }
            if (st.sessionLimitMin != null) {
                append("\nSesión: ${st.sessionMinutes} / ${st.sessionLimitMin} min")
            }
            if (st.blocked && st.availableAgainAt != null) {
                append("\n${reasonShort(st.reason)} · vuelve a las ${TimeUtils.formatClock(st.availableAgainAt)}")
            }
        }
        Text(details, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun reasonShort(reason: BlockReason): String = when (reason) {
    BlockReason.SCHEDULE -> "Horario"
    BlockReason.COOLDOWN -> "Cooldown"
    BlockReason.SESSION_LIMIT -> "Límite por sesión"
    BlockReason.OPEN_LIMIT -> "Límite de aperturas"
    BlockReason.TIME_LIMIT -> "Límite de tiempo"
    BlockReason.SCROLL_QUOTA -> "Cuota de scroll"
    BlockReason.NO_TOKENS -> "Sin tokens"
    BlockReason.INSUFFICIENT_TOKENS -> "Tokens insuficientes"
    BlockReason.NONE -> ""
}
