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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.freno.app.domain.util.TimeUtils
import com.freno.app.ui.MainViewModel
import com.freno.app.ui.components.FrenoTopBar
import com.freno.app.ui.components.SectionCard

@Composable
fun SettingsScreen(vm: MainViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val day by vm.dayState.collectAsState()
    val reflection by vm.reflectionMinutes.collectAsState()
    val monitoring by vm.monitoringEnabled.collectAsState()
    val pending by vm.pending.collectAsState()

    var budget by remember { mutableStateOf("") }
    var resetH by remember { mutableStateOf("0") }
    var resetM by remember { mutableStateOf("0") }
    var reflectionField by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var initialized by remember { mutableStateOf(false) }

    LaunchedEffect(day, reflection) {
        if (!initialized && day != null) {
            budget = day!!.dailyBudget.toString()
            resetH = day!!.resetHour.toString()
            resetM = day!!.resetMinute.toString()
            reflectionField = reflection.toString()
            initialized = true
        }
    }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        FrenoTopBar(title = "Ajustes", onBack = onBack)

        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {

            SectionCard {
                Text("Presupuesto diario de tokens", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Num("Tokens por día", budget) { budget = it }
                Button(
                    onClick = {
                        val b = budget.toIntOrNull() ?: return@Button
                        vm.changeBudget(b) { appliedNow ->
                            val msg = if (appliedNow) "Presupuesto actualizado."
                            else "Subir el presupuesto afloja: se aplicará tras el periodo de reflexión."
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Guardar presupuesto") }
            }

            SectionCard {
                Text("Hora de reinicio diario", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = resetH,
                        onValueChange = { if (it.all(Char::isDigit) && it.length <= 2) resetH = it },
                        label = { Text("Hora") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(110.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    OutlinedTextField(
                        value = resetM,
                        onValueChange = { if (it.all(Char::isDigit) && it.length <= 2) resetM = it },
                        label = { Text("Min") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(110.dp)
                    )
                }
                Button(
                    onClick = {
                        val h = (resetH.toIntOrNull() ?: 0).coerceIn(0, 23)
                        val m = (resetM.toIntOrNull() ?: 0).coerceIn(0, 59)
                        vm.setResetTime(h, m)
                        Toast.makeText(context, "Hora de reinicio: ${TimeUtils.formatHm(h * 60 + m)}", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Guardar hora") }
            }

            SectionCard {
                Text("Rigidez", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("Monitoreo global activo", modifier = Modifier.weight(1f))
                    Switch(checked = monitoring, onCheckedChange = { vm.setMonitoring(it) })
                }
                Num("Periodo de reflexión (minutos)", reflectionField) { reflectionField = it }
                OutlinedButton(
                    onClick = {
                        val v = reflectionField.toIntOrNull() ?: return@OutlinedButton
                        vm.setReflectionMinutes(v)
                        Toast.makeText(context, "Periodo de reflexión: $v min", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Guardar periodo") }
            }

            SectionCard {
                Text("Cambiar PIN", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 8 && it.all(Char::isDigit)) pin = it },
                    label = { Text("Nuevo PIN (mín. 4)") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedButton(
                    onClick = {
                        vm.setPin(pin)
                        pin = ""
                        Toast.makeText(context, "PIN actualizado.", Toast.LENGTH_SHORT).show()
                    },
                    enabled = pin.length >= 4,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Actualizar PIN") }
            }

            if (pending.isNotEmpty()) {
                SectionCard {
                    Text("Cambios pendientes", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    pending.forEach { change ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.weight(1f)) {
                                Text(change.description, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    "Se aplica a las ${TimeUtils.formatClock(change.applyAt)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            OutlinedButton(onClick = { vm.cancelPending(change) }) { Text("Cancelar") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Num(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { if (it.all(Char::isDigit) && it.length <= 6) onChange(it) },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth()
    )
}
