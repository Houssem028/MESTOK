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
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.TextView
import kotlin.math.abs

class TikTokNotificationListener : NotificationListenerService() {
    private var bubbleView: TextView? = null
    private var windowManager: WindowManager? = null
    private var bubbleParams: WindowManager.LayoutParams? = null

    override fun onListenerConnected() {
        super.onListenerConnected()
        // Pick up TikTok notifications that were already present when access was enabled.
        activeNotifications?.filter { isTikTokPackage(it.packageName) }
            ?.maxByOrNull { it.postTime }
            ?.let { saveNotification(it) }
        if (Settings.canDrawOverlays(this) && hasSavedNotification()) showBubble()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (!isTikTokPackage(sbn.packageName)) return
        saveNotification(sbn)
        if (Settings.canDrawOverlays(this)) showBubble()
    }

    private fun saveNotification(sbn: StatusBarNotification) {
        val extras = sbn.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val message = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()

        // Notification previews remain on this device; nothing is uploaded.
        getSharedPreferences("mestok_local", Context.MODE_PRIVATE).edit()
            .putString("last_title", title)
            .putString("last_message", message)
            .putLong("last_time", sbn.postTime)
            .apply()
    }

    private fun hasSavedNotification(): Boolean {
        val prefs = getSharedPreferences("mestok_local", Context.MODE_PRIVATE)
        return !prefs.getString("last_title", "").isNullOrBlank() ||
            !prefs.getString("last_message", "").isNullOrBlank()
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
            contentDescription = "اسحب فقاعة MESTOK أو اضغط لفتح التطبيق"
        }
        val params = WindowManager.LayoutParams(
            dp(54), dp(54), WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = resources.displayMetrics.widthPixels - dp(68)
            y = resources.displayMetrics.heightPixels / 2
        }
        bubble.setOnTouchListener { view, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    view.setTag(R.id.mestok_bubble_touch_x, event.rawX)
                    view.setTag(R.id.mestok_bubble_touch_y, event.rawY)
                    view.setTag(R.id.mestok_bubble_start_x, params.x)
                    view.setTag(R.id.mestok_bubble_start_y, params.y)
                    view.setTag(R.id.mestok_bubble_moved, false)
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val downX = (view.getTag(R.id.mestok_bubble_touch_x) as? Float) ?: event.rawX
                    val downY = (view.getTag(R.id.mestok_bubble_touch_y) as? Float) ?: event.rawY
                    val startX = (view.getTag(R.id.mestok_bubble_start_x) as? Int) ?: params.x
                    val startY = (view.getTag(R.id.mestok_bubble_start_y) as? Int) ?: params.y
                    val dx = event.rawX - downX
                    val dy = event.rawY - downY
                    if (abs(dx) > dp(5) || abs(dy) > dp(5)) view.setTag(R.id.mestok_bubble_moved, true)
                    params.x = (startX + dx.toInt()).coerceIn(0, resources.displayMetrics.widthPixels - dp(54))
                    params.y = (startY + dy.toInt()).coerceIn(0, resources.displayMetrics.heightPixels - dp(100))
                    try { wm.updateViewLayout(view, params) } catch (_: Exception) { }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val moved = view.getTag(R.id.mestok_bubble_moved) as? Boolean ?: false
                    if (!moved) {
                        val launch = packageManager.getLaunchIntentForPackage(packageName)
                        launch?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        if (launch != null) startActivity(launch)
                    }
                    true
                }
                else -> true
            }
        }
        try {
            wm.addView(bubble, params)
            bubbleView = bubble
            bubbleParams = params
        } catch (_: Exception) {
            bubbleView = null
            bubbleParams = null
        }
    }

    private fun removeBubble() {
        val view = bubbleView ?: return
        try { windowManager?.removeView(view) } catch (_: Exception) { }
        bubbleView = null
        bubbleParams = null
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        removeBubble()
        super.onDestroy()
    }
}
