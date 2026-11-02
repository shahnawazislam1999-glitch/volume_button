package com.raj.volpower

import android.content.Context
import android.media.AudioManager

/** [ANCHOR:VOLUME_HELPER] All volume logic lives here. */
object VolumeHelper {
    private const val PREF = "volpower"

    private fun prefs(c: Context) = c.getSharedPreferences(PREF, Context.MODE_PRIVATE)
    fun am(c: Context): AudioManager = c.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    fun isEnabled(c: Context) = prefs(c).getBoolean("enabled", true)
    fun setEnabled(c: Context, v: Boolean) = prefs(c).edit().putBoolean("enabled", v).apply()

    fun stream(c: Context) = prefs(c).getInt("stream", AudioManager.STREAM_MUSIC)

    fun toggleStream(c: Context) {
        val n = if (stream(c) == AudioManager.STREAM_MUSIC) AudioManager.STREAM_RING else AudioManager.STREAM_MUSIC
        prefs(c).edit().putInt("stream", n).apply()
    }

    fun streamName(c: Context) = if (stream(c) == AudioManager.STREAM_RING) "Ring" else "Media"
    fun max(c: Context): Int = am(c).getStreamMaxVolume(stream(c))
    fun current(c: Context): Int = am(c).getStreamVolume(stream(c))

    fun isMuted(c: Context): Boolean = try {
        am(c).isStreamMute(stream(c)) || current(c) == 0
    } catch (e: Exception) {
        current(c) == 0
    }

    fun label(c: Context): String {
        val m = isMuted(c)
        return "${streamName(c)} ${if (m) 0 else current(c)}/${max(c)}" + if (m) " (muted)" else ""
    }

    fun up(c: Context) = adjust(c, AudioManager.ADJUST_RAISE)
    fun down(c: Context) = adjust(c, AudioManager.ADJUST_LOWER)

    private fun adjust(c: Context, dir: Int) {
        try {
            am(c).adjustStreamVolume(stream(c), dir, 0)
        } catch (e: SecurityException) {
            val t = current(c) + if (dir == AudioManager.ADJUST_RAISE) 1 else -1
            setLevel(c, t.coerceIn(0, max(c)))
        }
    }

    fun setLevel(c: Context, v: Int) {
        try {
            am(c).setStreamVolume(stream(c), v.coerceIn(0, max(c)), 0)
        } catch (e: SecurityException) {
            // Do-Not-Disturb policy access missing (ring stream only)
        }
    }

    fun toggleMute(c: Context) {
        val s = stream(c)
        try {
            am(c).adjustStreamVolume(s, AudioManager.ADJUST_TOGGLE_MUTE, 0)
        } catch (e: SecurityException) {
            val p = prefs(c)
            if (current(c) > 0) {
                p.edit().putInt("last_$s", current(c)).apply()
                setLevel(c, 0)
            } else {
                setLevel(c, p.getInt("last_$s", max(c) / 2))
            }
        }
    }
}
