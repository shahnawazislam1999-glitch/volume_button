package com.raj.volpower

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast

/** [ANCHOR:POWER_SERVICE] Gives us lock-screen + power-menu without the hardware button. */
class PowerAccessibilityService : AccessibilityService() {

    companion object {
        @Volatile var instance: PowerAccessibilityService? = null

        private fun needEnable(c: Context) {
            Toast.makeText(c, "Enable \"Vol Power\" in Accessibility settings first", Toast.LENGTH_LONG).show()
        }

        fun lockScreen(c: Context): Boolean {
            val s = instance
            if (s == null) { needEnable(c); return false }
            return if (Build.VERSION.SDK_INT >= 28) s.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN) else false
        }

        fun powerMenu(c: Context): Boolean {
            val s = instance
            if (s == null) { needEnable(c); return false }
            return s.performGlobalAction(GLOBAL_ACTION_POWER_DIALOG)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        ServiceStarter.start(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Cheap self-heal: if the foreground service died, bring it back.
        if (KeepAliveService.instance == null) ServiceStarter.start(this)
    }

    override fun onInterrupt() {}

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        instance = null
        return super.onUnbind(intent)
    }
}
