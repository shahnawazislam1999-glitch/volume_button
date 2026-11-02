package com.raj.volpower

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** [ANCHOR:BOOT_RECEIVER] Starts everything after reboot / app update. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        ServiceStarter.start(c)
        ServiceStarter.scheduleWatchdog(c)
    }
}
