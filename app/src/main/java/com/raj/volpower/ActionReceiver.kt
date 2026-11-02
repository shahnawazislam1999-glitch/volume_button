package com.raj.volpower

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** [ANCHOR:ACTION_RECEIVER] Handles every button tap from the notification. */
class ActionReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        when (i.action) {
            Actions.UP -> VolumeHelper.up(c)
            Actions.DOWN -> VolumeHelper.down(c)
            Actions.MUTE -> VolumeHelper.toggleMute(c)
            Actions.STREAM -> VolumeHelper.toggleStream(c)
            Actions.POWER -> PowerAccessibilityService.lockScreen(c)
            Actions.MENU -> PowerAccessibilityService.powerMenu(c)
            Actions.REPOST -> { /* notification swiped away: just re-post below */ }
        }
        ServiceStarter.refresh(c)
    }
}
