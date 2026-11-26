package com.irecstudio.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import com.irecstudio.state.OverlayStateHolder

class BrowserAccessibilityService : AccessibilityService() {

    private val browserPackages = setOf(
        "com.android.chrome",
        "com.chrome.beta",
        "com.sec.android.app.sbrowser",
        "org.mozilla.firefox",
        "com.brave.browser",
        "com.opera.browser",
        "com.microsoft.emmx"
    )

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val pkg = event.packageName?.toString() ?: return
            if (pkg == "com.android.systemui") return
            OverlayStateHolder.setBrowserActive(pkg in browserPackages)
        }
    }

    override fun onInterrupt() {}
}