package com.freno.app.ui.screens

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.freno.app.core.PermissionsHelper
import com.freno.app.ui.MainViewModel
import com.freno.app.ui.components.SectionCard

@Composable
fun OnboardingScreen(vm: MainViewModel, onFinish: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var tick by remember { mutableIntStateOf(0) }

    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) tick++ }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    val accessOk = remember(tick) { PermissionsHelper.isAccessibilityEnabled(context) }
    val overlayOk = remember(tick) { PermissionsHelper.canDrawOverlays(context) }
    val notifOk = remember(tick) { PermissionsHelper.hasNotificationPermission(context) }
    val batteryOk = remember(tick) { PermissionsHelper.isIgnoringBatteryOptimizations(context) }
    val pinSet by vm.pinSet.collectAsState()

    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { tick++ }

    var pin by remember { mutableStateOf("") }
    var pin2 by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Freno", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
        Text(
            "Configura los permisos necesarios. Freno funciona 100% en tu teléfono, sin enviar datos a ningún servidor.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        StepCard(
            title = "1. Accesibilidad (obligatorio)",
            description = "Permite detectar qué app está en primer plano y aplicar el bloqueo. Freno leerá la pantalla solo localmente para detectar Reels/Shorts.",
            done = accessOk,
            buttonText = "Abrir ajustes de accesibilidad",
            onClick = { context.startActivity(PermissionsHelper.accessibilitySettingsIntent()) }
        )

        StepCard(
            title = "2. Superposición (obligatorio)",
            description = "Permite mostrar la pantalla de bloqueo sobre otras apps.",
            done = overlayOk,
            buttonText = "Permitir superposición",
            onClick = { context.startActivity(PermissionsHelper.overlaySettingsIntent(context)) }
        )

        StepCard(
            title = "3. Notificaciones (recomendado)",
            description = "Para la notificación del servicio de monitoreo.",
            done = notifOk,
            buttonText = "Permitir notificaciones",
            onClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notifLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        )

        StepCard(
            title = "4. Batería sin restricciones (recomendado)",
            description = "Evita que el sistema detenga el monitoreo en segundo plano.",
            done = batteryOk,
            buttonText = "Configurar batería",
            onClick = { context.startActivity(PermissionsHelper.batteryOptimizationIntent(context)) }
        )

        SectionCard {
            Text(
                "5. PIN de protección" + if (pinSet) "  ✓" else "",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "Necesario para cambiar reglas. Los cambios que aflojan restricciones se aplican con retardo.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = pin,
                onValueChange = { if (it.length <= 8 && it.all(Char::isDigit)) pin = it },
                label = { Text("PIN (mín. 4 dígitos)") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = pin2,
                onValueChange = { if (it.length <= 8 && it.all(Char::isDigit)) pin2 = it },
                label = { Text("Repetir PIN") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = { vm.setPin(pin) },
                enabled = pin.length >= 4 && pin == pin2,
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (pinSet) "Actualizar PIN" else "Guardar PIN") }
        }

        Button(
            onClick = {
                vm.completeOnboarding()
                onFinish()
            },
            enabled = accessOk && overlayOk && pinSet,
            modifier = Modifier.fillMaxWidth()
        ) { Text("Comenzar") }

        if (!(accessOk && overlayOk && pinSet)) {
            Text(
                "Faltan pasos obligatorios (accesibilidad, superposición y PIN).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StepCard(
    title: String,
    description: String,
    done: Boolean,
    buttonText: String,
    onClick: () -> Unit
) {
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(
                title + if (done) "  ✓" else "",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
        }
        Text(
            description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (!done) {
            OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) { Text(buttonText) }
        }
    }
}
