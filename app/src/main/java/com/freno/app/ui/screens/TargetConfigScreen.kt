package com.freno.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.freno.app.data.entity.MonitoredTarget
import com.freno.app.domain.model.QuotaWindow
import com.freno.app.domain.model.TargetType
import com.freno.app.ui.MainViewModel
import com.freno.app.ui.components.FrenoTopBar
import com.freno.app.ui.components.SectionCard

@Composable
fun TargetConfigScreen(vm: MainViewModel, targetId: String, onBack: () -> Unit) {
    val context = LocalContext.current
    var base by remember { mutableStateOf<MonitoredTarget?>(null) }
    var loaded by remember { mutableStateOf(false) }

    // Estados editables
    var enabled by remember { mutableStateOf(true) }
    var openCost by remember { mutableStateOf("5") }
    var perMin by remember { mutableStateOf("1") }
    var perScroll by remember { mutableStateOf("0") }
    var quota by remember { mutableStateOf("5") }
    var quotaCooldown by remember { mutableStateOf(false) }
    var quotaHours by remember { mutableStateOf("2") }

    var timeLimitOn by remember { mutableStateOf(false) }
    var timeLimit by remember { mutableStateOf("30") }
    var sessionLimitOn by remember { mutableStateOf(false) }
    var sessionLimit by remember { mutableStateOf("10") }
    var openLimitOn by remember { mutableStateOf(false) }
    var openLimit by remember { mutableStateOf("5") }
    var cooldownOn by remember { mutableStateOf(false) }
    var cooldown by remember { mutableStateOf("120") }
    var schedOn by remember { mutableStateOf(false) }
    var schedStart by remember { mutableStateOf("1320") } // 22:00
    var schedEnd by remember { mutableStateOf("600") }    // 10:00

    LaunchedEffect(targetId) {
        val t = vm.getTarget(targetId) ?: run { onBack(); return@LaunchedEffect }
        base = t
        enabled = t.enabled
        openCost = t.openCostTokens.toString()
        perMin = t.perMinuteCostTokens.toString()
        perScroll = t.perScrollCost.toString()
        quota = (t.scrollQuota ?: 5).toString()
        quotaCooldown = t.quotaWindow == QuotaWindow.COOLDOWN
        quotaHours = t.quotaWindowHours.toString()
        timeLimitOn = t.dailyTimeLimitMin != null
        timeLimit = (t.dailyTimeLimitMin ?: 30).toString()
        sessionLimitOn = t.sessionLimitMin != null
        sessionLimit = (t.sessionLimitMin ?: 10).toString()
        openLimitOn = t.dailyOpenLimit != null
        openLimit = (t.dailyOpenLimit ?: 5).toString()
        cooldownOn = t.cooldownMin != null
        cooldown = (t.cooldownMin ?: 120).toString()
        schedOn = t.scheduleStart != null && t.scheduleEnd != null
        if (t.scheduleStart != null) schedStart = t.scheduleStart.toString()
        if (t.scheduleEnd != null) schedEnd = t.scheduleEnd.toString()
        loaded = true
    }

    val t = base
    if (!loaded || t == null) {
        Column(Modifier.fillMaxSize().padding(24.dp)) { Text("Cargando…") }
        return
    }

    val isFeature = t.type == TargetType.FEATURE

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
    ) {
        FrenoTopBar(title = t.displayName, onBack = onBack)

        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {

            SectionCard {
                SwitchRow("Monitoreo activo", enabled) { enabled = it }
            }

            SectionCard {
                Text("Costos de tokens", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                NumberField("Costo por abrir", openCost) { openCost = it }
                NumberField("Costo por minuto", perMin) { perMin = it }
                if (isFeature) NumberField("Costo por reel/short", perScroll) { perScroll = it }
            }

            if (isFeature) {
                SectionCard {
                    Text("Cuota de scroll", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    NumberField("Reels/shorts permitidos antes de bloquear", quota) { quota = it }
                    SwitchRow("Bloquear solo por unas horas (en vez de hasta el reinicio)", quotaCooldown) { quotaCooldown = it }
                    if (quotaCooldown) NumberField("Horas de bloqueo", quotaHours) { quotaHours = it }
                }
            }

            SectionCard {
                Text("Reglas duras", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)

                SwitchRow("Límite de tiempo diario", timeLimitOn) { timeLimitOn = it }
                if (timeLimitOn) NumberField("Minutos por día", timeLimit) { timeLimit = it }

                SwitchRow("Uso máximo por sesión", sessionLimitOn) { sessionLimitOn = it }
                if (sessionLimitOn) {
                    NumberField("Minutos por sesión", sessionLimit) { sessionLimit = it }
                    val gap = cooldown.toIntOrNull()?.takeIf { cooldownOn } ?: 5
                    Text(
                        "Al agotarse se bloquea y empieza el cooldown. Para iniciar una sesión nueva " +
                            "hay que estar $gap min fuera de la app.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                SwitchRow("Límite de aperturas diarias", openLimitOn) { openLimitOn = it }
                if (openLimitOn) NumberField("Aperturas por día", openLimit) { openLimit = it }

                SwitchRow("Cooldown tras cerrar", cooldownOn) { cooldownOn = it }
                if (cooldownOn) NumberField("Minutos de cooldown", cooldown) { cooldown = it }

                SwitchRow("Bloqueo por horario", schedOn) { schedOn = it }
                if (schedOn) {
                    TimeRow("Desde", schedStart) { schedStart = it }
                    TimeRow("Hasta", schedEnd) { schedEnd = it }
                    Text(
                        "Formato: minutos desde medianoche (ej. 1320 = 22:00). Soporta cruce de medianoche.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Button(
                onClick = {
                    val updated = t.copy(
                        enabled = enabled,
                        openCostTokens = openCost.toIntOrNull() ?: 0,
                        perMinuteCostTokens = perMin.toIntOrNull() ?: 0,
                        perScrollCost = if (isFeature) perScroll.toIntOrNull() ?: 0 else 0,
                        scrollQuota = if (isFeature) quota.toIntOrNull() else null,
                        quotaWindow = if (quotaCooldown) QuotaWindow.COOLDOWN else QuotaWindow.UNTIL_RESET,
                        quotaWindowHours = quotaHours.toIntOrNull() ?: 2,
                        dailyTimeLimitMin = if (timeLimitOn) timeLimit.toIntOrNull() else null,
                        sessionLimitMin = if (sessionLimitOn) sessionLimit.toIntOrNull() else null,
                        dailyOpenLimit = if (openLimitOn) openLimit.toIntOrNull() else null,
                        cooldownMin = if (cooldownOn) cooldown.toIntOrNull() else null,
                        scheduleStart = if (schedOn) schedStart.toIntOrNull()?.coerceIn(0, 1439) else null,
                        scheduleEnd = if (schedOn) schedEnd.toIntOrNull()?.coerceIn(0, 1439) else null
                    )
                    vm.saveConfig(updated) { appliedNow ->
                        val msg = if (appliedNow) "Cambios aplicados."
                        else "Como aflojan restricciones, se aplicarán tras el periodo de reflexión."
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        onBack()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Guardar") }

            OutlinedButton(
                onClick = {
                    vm.removeTarget(t.targetId, t.displayName) {
                        Toast.makeText(context, "Se dejará de monitorear tras el periodo de reflexión.", Toast.LENGTH_LONG).show()
                        onBack()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Dejar de monitorear") }
        }
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun NumberField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { if (it.all(Char::isDigit) && it.length <= 5) onChange(it) },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun TimeRow(label: String, minutesValue: String, onChange: (String) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(label, modifier = Modifier.width(64.dp), style = MaterialTheme.typography.bodyMedium)
        OutlinedTextField(
            value = minutesValue,
            onValueChange = { if (it.all(Char::isDigit) && it.length <= 4) onChange(it) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.width(120.dp)
        )
        Spacer(Modifier.width(12.dp))
        val mins = minutesValue.toIntOrNull()?.coerceIn(0, 1439) ?: 0
        Text(com.freno.app.domain.util.TimeUtils.formatHm(mins), style = MaterialTheme.typography.bodyMedium)
    }
}
