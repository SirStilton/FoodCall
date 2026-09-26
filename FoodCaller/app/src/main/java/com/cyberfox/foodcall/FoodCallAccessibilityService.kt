package com.cyberfox.foodcall

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent

class FoodCallAccessibilityService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private var lastBounce = 0L

    private fun kioskEnabled(): Boolean {
        return getSharedPreferences(
            "foodcall_kiosk",
            MODE_PRIVATE
        ).getBoolean("enabled", false)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {

        if (!kioskEnabled()) return
        if (event == null) return

        val allowedTypes =
            event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
            event.eventType == AccessibilityEvent.TYPE_WINDOWS_CHANGED ||
            event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED

        if (!allowedTypes) return

        val pkg = event.packageName?.toString() ?: return

        // FoodCall selbst erlauben
        if (pkg == packageName) return

        // Tastaturen erlauben
        if (
            pkg.contains("inputmethod", true) ||
            pkg.contains("keyboard", true) ||
            pkg.contains("ime", true)
        ) {
            return
        }

        val now = System.currentTimeMillis()

        if (now - lastBounce < 250) {
            return
        }

        lastBounce = now

        // Recents / SystemUI / fremde App erkannt
        returnHome()
    }

    private fun returnHome() {

        try {
            performGlobalAction(GLOBAL_ACTION_HOME)
        } catch (_: Exception) {
        }

        handler.postDelayed(
            {
                try {
                    val intent =
                        Intent(
                            this,
                            MainActivity::class.java
                        )

                    intent.addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                    )

                    startActivity(intent)

                } catch (_: Exception) {
                }
            },
            80
        )
    }

    override fun onInterrupt() {
    }
}
