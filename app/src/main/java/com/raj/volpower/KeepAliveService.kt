package com.raj.volpower

import android.app.*
import android.content.*
import android.content.pm.ServiceInfo
import android.database.ContentObserver
import android.media.AudioManager
import android.os.*
import android.provider.Settings
import android.util.Log
import android.widget.RemoteViews

/** [ANCHOR:SERVICE_STARTER] Helpers to (re)start the service and schedule the watchdog. */
object ServiceStarter {
    fun start(c: Context) {
        if (!VolumeHelper.isEnabled(c)) return
        try {
            c.startForegroundService(Intent(c, KeepAliveService::class.java))
        } catch (e: Exception) {
            Log.w("VolPower", "start failed: $e")
        }
    }

    fun refresh(c: Context) {
        val s = KeepAliveService.instance
        if (s != null) s.refresh() else start(c)
    }

    fun scheduleWatchdog(c: Context) {
        try {
            val js = c.getSystemService(Context.JOB_SCHEDULER_SERVICE) as android.app.job.JobScheduler
            if (js.getPendingJob(4242) != null) return
            val job = android.app.job.JobInfo.Builder(4242, ComponentName(c, WatchdogJobService::class.java))
                .setPeriodic(15 * 60 * 1000L)
                .setPersisted(true)
                .build()
            js.schedule(job)
        } catch (e: Exception) {
            Log.w("VolPower", "watchdog failed: $e")
        }
    }
}

/** [ANCHOR:KEEP_ALIVE_SERVICE] Foreground service that owns the notification-drawer controls. */
class KeepAliveService : Service() {

    companion object {
        const val CHANNEL = "vp_channel"
        const val ID = 1001
        @Volatile var instance: KeepAliveService? = null
    }

    private lateinit var nm: NotificationManager
    private val handler = Handler(Looper.getMainLooper())
    private val doRefresh = Runnable { nm.notify(ID, buildNotification()) }

    private val rx = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, i: Intent?) = refresh()
    }
    private val obs = object : ContentObserver(handler) {
        override fun onChange(selfChange: Boolean) = refresh()
    }

    override fun onBind(i: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        createChannel()
        val n = buildNotification()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(ID, n)
        }
        val f = IntentFilter().apply {
            addAction("android.media.VOLUME_CHANGED_ACTION")
            addAction("android.media.STREAM_MUTE_CHANGED_ACTION")
            addAction(AudioManager.RINGER_MODE_CHANGED_ACTION)
        }
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(rx, f, Context.RECEIVER_EXPORTED)
        else registerReceiver(rx, f)
        contentResolver.registerContentObserver(Settings.System.CONTENT_URI, true, obs)
        ServiceStarter.scheduleWatchdog(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        refresh()
        return START_STICKY
    }

    /** Debounced (40 ms) so rapid volume changes don't spam the notification manager. */
    fun refresh() {
        handler.removeCallbacks(doRefresh)
        handler.postDelayed(doRefresh, 40)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        scheduleRestart()
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        instance = null
        try { unregisterReceiver(rx) } catch (_: Exception) {}
        try { contentResolver.unregisterContentObserver(obs) } catch (_: Exception) {}
        if (VolumeHelper.isEnabled(this)) scheduleRestart()
        super.onDestroy()
    }

    /** [ANCHOR:SCHEDULE_RESTART] Alarm that brings the service back after it is killed. */
    private fun scheduleRestart() {
        if (!VolumeHelper.isEnabled(this)) return
        try {
            val pi = PendingIntent.getForegroundService(
                this, 77, Intent(this, KeepAliveService::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            (getSystemService(ALARM_SERVICE) as AlarmManager)
                .set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + 1500, pi)
        } catch (e: Exception) {
            Log.w("VolPower", "restart alarm failed: $e")
        }
    }

    private fun createChannel() {
        val ch = NotificationChannel(CHANNEL, "Volume & power controls", NotificationManager.IMPORTANCE_DEFAULT).apply {
            setSound(null, null)
            enableVibration(false)
            setShowBadge(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        nm.createNotificationChannel(ch)
    }

    private fun bc(action: String, code: Int): PendingIntent = PendingIntent.getBroadcast(
        this, code, Intent(this, ActionReceiver::class.java).setAction(action),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    private fun sliderPi(): PendingIntent = PendingIntent.getActivity(
        this, 50,
        Intent(this, SliderActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    /** [ANCHOR:BUILD_NOTIFICATION] Builds the collapsed + expanded drawer layouts. */
    private fun buildNotification(): Notification {
        val small = RemoteViews(packageName, R.layout.notif_small)
        val big = RemoteViews(packageName, R.layout.notif_big)
        val label = VolumeHelper.label(this)
        val max = VolumeHelper.max(this)
        val cur = if (VolumeHelper.isMuted(this)) 0 else VolumeHelper.current(this)

        small.setTextViewText(R.id.txt_level, label)
        big.setTextViewText(R.id.txt_level, label)
        big.setProgressBar(R.id.progress, max, cur, false)

        for (v in listOf(small, big)) {
            v.setOnClickPendingIntent(R.id.btn_down, bc(Actions.DOWN, 1))
            v.setOnClickPendingIntent(R.id.btn_up, bc(Actions.UP, 2))
            v.setOnClickPendingIntent(R.id.btn_mute, bc(Actions.MUTE, 3))
            v.setOnClickPendingIntent(R.id.btn_slider, sliderPi())
            v.setOnClickPendingIntent(R.id.btn_power, bc(Actions.POWER, 5))
        }
        big.setOnClickPendingIntent(R.id.btn_stream, bc(Actions.STREAM, 4))
        big.setOnClickPendingIntent(R.id.btn_menu, bc(Actions.MENU, 6))

        val open = PendingIntent.getActivity(
            this, 60, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val b = Notification.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setStyle(Notification.DecoratedCustomViewStyle())
            .setCustomContentView(small)
            .setCustomBigContentView(big)
            .setContentIntent(open)
            .setDeleteIntent(bc(Actions.REPOST, 99))
        if (Build.VERSION.SDK_INT >= 31) {
            b.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
        }
        return b.build()
    }
}
