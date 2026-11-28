package com.mestok.app

import android.app.Notification
import android.app.PendingIntent
import android.app.RemoteInput
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.SweepGradient
import android.graphics.drawable.GradientDrawable
import android.os.Build
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
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import kotlin.math.abs

private class MESTOKBubbleView(context: Context) : android.view.View(context) {
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
    private val markPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.WHITE }
    private val cyanPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF00E8F2.toInt() }
    private val pinkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFF075F.toInt() }
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
        setLayerType(android.view.View.LAYER_TYPE_SOFTWARE, null)
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

private data class MestokConversation(
    val id: String,
    var title: String,
    var message: String,
    var postTime: Long,
    var notificationKey: String,
    var replyAction: Notification.Action?
)

private data class BubbleWindow(
    val view: android.view.View,
    val params: WindowManager.LayoutParams
)

class TikTokNotificationListener : NotificationListenerService() {
    private val conversations = LinkedHashMap<String, MestokConversation>()
    private val bubbles = LinkedHashMap<String, BubbleWindow>()
    private var panelView: LinearLayout? = null
    private var windowManager: WindowManager? = null
    private var selectedConversationId: String? = null

    override fun onListenerConnected() {
        super.onListenerConnected()
        conversations.clear()
        activeNotifications
            ?.filter { isTikTokPackage(it.packageName) }
            ?.sortedBy { it.postTime }
            ?.forEach { upsertNotification(it) }

        if (conversations.isNotEmpty() && Settings.canDrawOverlays(this)) {
            conversations.values.sortedByDescending { it.postTime }
                .take(MAX_VISIBLE_BUBBLES)
                .forEach { showBubble(it.id) }
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (!isTikTokPackage(sbn.packageName)) return
        val conversation = upsertNotification(sbn)
        if (Settings.canDrawOverlays(this)) showBubble(conversation.id)
        if (panelView != null) showConversationPanel(selectedConversationId ?: conversation.id)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        if (!isTikTokPackage(sbn.packageName)) return
        val id = conversationId(sbn)
        val existing = conversations[id] ?: return
        // Keep the visible conversation preview, but do not reuse a dismissed notification's
        // PendingIntent: it may already have been cancelled by Android/TikTok.
        if (existing.notificationKey == sbn.key) existing.replyAction = null
        if (panelView != null && selectedConversationId == id) showConversationPanel(id)
    }

    private fun upsertNotification(sbn: StatusBarNotification): MestokConversation {
        val extras = sbn.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)?.toString()
            ?.takeIf { it.isNotBlank() }
            ?: extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
                ?.takeIf { it.isNotBlank() }
            ?: "TikTok"
        val message = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
            ?.takeIf { it.isNotBlank() }
            ?: extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        val id = conversationId(sbn)
        val item = conversations[id] ?: MestokConversation(
            id = id,
            title = title,
            message = message,
            postTime = sbn.postTime,
            notificationKey = sbn.key,
            replyAction = null
        )
        item.title = title
        item.message = message
        item.postTime = sbn.postTime
        item.notificationKey = sbn.key
        item.replyAction = findReplyAction(sbn.notification)
        conversations[id] = item

        getSharedPreferences("mestok_local", Context.MODE_PRIVATE).edit()
            .putString("last_title", title)
            .putString("last_message", message)
            .putLong("last_time", sbn.postTime)
            .apply()
        return item
    }

