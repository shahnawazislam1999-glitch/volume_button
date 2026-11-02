package com.raj.volpower

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.drawable.GradientDrawable
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.*

/** [ANCHOR:SLIDER_ACTIVITY] Floating draggable volume slider (opened from the notification / tile). */
class SliderActivity : Activity() {

    private lateinit var seek: SeekBar
    private lateinit var title: TextView
    private val h = Handler(Looper.getMainLooper())
    private val closer = Runnable { finish() }

    private val rx = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, i: Intent?) = sync()
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val d = resources.displayMetrics.density
        fun dp(v: Int) = (v * d).toInt()

        val root = FrameLayout(this)
        root.setOnClickListener { finish() }

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(16), dp(20), dp(16))
            isClickable = true
            background = GradientDrawable().apply {
                setColor(0xF2202124.toInt())
                cornerRadius = dp(24).toFloat()
            }
        }

        title = TextView(this).apply {
            textSize = 18f
            setTextColor(0xFFFFFFFF.toInt())
            setPadding(0, 0, 0, dp(8))
        }
        card.addView(title)

        seek = SeekBar(this).apply {
            max = VolumeHelper.max(this@SliderActivity)
            setPadding(dp(8), dp(20), dp(8), dp(20))
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, p: Int, fromUser: Boolean) {
                    if (!fromUser) return
                    VolumeHelper.setLevel(this@SliderActivity, p)
                    title.text = VolumeHelper.label(this@SliderActivity)
                    ServiceStarter.refresh(this@SliderActivity)
                }
                override fun onStartTrackingTouch(s: SeekBar?) {}
                override fun onStopTrackingTouch(s: SeekBar?) {}
            })
        }
        card.addView(seek, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        fun btn(t: String, f: () -> Unit) = Button(this).apply {
            text = t
            setOnClickListener { f(); sync() }
        }.also { row.addView(it, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)) }

        btn("Mute") { VolumeHelper.toggleMute(this); ServiceStarter.refresh(this) }
        btn("Media/Ring") {
            VolumeHelper.toggleStream(this)
            seek.max = VolumeHelper.max(this)
            ServiceStarter.refresh(this)
        }
        btn("Lock") { PowerAccessibilityService.lockScreen(this); finish() }
        card.addView(row)

        val lp = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT,
            Gravity.TOP or Gravity.CENTER_HORIZONTAL).apply { setMargins(dp(16), dp(72), dp(16), 0) }
        root.addView(card, lp)
        setContentView(root)
    }

    private fun sync() {
        seek.max = VolumeHelper.max(this)
        seek.progress = if (VolumeHelper.isMuted(this)) 0 else VolumeHelper.current(this)
        title.text = VolumeHelper.label(this)
    }

    override fun onStart() {
        super.onStart()
        sync()
        val f = IntentFilter().apply {
            addAction("android.media.VOLUME_CHANGED_ACTION")
            addAction("android.media.STREAM_MUTE_CHANGED_ACTION")
            addAction(AudioManager.RINGER_MODE_CHANGED_ACTION)
        }
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(rx, f, Context.RECEIVER_EXPORTED)
        else registerReceiver(rx, f)
        resetTimer()
    }

    override fun onStop() {
        try { unregisterReceiver(rx) } catch (_: Exception) {}
        h.removeCallbacks(closer)
        finish()
        super.onStop()
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        resetTimer()
        return super.dispatchTouchEvent(ev)
    }

    private fun resetTimer() {
        h.removeCallbacks(closer)
        h.postDelayed(closer, 5000)
    }
}
