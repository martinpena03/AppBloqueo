package com.freno.app.ui

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.freno.app.ui.components.PinDialog
import com.freno.app.ui.screens.DashboardScreen
import com.freno.app.ui.screens.OnboardingScreen
import com.freno.app.ui.screens.SettingsScreen
import com.freno.app.ui.screens.TargetConfigScreen
import com.freno.app.ui.screens.TargetPickerScreen
import com.freno.app.ui.theme.FrenoTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FrenoTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AppRoot()
                }
            }
        }
    }
}

private const val DASHBOARD = "dashboard"
private const val PICKER = "picker"
private const val CONFIG = "config"
private const val SETTINGS = "settings"

@Composable
private fun AppRoot() {
    val vm: MainViewModel = viewModel()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val onboardingDone by vm.onboardingDone.collectAsState()
    val pinSet by vm.pinSet.collectAsState()

    LaunchedEffect(Unit) { vm.applyDue() }

    var route by rememberSaveable { mutableStateOf(DASHBOARD) }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }

    var pendingRoute by remember { mutableStateOf<String?>(null) }
    var pendingId by remember { mutableStateOf<String?>(null) }
    var showPin by remember { mutableStateOf(false) }

    if (!onboardingDone) {
        OnboardingScreen(vm, onFinish = { route = DASHBOARD })
        return
    }

    fun go(r: String, id: String? = null) {
        route = r
        selectedId = id
    }

    fun goProtected(r: String, id: String? = null) {
        if (pinSet) {
            pendingRoute = r
            pendingId = id
            showPin = true
        } else {
            go(r, id)
        }
    }

    when (route) {
        DASHBOARD -> DashboardScreen(
            vm = vm,
            onOpenPicker = { goProtected(PICKER) },
            onOpenSettings = { goProtected(SETTINGS) },
            onOpenTarget = { id -> goProtected(CONFIG, id) }
        )
        PICKER -> TargetPickerScreen(
            vm = vm,
            onBack = { go(DASHBOARD) },
            onTargetReady = { id -> go(CONFIG, id) }
        )
        CONFIG -> {
            val id = selectedId
            if (id == null) go(DASHBOARD) else TargetConfigScreen(vm, id, onBack = { go(DASHBOARD) })
        }
        SETTINGS -> SettingsScreen(vm, onBack = { go(DASHBOARD) })
    }

    BackHandler(enabled = route != DASHBOARD) { go(DASHBOARD) }

    if (showPin) {
        PinDialog(
            title = "Ingresa tu PIN",
            onDismiss = {
                showPin = false
                pendingRoute = null
                pendingId = null
            },
            onSubmit = { pin ->
                scope.launch {
                    if (vm.verifyPin(pin)) {
                        showPin = false
                        val r = pendingRoute
                        val id = pendingId
                        pendingRoute = null
                        pendingId = null
                        if (r != null) go(r, id)
                    } else {
                        Toast.makeText(context, "PIN incorrecto.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }
}
