package com.raj.volpower

import android.app.job.JobParameters
import android.app.job.JobService

/** [ANCHOR:WATCHDOG] Runs every ~15 min (survives reboot) and revives the service if killed. */
class WatchdogJobService : JobService() {
    override fun onStartJob(p: JobParameters?): Boolean {
        if (KeepAliveService.instance == null) ServiceStarter.start(this)
        return false
    }
    override fun onStopJob(p: JobParameters?): Boolean = true
}
