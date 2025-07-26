package io.github.arashiyama11.tinybudget

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.launch

class AppLaunchAccessibilityService: AccessibilityService() {
    private val monitoredPackages = mutableSetOf("jp.ne.paypay.android.app")
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

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
        if (monitoredPackages.contains(pkg)) {
            scope.launch {
                launchRequestFlow.emit(pkg)
            }
        }
    }

    private val launchRequestFlow = MutableSharedFlow<String>()

    @OptIn(FlowPreview::class)
    private fun startWatchRequest(){
        scope.launch {
            launchRequestFlow.sample(500).collect {
                Log.d("AppLaunchAccessibilityService", "Starting OverlayService")
                val intent = Intent(this@AppLaunchAccessibilityService, OverlayService::class.java)
                startForegroundService(intent)
            }
        }
    }



    override fun onInterrupt() { /* NOP */ }

    fun addMonitor(pkg: String)    { monitoredPackages += pkg }
    fun removeMonitor(pkg: String) { monitoredPackages -= pkg }
}