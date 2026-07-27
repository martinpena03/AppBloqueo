package com.freno.app.service

import android.accessibilityservice.AccessibilityService
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import com.freno.app.data.entity.FeatureSignature
import com.freno.app.di.Graph
import com.freno.app.domain.model.BlockDecision
import com.freno.app.domain.model.TargetType
import com.freno.app.service.detect.FeatureDetector
import com.freno.app.ui.BlockActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

/**
 * Corazón del sistema. Detecta la app/función en primer plano, cuenta swipes en Reels/Shorts y dispara
 * el bloqueo cuando corresponde. El manejo se serializa en un dispatcher de un solo hilo.
 */
class MonitorAccessibilityService : AccessibilityService() {

    // Un solo hilo => el manejo de eventos se serializa y evita carreras sobre currentTargetId.
    private val worker = Executors.newSingleThreadExecutor()
    private val scope = CoroutineScope(SupervisorJob() + worker.asCoroutineDispatcher())

    @Volatile private var currentTargetId: String? = null
    @Volatile private var serviceRunning = false
    private var lastPackage: String? = null
    private var lastFeatureKey: String? = null
    private var lastDetectAt = 0L
    private var lastScrollAt = 0L
    private var lastSwitchPkg: String? = "__init__"
    private var lastSwitchFeature: String? = null

    @Volatile private var signaturesByPkg: Map<String, List<FeatureSignature>> = emptyMap()

    override fun onServiceConnected() {
        super.onServiceConnected()
        scope.launch { reloadSignatures() }
    }

    private suspend fun reloadSignatures() {
        if (!Graph.isReady) return
        signaturesByPkg = Graph.repository.getSignatures().groupBy { it.packageName }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (!Graph.isReady) return
        val pkg = event.packageName?.toString() ?: return

        when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_SCROLLED -> onScrollEvent()
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> onWindowEvent(pkg)
        }
    }

    private fun onWindowEvent(pkg: String) {
        // Nuestras propias ventanas (pantalla de bloqueo / app) = salir del objetivo.
        if (pkg == packageName) {
            maybeSwitch(null, null)
            return
        }
        maybeSwitch(pkg, detectThrottled(pkg))
    }

    private fun detectThrottled(pkg: String): String? {
        val nowUp = SystemClock.uptimeMillis()
        if (pkg == lastPackage && nowUp - lastDetectAt < DETECT_THROTTLE_MS) return lastFeatureKey
        lastDetectAt = nowUp
        lastPackage = pkg
        val sigs = signaturesByPkg[pkg]
        val detected = if (sigs.isNullOrEmpty()) null else FeatureDetector.detect(rootInActiveWindow, sigs)
        lastFeatureKey = detected
        return detected
    }

    /** Deduplica: solo cambia de objetivo cuando el par (paquete, función) realmente cambia. */
    private fun maybeSwitch(pkg: String?, feature: String?) {
        if (pkg == lastSwitchPkg && feature == lastSwitchFeature) return
        lastSwitchPkg = pkg
        lastSwitchFeature = feature
        switchTarget(pkg, feature)
    }

    private fun switchTarget(pkg: String?, featureKey: String?) {
        scope.launch {
            val target = if (pkg == null) null else Graph.repository.resolveActiveTarget(pkg, featureKey)
            val newId = target?.targetId
            if (newId == currentTargetId) return@launch

            val prev = currentTargetId
            currentTargetId = newId
            if (prev != null) {
                Graph.repository.onBackground(prev)
            }
            if (target == null) {
                stopService()
                return@launch
            }

            when (val decision = Graph.repository.onForeground(newId!!)) {
                is BlockDecision.Block -> {
                    stopService()
                    Graph.repository.onBackground(newId)
                    currentTargetId = null
                    blockNow(decision)
                }
                BlockDecision.Allow -> {
                    startService(newId)
                }
            }
        }
    }

    private fun startService(id: String) {
        MonitoringService.start(this, id)
        serviceRunning = true
    }

    private fun stopService() {
        if (serviceRunning) {
            MonitoringService.stop(this)
            serviceRunning = false
        }
    }

    private fun onScrollEvent() {
        val id = currentTargetId ?: return
        val nowUp = SystemClock.uptimeMillis()
        if (nowUp - lastScrollAt < SCROLL_DEBOUNCE_MS) return
        lastScrollAt = nowUp

        scope.launch {
            val target = Graph.repository.getTarget(id) ?: return@launch
            if (target.type != TargetType.FEATURE) return@launch
            val decision = Graph.repository.onScroll(id)
            if (decision is BlockDecision.Block) {
                stopService()
                Graph.repository.onBackground(id)
                currentTargetId = null
                blockNow(decision)
            }
        }
    }

    private fun blockNow(decision: BlockDecision.Block) {
        BlockActivity.launch(applicationContext, decision)
    }

    override fun onInterrupt() { /* no-op */ }

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        currentTargetId = null
        stopService()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        worker.shutdownNow()
        super.onDestroy()
    }

    companion object {
        private const val DETECT_THROTTLE_MS = 350L
        private const val SCROLL_DEBOUNCE_MS = 500L
    }
}
