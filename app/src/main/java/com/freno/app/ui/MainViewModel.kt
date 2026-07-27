package com.freno.app.ui

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.freno.app.data.FeatureCatalog
import com.freno.app.data.entity.DayState
import com.freno.app.data.entity.MonitoredTarget
import com.freno.app.data.entity.PendingChange
import com.freno.app.di.Graph
import com.freno.app.domain.model.DashboardSnapshot
import com.freno.app.domain.model.TargetType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AppInfo(val packageName: String, val label: String)

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = Graph.repository
    private val settings = Graph.settings

    val onboardingDone: StateFlow<Boolean> =
        settings.onboardingDone.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val pinSet: StateFlow<Boolean> =
        settings.pinHash.map { it != null }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val reflectionMinutes: StateFlow<Int> =
        settings.reflectionMinutes.stateIn(viewModelScope, SharingStarted.Eagerly, 120)

    val monitoringEnabled: StateFlow<Boolean> =
        settings.monitoringEnabled.stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val targets: StateFlow<List<MonitoredTarget>> =
        repo.observeTargets().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pending: StateFlow<List<PendingChange>> =
        repo.observePending().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dayState: StateFlow<DayState?> =
        repo.observeDayState().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val dashboard: StateFlow<DashboardSnapshot?> =
        flow {
            while (true) {
                emit(repo.dashboardSnapshot())
                kotlinx.coroutines.delay(2000)
            }
        }.flowOn(Dispatchers.IO)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // ---- Selección de apps ----
    suspend fun loadInstalledApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        val ctx = getApplication<Application>()
        val pm = ctx.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        pm.queryIntentActivities(intent, 0)
            .mapNotNull { ri ->
                val pkg = ri.activityInfo.packageName
                if (pkg == ctx.packageName) null else AppInfo(pkg, ri.loadLabel(pm).toString())
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }

    val featureCatalog get() = FeatureCatalog.all

    // ---- Creación de objetivos (apretar => inmediato) ----
    fun createAppTarget(app: AppInfo, onReady: (String) -> Unit) = viewModelScope.launch {
        val id = app.packageName
        if (repo.getTarget(id) == null) {
            repo.addTargetImmediate(
                MonitoredTarget(
                    targetId = id,
                    type = TargetType.APP,
                    packageName = app.packageName,
                    displayName = app.label
                )
            )
        }
        onReady(id)
    }

    fun createFeatureTarget(feature: FeatureCatalog.FeatureDef, onReady: (String) -> Unit) = viewModelScope.launch {
        val id = "${feature.packageName}#${feature.featureKey}"
        if (repo.getTarget(id) == null) {
            repo.addTargetImmediate(
                MonitoredTarget(
                    targetId = id,
                    type = TargetType.FEATURE,
                    packageName = feature.packageName,
                    featureKey = feature.featureKey,
                    displayName = feature.displayName,
                    openCostTokens = 0,
                    perMinuteCostTokens = 0,
                    perScrollCost = 0,
                    scrollQuota = 5
                )
            )
        }
        onReady(id)
    }

    // ---- Edición ----
    fun saveConfig(target: MonitoredTarget, onResult: (appliedNow: Boolean) -> Unit) = viewModelScope.launch {
        val applied = repo.saveTargetConfig(target, reflectionMinutes.value)
        onResult(applied)
    }

    fun removeTarget(id: String, name: String, onDone: () -> Unit) = viewModelScope.launch {
        repo.removeTargetDeferred(id, name, reflectionMinutes.value)
        onDone()
    }

    fun changeBudget(newBudget: Int, onResult: (appliedNow: Boolean) -> Unit) = viewModelScope.launch {
        val applied = repo.changeBudget(newBudget, reflectionMinutes.value)
        onResult(applied)
    }

    fun setResetTime(hour: Int, minute: Int) = viewModelScope.launch { repo.setResetTime(hour, minute) }

    fun setMonitoring(enabled: Boolean) = viewModelScope.launch { settings.setMonitoringEnabled(enabled) }

    fun setReflectionMinutes(minutes: Int) = viewModelScope.launch { settings.setReflectionMinutes(minutes) }

    fun setPin(pin: String) = viewModelScope.launch { settings.setPin(pin) }

    fun completeOnboarding() = viewModelScope.launch { settings.setOnboardingDone(true) }

    fun cancelPending(change: PendingChange) = viewModelScope.launch { repo.cancelPending(change) }

    /** Aplica los cambios diferidos que ya vencieron (al abrir la app). */
    fun applyDue() = viewModelScope.launch { repo.applyDuePendingChanges() }

    fun applyDue() = viewModelScope.launch {
        repo.applyDuePendingChanges()
        repo.ensureDayState()
    }

    suspend fun verifyPin(pin: String): Boolean = settings.verifyPin(pin)

    suspend fun isPinSet(): Boolean = settings.isPinSet()

    suspend fun getTarget(id: String): MonitoredTarget? = repo.getTarget(id)
}
