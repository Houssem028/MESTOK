package com.mestok.app

import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Ink = Color(0xFF090A10)
private val Panel = Color(0xFF151722)
private val Purple = Color(0xFF8B5CF6)
private val Cyan = Color(0xFF42D9F5)
private val Muted = Color(0xFF9CA3B8)

class MainActivity : ComponentActivity() {
    private var refreshTick by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        render()
    }

    override fun onResume() {
        super.onResume()
        refreshTick++
    }

    private fun render() {
        setContent {
            val tick = refreshTick
            val prefs = getSharedPreferences("mestok_local", MODE_PRIVATE)
            val latestTitle = prefs.getString("last_title", null).orEmpty()
            val latestMessage = prefs.getString("last_message", null).orEmpty()
            val hasLatest = latestTitle.isNotBlank() || latestMessage.isNotBlank()

            MaterialTheme(colorScheme = darkColorScheme(
                background = Ink, surface = Panel, primary = Purple,
                secondary = Cyan, onBackground = Color.White, onSurface = Color.White
            )) {
                MestokHome(
                    latestTitle = latestTitle,
                    latestMessage = latestMessage,
                    hasLatest = hasLatest,
                    overlayEnabled = Settings.canDrawOverlays(this),
                    notificationEnabled = isNotificationAccessEnabled(),
                    onOverlayClick = {
                        startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
                    },
                    onNotificationClick = {
                        startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    },
                    onOpenTikTok = {
                        val tiktok = packageManager.getLaunchIntentForPackage("com.zhiliaoapp.musically")
                            ?: packageManager.getLaunchIntentForPackage("com.ss.android.ugc.trill")
                        tiktok?.let { startActivity(it) }
                    }
                )
            }
        }
    }

    private fun isNotificationAccessEnabled(): Boolean {
        val enabled = Settings.Secure.getString(contentResolver, "enabled_notification_listeners") ?: return false
        val component = ComponentName(this, TikTokNotificationListener::class.java)
        return enabled.split(":").any { it.equals(component.flattenToString(), true) }
    }
}

@Composable
private fun MestokHome(
    latestTitle: String,
    latestMessage: String,
    hasLatest: Boolean,
    overlayEnabled: Boolean,
    notificationEnabled: Boolean,
    onOverlayClick: () -> Unit,
    onNotificationClick: () -> Unit,
    onOpenTikTok: () -> Unit
) {
    Surface(Modifier.fillMaxSize(), color = Ink) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                Spacer(Modifier.height(24.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(painterResource(R.drawable.mestok_logo), "MESTOK logo",
                        Modifier.size(54.dp).clip(RoundedCornerShape(16.dp)))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("MESTOK", fontSize = 25.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                        Text("Stay in the moment.", color = Muted, fontSize = 12.sp)
                    }
                    Spacer(Modifier.weight(1f))
                    Box(Modifier.clip(CircleShape).background(Color(0xFF202333)).padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Text("BETA", color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(26.dp)) {
                    Box(Modifier.fillMaxWidth().background(
                        Brush.linearGradient(listOf(Color(0xFF29204A), Color(0xFF171A2A)))
                    ).padding(20.dp)) {
                        Column {
                            Text("خليك في الفيديو…", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(6.dp))
                            Text("رسائلك المهمة توصلك في فقاعة عائمة، من غير ما تقطع المشاهدة.",
                                color = Color(0xFFC7C8D8), fontSize = 14.sp, lineHeight = 21.sp)
                            Spacer(Modifier.height(18.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(42.dp).clip(CircleShape)
                                    .background(Brush.linearGradient(listOf(Cyan, Purple))),
                                    contentAlignment = Alignment.Center) {
                                    Text("M", color = Color.White, fontWeight = FontWeight.Black, fontSize = 22.sp)
                                }
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text("MESTOK bubble", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("تصلك فوق التطبيقات", color = Muted, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
            item {
                Text("آخر إشعار من TikTok", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        if (hasLatest) {
                            Text(latestTitle.ifBlank { "TikTok" }, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Spacer(Modifier.height(5.dp))
                            Text(latestMessage.ifBlank { "لا يوجد نص في هذا الإشعار." }, color = Muted, fontSize = 13.sp)
                            Spacer(Modifier.height(12.dp))
                            Button(onClick = onOpenTikTok, colors = ButtonDefaults.buttonColors(containerColor = Purple)) {
                                Text("فتح TikTok")
                            }
                        } else {
                            Text("ما وصلنا حتى إشعار بعد", fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(5.dp))
                            Text("فعّل الوصول للإشعارات، ثم انتظر إشعار TikTok جديداً.", color = Muted, fontSize = 12.sp)
                        }
                    }
                }
            }
            item {
                Text("جهّز MESTOK", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                PermissionRow("الوصول للإشعارات", "لاستقبال إشعارات TikTok التي يسمح بها النظام",
                    notificationEnabled, onNotificationClick)
                Spacer(Modifier.height(10.dp))
                PermissionRow("الظهور فوق التطبيقات", "لعرض فقاعة MESTOK أثناء المشاهدة",
                    overlayEnabled, onOverlayClick)
            }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF11131C))) {
                    Column(Modifier.padding(16.dp)) {
                        Text("الخصوصية والحدود", color = Cyan, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(5.dp))
                        Text("معاينة الإشعار تُحفظ محلياً على هاتفك. MESTOK لا يرسلها إلى خادم ولا يدخل إلى رسائلك الخاصة. الرد المباشر يحتاج إجراء رد يتيحه إشعار TikTok نفسه.",
                            color = Muted, fontSize = 12.sp, lineHeight = 18.sp)
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun PermissionRow(title: String, subtitle: String, enabled: Boolean, action: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp)) {
        Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(Modifier.height(3.dp))
                Text(subtitle, color = Muted, fontSize = 11.sp, lineHeight = 16.sp)
                Spacer(Modifier.height(7.dp))
                Text(if (enabled) "مفعّل" else "غير مفعّل",
                    color = if (enabled) Color(0xFF4ADE80) else Color(0xFFFBBF24),
                    fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Button(onClick = action, colors = ButtonDefaults.buttonColors(containerColor = Purple),
                shape = RoundedCornerShape(13.dp)) {
                Text(if (enabled) "إدارة" else "تفعيل", fontSize = 12.sp)
            }
        }
    }
}
