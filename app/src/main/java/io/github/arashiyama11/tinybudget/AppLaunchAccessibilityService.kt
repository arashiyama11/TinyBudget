package io.github.arashiyama11.tinybudget

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import io.github.arashiyama11.tinybudget.data.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.launch

class AppLaunchAccessibilityService : AccessibilityService() {
    private lateinit var settingsRepository: SettingsRepository
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        settingsRepository = (application as TinyBudgetApp).appContainer.settingsRepository
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        val info = serviceInfo
        info.apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 0
            packageNames = null
        }
        serviceInfo = info
        startWatchRequest()
    }

    @OptIn(FlowPreview::class)
    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val pkg = event.packageName?.toString() ?: return
        Log.d("AppLaunchAccessibilityService", "Package launched: $pkg, Event: $event")
        scope.launch {
            val lastDestroyed = settingsRepository.overlayDestroyedAt.first() ?: 0L
            if (System.currentTimeMillis() - lastDestroyed < 5000) {
                Log.d(
                    "AppLaunchAccessibilityService",
                    "Skipping overlay start because it was recently destroyed"
                )
                return@launch
            }

            val triggerApps = settingsRepository.triggerApps.first()
            Log.d("AppLaunchAccessibilityService", "Package: $pkg, Trigger Apps: $triggerApps")
            if (triggerApps.contains(pkg)) {
                launchRequestFlow.emit(pkg)
            }
        }
    }

    private val launchRequestFlow = MutableSharedFlow<String>()

    @OptIn(FlowPreview::class)
    private fun startWatchRequest() {
        scope.launch {
            launchRequestFlow.sample(500).collect {
                Log.d("AppLaunchAccessibilityService", "Starting OverlayService")
                val intent = Intent(this@AppLaunchAccessibilityService, OverlayService::class.java)
                startForegroundService(intent)
            }
        }
    }


    override fun onInterrupt() { /* NOP */
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
