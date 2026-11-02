package com.raj.volpower

import android.Manifest
import android.app.Activity
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.ViewGroup
import android.widget.*

/** [ANCHOR:MAIN_ACTIVITY] Setup screen: grant permissions, check status. */
class MainActivity : Activity() {

    private lateinit var status: TextView

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val d = resources.displayMetrics.density
        fun dp(v: Int) = (v * d).toInt()

        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(32), dp(16), dp(24))
        }
        val sv = ScrollView(this).apply { addView(col) }

        col.addView(TextView(this).apply { text = "Vol Power"; textSize = 26f })
        status = TextView(this).apply { textSize = 15f; setPadding(0, dp(12), 0, dp(12)) }
        col.addView(status)

        fun btn(t: String, f: () -> Unit) {
            col.addView(Button(this).apply { text = t; setOnClickListener { try { f() } catch (e: Exception) { toast("Not available: $e") } } },
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }

        btn("1. Allow notifications") {
            if (Build.VERSION.SDK_INT >= 33) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
            else startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName))
        }
        btn("2. Battery: don't optimize (allow)") {
            startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:$packageName")))
        }
        btn("3. OPPO Auto-start / background (open)") { OemHelper.openAutoStart(this) }
        btn("4. App info (Allow restricted settings, battery)") { OemHelper.openAppInfo(this) }
        btn("5. Accessibility: enable Vol Power (lock / power)") {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        btn("6. Do Not Disturb access (for Ring mute)") {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
        }
        btn("Start / restart service") {
            VolumeHelper.setEnabled(this, true); ServiceStarter.start(this); updateStatus()
        }
        btn("Open volume slider") { startActivity(Intent(this, SliderActivity::class.java)) }
        btn("Stop service (disable keep-alive)") {
            VolumeHelper.setEnabled(this, false)
            stopService(Intent(this, KeepAliveService::class.java))
            updateStatus()
        }

        col.addView(TextView(this).apply {
            textSize = 14f
            setPadding(0, dp(16), 0, 0)
            text = "OPPO / ColorOS checklist:\n" +
                "- Recent apps -> long-press Vol Power -> Lock (padlock) so 'clear all' keeps it.\n" +
                "- Settings -> Battery -> App battery management -> Vol Power -> Allow background activity + Allow auto-launch.\n" +
                "- Settings -> Apps -> App management -> Vol Power -> Battery usage -> Allow background.\n" +
                "- Android 13+: App info -> top-right menu -> 'Allow restricted settings' BEFORE enabling Accessibility.\n" +
                "- Swipe down twice -> pencil -> add tiles: Volume +, Volume -, Mute, Vol slider, Lock screen."
        })
        setContentView(sv)
    }

    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_LONG).show()

    override fun onResume() {
        super.onResume()
        ServiceStarter.start(this)
        updateStatus()
        status.postDelayed({ updateStatus() }, 800)
    }

    private fun updateStatus() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        fun m(ok: Boolean) = if (ok) "✅" else "❌"
        status.text =
            "${m(KeepAliveService.instance != null)} Service running\n" +
            "${m(nm.areNotificationsEnabled())} Notifications allowed\n" +
            "${m(pm.isIgnoringBatteryOptimizations(packageName))} Battery optimization off\n" +
            "${m(PowerAccessibilityService.instance != null)} Accessibility (lock / power menu)\n" +
            "${m(nm.isNotificationPolicyAccessGranted)} Do Not Disturb access (optional)"
    }
}
