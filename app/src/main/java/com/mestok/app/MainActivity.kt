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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(
                background = Ink, surface = Panel, primary = Purple,
                secondary = Cyan, onBackground = Color.White, onSurface = Color.White
            )) {
                MestokHome(
                    overlayEnabled = Settings.canDrawOverlays(this),
                    notificationEnabled = isNotificationAccessEnabled(),
                    onOverlayClick = {
                        startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
                    },
                    onNotificationClick = {
                        startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
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
    overlayEnabled: Boolean,
    notificationEnabled: Boolean,
    onOverlayClick: () -> Unit,
    onNotificationClick: () -> Unit
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
                Text("جهّز MESTOK", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                PermissionRow("الوصول للإشعارات", "لاستقبال إشعارات TikTok التي يسمح بها النظام",
                    notificationEnabled, onNotificationClick)
                Spacer(Modifier.height(10.dp))
                PermissionRow("الظهور فوق التطبيقات", "لعرض فقاعة MESTOK أثناء المشاهدة",
                    overlayEnabled, onOverlayClick)
            }
            item {
                Text("معاينة المحادثات", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                ChatPreview("سارة", "بعثتلك فيديو جديد 👀", "الآن", "S", Purple)
                Spacer(Modifier.height(9.dp))
                ChatPreview("Ahmed", "وينك؟ نستنا فيك", "2 د", "A", Cyan)
                Spacer(Modifier.height(9.dp))
                ChatPreview("MESTOK", "هنا تظهر إشعاراتك المدعومة", "تجريبي", "M", Color(0xFFE879F9))
            }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF11131C))) {
                    Column(Modifier.padding(16.dp)) {
                        Text("ملاحظة مهمة", color = Cyan, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(5.dp))
                        Text("MESTOK يعرض الإشعارات التي يرسلها TikTok فقط. الرد المباشر متاح عندما يحتوي الإشعار على إجراء رد تدعمه TikTok.",
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

@Composable
private fun ChatPreview(name: String, message: String, time: String, letter: String, tint: Color) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Panel).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(44.dp).clip(CircleShape).background(tint.copy(alpha = 0.22f)),
            contentAlignment = Alignment.Center) {
            Text(letter, color = tint, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(message, color = Muted, fontSize = 12.sp)
        }
        Text(time, color = Muted, fontSize = 10.sp)
    }
}
