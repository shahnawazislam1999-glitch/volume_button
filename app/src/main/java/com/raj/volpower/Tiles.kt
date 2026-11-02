package com.raj.volpower

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

/** [ANCHOR:TILES] Quick Settings tiles - an extra way to reach the same actions. */
abstract class BaseTile : TileService() {
    abstract fun onTap()
    override fun onStartListening() {
        qsTile?.apply { state = Tile.STATE_INACTIVE; updateTile() }
    }
    override fun onClick() {
        onTap()
        ServiceStarter.refresh(this)
    }
}

class TileVolUp : BaseTile() { override fun onTap() = VolumeHelper.up(this) }
class TileVolDown : BaseTile() { override fun onTap() = VolumeHelper.down(this) }
class TileMute : BaseTile() { override fun onTap() = VolumeHelper.toggleMute(this) }
class TilePower : BaseTile() { override fun onTap() { PowerAccessibilityService.lockScreen(this) } }

class TileSlider : BaseTile() {
    override fun onTap() {}
    @Suppress("DEPRECATION")
    override fun onClick() {
        val i = Intent(this, SliderActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (Build.VERSION.SDK_INT >= 34) {
            startActivityAndCollapse(
                PendingIntent.getActivity(this, 0, i, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            )
        } else {
            startActivityAndCollapse(i)
        }
    }
}