    private fun conversationId(sbn: StatusBarNotification): String {
        val shortcutId = if (Build.VERSION.SDK_INT >= 30) sbn.notification.shortcutId else null
        if (!shortcutId.isNullOrBlank()) return "shortcut:$shortcutId"
        val title = sbn.notification.extras
            .getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)?.toString()
            ?.takeIf { it.isNotBlank() }
            ?: sbn.notification.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
                ?.takeIf { it.isNotBlank() }
            ?: "TikTok"
        return "title:${title.trim().lowercase()}"
    }

    private fun findReplyAction(notification: Notification): Notification.Action? {
        val actions = notification.actions ?: return null
        val candidates = actions.filter { action ->
            action.remoteInputs?.any { it.allowFreeFormInput && !it.isDataOnly } == true
        }
        if (candidates.isEmpty()) return null
        return if (Build.VERSION.SDK_INT >= 28) {
            candidates.firstOrNull { it.semanticAction == Notification.Action.SEMANTIC_ACTION_REPLY }
                ?: candidates.first()
        } else candidates.first()
    }

    private fun isTikTokPackage(packageName: String) =
        packageName == "com.zhiliaoapp.musically" || packageName == "com.ss.android.ugc.trill"

    private fun showBubble(conversationId: String) {
        if (bubbles.containsKey(conversationId)) return
        if (!Settings.canDrawOverlays(this)) return
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        windowManager = wm
        // Keep the overlay tidy: rotate the oldest visible head out when a sixth
        // conversation arrives. All conversations remain available in the panel list.
        if (bubbles.size >= MAX_VISIBLE_BUBBLES) {
            val oldestId = bubbles.keys.firstOrNull()
            val oldest = oldestId?.let { bubbles.remove(it) }
            if (oldest != null) {
                try { wm.removeView(oldest.view) } catch (_: Exception) { }
            }
        }
        val bubble = MESTOKBubbleView(this).apply {
            contentDescription = "فقاعة MESTOK: ${conversations[conversationId]?.title ?: "محادثة"}"
            elevation = dp(8).toFloat()
        }
        val index = bubbles.size.coerceAtMost(MAX_VISIBLE_BUBBLES - 1)
        val params = WindowManager.LayoutParams(
            dp(66), dp(66), WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = resources.displayMetrics.widthPixels - dp(76)
            y = (resources.displayMetrics.heightPixels / 3 + index * dp(74))
                .coerceAtMost(resources.displayMetrics.heightPixels - dp(100))
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
                    if (!moved) showConversationPanel(conversationId)
                    true
                }
                else -> true
            }
        }

        try {
            wm.addView(bubble, params)
            bubbles[conversationId] = BubbleWindow(bubble, params)
            repositionBubbles()
        } catch (_: Exception) {
            bubbles.remove(conversationId)
        }
    }

    private fun repositionBubbles() {
        val wm = windowManager ?: return
        bubbles.values.forEachIndexed { index, bubble ->
            bubble.params.y = (resources.displayMetrics.heightPixels / 3 + index * dp(74))
                .coerceAtMost(resources.displayMetrics.heightPixels - dp(100))
            try { wm.updateViewLayout(bubble.view, bubble.params) } catch (_: Exception) { }
        }
    }

    private fun showConversationPanel(conversationId: String) {
        val selected = conversations[conversationId] ?: return
        selectedConversationId = conversationId
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        windowManager = wm
        removePanel()

        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = GradientDrawable().apply {
                setColor(0xFF151722.toInt())
                cornerRadius = dp(20).toFloat()
                setStroke(dp(1), 0xFF42D9F5.toInt())
            }
        }
        val heading = TextView(this).apply {
            text = "محادثات MESTOK"
            textSize = 17f
            setTextColor(0xFFFFFFFF.toInt())
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(0, 0, 0, dp(8))
        }
        panel.addView(heading)

        val listScroll = ScrollView(this)
        val list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        conversations.values.sortedByDescending { it.postTime }.forEach { item ->
            val row = TextView(this).apply {
                text = "${if (item.id == conversationId) "● " else ""}${item.title}\n${item.message}"
                textSize = 13f
                setTextColor(if (item.id == conversationId) 0xFF42D9F5.toInt() else 0xFFE5E7EF.toInt())
                setPadding(dp(10), dp(9), dp(10), dp(9))
                background = GradientDrawable().apply {
                    setColor(if (item.id == conversationId) 0xFF252A3A.toInt() else 0xFF1C2030.toInt())
                    cornerRadius = dp(10).toFloat()
                }
                setOnClickListener { showConversationPanel(item.id) }
            }
            list.addView(row, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(6) })
        }
        listScroll.addView(list)
        panel.addView(listScroll, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(150)
        ))

        val message = TextView(this).apply {
            text = selected.message.ifBlank { "لا يعرض إشعار TikTok نص الرسالة." }
            textSize = 14f
            setTextColor(0xFFCDD2E2.toInt())
            setPadding(0, dp(10), 0, dp(8))
        }
        panel.addView(TextView(this).apply {
            text = selected.title
            textSize = 15f
            setTextColor(0xFFFFFFFF.toInt())
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(0, dp(8), 0, 0)
        })
        panel.addView(message)

        val replyInput = EditText(this).apply {
            hint = if (selected.replyAction != null) "اكتب رسالة…" else "الرد غير متاح لهذا الإشعار حالياً"
            setTextColor(0xFFFFFFFF.toInt())
            setHintTextColor(0xFF9CA3B8.toInt())
            textSize = 14f
            isSingleLine = true
            isEnabled = selected.replyAction != null
            setPadding(dp(10), dp(8), dp(10), dp(8))
            background = GradientDrawable().apply {
                setColor(0xFF252A3A.toInt())
                cornerRadius = dp(12).toFloat()
            }
        }
        panel.addView(replyInput)

        val note = TextView(this).apply {
            text = "الإرسال يستخدم إجراء الرد الذي يتيحه إشعار TikTok؛ وقد يربط TikTok الرسالة بآخر رسالة واردة. لا يثبت هذا وحده وصولها."
            textSize = 10f
            setTextColor(0xFF9CA3B8.toInt())
            setPadding(0, dp(6), 0, dp(4))
        }
        panel.addView(note)

        val buttons = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
        }
        val closeButton = Button(this).apply {
            text = "إغلاق"
            setOnClickListener { removePanel() }
        }
        val sendButton = Button(this).apply {
            text = "إرسال"
            isEnabled = selected.replyAction != null
            setOnClickListener {
                val text = replyInput.text?.toString().orEmpty().trim()
                if (text.isNotBlank()) sendQuickReply(conversationId, text)
            }
        }
        buttons.addView(closeButton)
        buttons.addView(sendButton)
        panel.addView(buttons)

        val params = WindowManager.LayoutParams(
            dp(310), WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = (resources.displayMetrics.widthPixels - dp(326)).coerceAtLeast(dp(8))
            y = (resources.displayMetrics.heightPixels / 2 - dp(230)).coerceAtLeast(dp(32))
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        }
        try {
            wm.addView(panel, params)
            panelView = panel
        } catch (_: Exception) {
            Toast.makeText(this, "تعذر فتح نافذة MESTOK. تحقق من إذن الظهور فوق التطبيقات.", Toast.LENGTH_LONG).show()
        }
    }

    private fun sendQuickReply(conversationId: String, message: String) {
        val conversation = conversations[conversationId]
        val action = conversation?.replyAction
        val remoteInputs = action?.remoteInputs
        if (action == null || remoteInputs.isNullOrEmpty()) {
            Toast.makeText(this, "لا يوجد إجراء رد صالح لهذا الشخص. انتظر إشعاراً جديداً منه.", Toast.LENGTH_LONG).show()
            return
        }
        val freeFormInputs = remoteInputs.filter { it.allowFreeFormInput && !it.isDataOnly }
        if (freeFormInputs.isEmpty()) {
            Toast.makeText(this, "TikTok لم يوفّر حقل كتابة نصي لهذا الإشعار.", Toast.LENGTH_LONG).show()
            return
        }

        val fillInIntent = Intent()
        val results = Bundle().apply {
            freeFormInputs.forEach { putCharSequence(it.resultKey, message) }
        }
        RemoteInput.addResultsToIntent(remoteInputs, fillInIntent, results)
        if (Build.VERSION.SDK_INT >= 28) {
            RemoteInput.setResultsSource(fillInIntent, RemoteInput.SOURCE_FREE_FORM)
        }

        try {
            action.actionIntent.send(this, 0, fillInIntent)
            Toast.makeText(
                this,
                "تم تمرير طلب الإرسال إلى TikTok. تحقق من المحادثة للتأكد من وصول الرسالة.",
                Toast.LENGTH_LONG
            ).show()
        } catch (_: PendingIntent.CanceledException) {
            conversation.replyAction = null
            Toast.makeText(this, "انتهت صلاحية إجراء الرد. انتظر إشعاراً جديداً من هذا الشخص.", Toast.LENGTH_LONG).show()
            showConversationPanel(conversationId)
        } catch (_: Exception) {
            Toast.makeText(this, "تعذر تمرير الرد إلى TikTok. جرّب بعد وصول إشعار جديد.", Toast.LENGTH_LONG).show()
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
        bubbles.values.forEach {
            try { windowManager?.removeView(it.view) } catch (_: Exception) { }
        }
        bubbles.clear()
        super.onDestroy()
    }

    companion object {
        private const val MAX_VISIBLE_BUBBLES = 5
    }
}
