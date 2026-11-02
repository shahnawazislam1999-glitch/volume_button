package com.raj.volpower

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

/** [ANCHOR:OEM_HELPER] OPPO / ColorOS / realme auto-start + battery screens (best effort). */
object OemHelper {
    private val candidates = listOf(
        "com.coloros.safecenter" to "com.coloros.safecenter.permission.startup.StartupAppListActivity",
        "com.coloros.safecenter" to "com.coloros.safecenter.startupapp.StartupAppListActivity",
        "com.oplus.battery" to "com.oplus.startupapp.view.StartupAppListActivity",
        "com.oplus.battery" to "com.oplus.powermanager.fuelgaue.PowerUsageModelActivity",
        "com.coloros.oppoguardelf" to "com.coloros.powermanager.fuelgaue.PowerUsageModelActivity",
        "com.coloros.safecenter" to "com.coloros.safecenter.permission.startup.FakeActivity",
        "com.coloros.safecenter" to "com.coloros.safecenter.permission.PermissionTopActivity"
    )

    fun openAutoStart(c: Context): Boolean {
        for ((pkg, cls) in candidates) {
            try {
                c.startActivity(Intent().setComponent(ComponentName(pkg, cls)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                return true
            } catch (_: Exception) {}
        }
        openAppInfo(c)
        return false
    }

    fun openAppInfo(c: Context) {
        c.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${c.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
