package com.mestok.app

import android.app.Notification
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.view.Gravity
import android.view.WindowManager
import android.widget.TextView

class TikTokNotificationListener : NotificationListenerService() {
    private var bubbleView: TextView? = null
    private var windowManager: WindowManager? = null

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (!isTikTokPackage(sbn.packageName)) return
        val extras = sbn.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val message = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()

        // Notification previews remain on this device; nothing is uploaded.
        getSharedPreferences("mestok_local", Context.MODE_PRIVATE).edit()
            .putString("last_title", title)
            .putString("last_message", message)
            .putLong("last_time", sbn.postTime)
            .apply()

        if (Settings.canDrawOverlays(this)) showBubble()
    }

    private fun isTikTokPackage(packageName: String) =
        packageName == "com.zhiliaoapp.musically" || packageName == "com.ss.android.ugc.trill"

    private fun showBubble() {
        if (bubbleView != null) return
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        windowManager = wm
        val bubble = TextView(this).apply {
            text = "M"
            textSize = 22f
            setTextColor(0xFFFFFFFF.toInt())
            gravity = Gravity.CENTER
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(0xFF42D9F5.toInt(), 0xFF8B5CF6.toInt(), 0xFFE879F9.toInt())
            ).apply { shape = GradientDrawable.OVAL }
            elevation = 12f
            contentDescription = "Open MESTOK"
            setOnClickListener {
                val launch = packageManager.getLaunchIntentForPackage(packageName)
                launch?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (launch != null) startActivity(launch)
                removeBubble()
            }
        }
        val params = WindowManager.LayoutParams(
            dp(54), dp(54), WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            x = dp(12)
            y = 0
        }
        try {
            wm.addView(bubble, params)
            bubbleView = bubble
        } catch (_: Exception) {
            bubbleView = null
        }
    }

    private fun removeBubble() {
        val view = bubbleView ?: return
        try { windowManager?.removeView(view) } catch (_: Exception) { }
        bubbleView = null
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        removeBubble()
        super.onDestroy()
    }
}
