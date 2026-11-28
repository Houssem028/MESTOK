package com.mestok.app

import android.app.Notification
import android.app.RemoteInput
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.graphics.SweepGradient
import android.view.View
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import kotlin.math.abs

/**
 * Branded chat-head inspired by the official MESTOK neon mark:
 * white M, cyan/pink offset edges and a cyan-to-magenta neon ring.
 */
private class MESTOKBubbleView(context: Context) : View(context) {
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3.4f
        shader = SweepGradient(
            50f, 50f,
            intArrayOf(0xFF00E9F5.toInt(), 0xFF168BFF.toInt(), 0xFFB43BFF.toInt(),
                0xFFFF168B.toInt(), 0xFFFF168B.toInt(), 0xFF00E9F5.toInt()),
            null
        )
    }
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3.4f
        color = 0xFF00DDF5.toInt()
        maskFilter = android.graphics.BlurMaskFilter(9f, android.graphics.BlurMaskFilter.Blur.NORMAL)
    }
    private val markPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = android.graphics.Color.WHITE
    }
    private val cyanPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0xFF00E8F2.toInt()
    }
    private val pinkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0xFFFF075F.toInt()
    }
    private val mark = Path().apply {
        moveTo(13f, 76f)
        lineTo(25f, 35f)
        cubicTo(28f, 25f, 38f, 23f, 46f, 31f)
        lineTo(53f, 43f)
        lineTo(69f, 22f)
        cubicTo(71f, 19f, 74f, 18f, 78f, 18f)
        lineTo(84f, 18f)
        lineTo(85f, 28f)
        cubicTo(86f, 37f, 90f, 42f, 96f, 44f)
        lineTo(92f, 73f)
        cubicTo(90f, 83f, 82f, 87f, 72f, 88f)
        lineTo(72f, 51f)
        lineTo(55f, 69f)
        lineTo(43f, 52f)
        lineTo(36f, 77f)
        cubicTo(33f, 87f, 22f, 87f, 13f, 82f)
        close()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val side = minOf(width, height).toFloat()
        val save = canvas.save()
        canvas.scale(side / 100f, side / 100f)
        setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        canvas.drawCircle(50f, 50f, 44f, glowPaint)
        canvas.drawCircle(50f, 50f, 44f, ringPaint)
        canvas.save()
        canvas.translate(-1.5f, -0.5f)
        canvas.drawPath(mark, cyanPaint)
        canvas.restore()
        canvas.save()
        canvas.translate(1.8f, 1.8f)
        canvas.drawPath(mark, pinkPaint)
        canvas.restore()
        canvas.drawPath(mark, markPaint)
        canvas.restoreToCount(save)
    }
}

class TikTokNotificationListener : NotificationListenerService() {
    private var bubbleView: View? = null
    private var panelView: LinearLayout? = null
    private var windowManager: WindowManager? = null
    private var bubbleParams: WindowManager.LayoutParams? = null
    private var latestTitle: String = "TikTok"
    private var latestMessage: String = ""
    private var latestReplyAction: Notification.Action? = null

    override fun onListenerConnected() {
        super.onListenerConnected()
        // Restore the bubble only when a TikTok notification is already present.
        // Do not show an idle bubble merely because notification access was enabled.
        activeNotifications?.filter { isTikTokPackage(it.packageName) }
            ?.maxByOrNull { it.postTime }
            ?.let {
                saveNotification(it)
                latestReplyAction = findReplyAction(it.notification)
                if (Settings.canDrawOverlays(this)) showBubble()
            }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (!isTikTokPackage(sbn.packageName)) return
        saveNotification(sbn)
        latestReplyAction = findReplyAction(sbn.notification)
        // Show the floating bubble only when a new TikTok notification arrives.
        if (Settings.canDrawOverlays(this)) showBubble()
        // Refresh the preview if the user already has the MESTOK panel open.
        if (panelView != null) showPreviewPanel()
    }

    private fun findReplyAction(notification: Notification): Notification.Action? =
        notification.actions?.firstOrNull { !it.remoteInputs.isNullOrEmpty() }

    private fun saveNotification(sbn: StatusBarNotification) {
        val extras = sbn.notification.extras
        latestTitle = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
            .ifBlank { "TikTok" }
        latestMessage = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()

        getSharedPreferences("mestok_local", Context.MODE_PRIVATE).edit()
            .putString("last_title", latestTitle)
            .putString("last_message", latestMessage)
            .putLong("last_time", sbn.postTime)
            .apply()
    }

    private fun isTikTokPackage(packageName: String) =
        packageName == "com.zhiliaoapp.musically" || packageName == "com.ss.android.ugc.trill"

    private fun showBubble() {
        if (bubbleView != null) return
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        windowManager = wm
        val bubble = MESTOKBubbleView(this).apply {
            contentDescription = "فقاعة MESTOK العائمة"
            elevation = dp(8).toFloat()
        }
        val params = WindowManager.LayoutParams(
            dp(66), dp(66), WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = resources.displayMetrics.widthPixels - dp(76)
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
                    params.x = (startX + dx.toInt()).coerceIn(0, resources.displayMetrics.widthPixels - dp(66))
                    params.y = (startY + dy.toInt()).coerceIn(0, resources.displayMetrics.heightPixels - dp(100))
                    try { wm.updateViewLayout(view, params) } catch (_: Exception) { }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val moved = view.getTag(R.id.mestok_bubble_moved) as? Boolean ?: false
                    if (!moved) showPreviewPanel()
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

    private fun showPreviewPanel() {
        val wm = (getSystemService(WINDOW_SERVICE) as WindowManager)
        windowManager = wm
        panelView?.let {
            try { wm.removeView(it) } catch (_: Exception) { }
            panelView = null
        }

        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            background = GradientDrawable().apply {
                setColor(0xFF151722.toInt())
                cornerRadius = dp(20).toFloat()
                setStroke(dp(1), 0xFF42D9F5.toInt())
            }
        }
        val heading = TextView(this).apply {
            text = latestTitle.ifBlank { "TikTok" }
            textSize = 17f
            setTextColor(0xFFFFFFFF.toInt())
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
        val message = TextView(this).apply {
            text = latestMessage.ifBlank { "فقاعة MESTOK جاهزة. ستظهر هنا أحدث إشعارات TikTok عند وصولها." }
            textSize = 14f
            setTextColor(0xFFCDD2E2.toInt())
            setPadding(0, dp(8), 0, dp(12))
        }
        val replyInput = EditText(this).apply {
            hint = if (latestReplyAction != null) "اكتب رداً سريعاً…" else "الرد غير متاح في هذا الإشعار"
            setTextColor(0xFFFFFFFF.toInt())
            setHintTextColor(0xFF9CA3B8.toInt())
            textSize = 14f
            isSingleLine = true
            isEnabled = latestReplyAction != null
            setPadding(dp(10), dp(8), dp(10), dp(8))
            background = GradientDrawable().apply {
                setColor(0xFF252A3A.toInt())
                cornerRadius = dp(12).toFloat()
            }
        }
        val buttons = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
        }
        val closeButton = Button(this).apply {
            text = "إغلاق"
            setOnClickListener { removePanel() }
        }
        val sendButton = Button(this).apply {
            text = "إرسال الرد"
            isEnabled = latestReplyAction != null
            setOnClickListener {
                val reply = replyInput.text?.toString().orEmpty().trim()
                if (reply.isNotBlank()) sendQuickReply(reply)
            }
        }
        buttons.addView(closeButton)
        buttons.addView(sendButton)
        panel.addView(heading)
        panel.addView(message)
        panel.addView(replyInput)
        panel.addView(buttons)

        val params = WindowManager.LayoutParams(
            dp(300), WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = (resources.displayMetrics.widthPixels - dp(316)).coerceAtLeast(dp(8))
            y = (resources.displayMetrics.heightPixels / 2 - dp(130)).coerceAtLeast(dp(40))
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        }
        try {
            wm.addView(panel, params)
            panelView = panel
        } catch (_: Exception) {
            Toast.makeText(this, "تعذر فتح نافذة MESTOK. تحقق من إذن الظهور فوق التطبيقات.", Toast.LENGTH_LONG).show()
        }
    }

    private fun sendQuickReply(message: String) {
        val action = latestReplyAction
        val remoteInputs = action?.remoteInputs
        if (action == null || remoteInputs.isNullOrEmpty()) {
            Toast.makeText(this, "هذا الإشعار لا يدعم الرد السريع.", Toast.LENGTH_SHORT).show()
            return
        }
        val fillInIntent = Intent()
        val results = Bundle().apply {
            putCharSequence(remoteInputs[0].resultKey, message)
        }
        RemoteInput.addResultsToIntent(remoteInputs, fillInIntent, results)
        try {
            action.actionIntent.send(this, 0, fillInIntent)
            Toast.makeText(this, "تم إرسال طلب الرد إلى TikTok.", Toast.LENGTH_SHORT).show()
            removePanel()
        } catch (_: Exception) {
            Toast.makeText(this, "لم يقبل TikTok إرسال الرد من هذا الإشعار.", Toast.LENGTH_LONG).show()
        }
    }

    private fun removePanel() {
        val view = panelView ?: return
        try { windowManager?.removeView(view) } catch (_: Exception) { }
        panelView = null
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        removePanel()
        val view = bubbleView
        if (view != null) {
            try { windowManager?.removeView(view) } catch (_: Exception) { }
        }
        bubbleView = null
        bubbleParams = null
        super.onDestroy()
    }
}
