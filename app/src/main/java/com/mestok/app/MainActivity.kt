package com.mestok.app

import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings as SettingsIcon
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import java.util.Locale
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
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
            var page by remember { mutableStateOf("welcome") }
            var notice by remember { mutableStateOf<String?>(null) }

            MaterialTheme(colorScheme = darkColorScheme(
                background = Ink, surface = Panel, primary = Purple,
                secondary = Cyan, onBackground = Color.White, onSurface = Color.White
            )) {
                when (page) {
                    "welcome" -> WelcomeScreen(onContinue = { page = "login" })
                    "login" -> LoginScreen(
                        onTikTok = { notice = localized(
                            "سيتم تفعيل تسجيل TikTok الرسمي بعد ربط Login Kit ومفاتيح المطوّر.",
                            "La connexion TikTok officielle sera activée après la configuration de Login Kit.",
                            "Official TikTok sign-in will be enabled after Login Kit and developer keys are configured."
                        ) }
                    )
                    "settings" -> SettingsScreen(
                        notificationEnabled = isNotificationAccessEnabled(),
                        overlayEnabled = Settings.canDrawOverlays(this),
                        onBack = { page = "home" },
                        onNotificationAccess = {
                            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                        },
                        onAppInfo = {
                            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.parse("package:$packageName")))
                        },
                        onOverlay = {
                            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:$packageName")))
                        },
                        onPrivacy = { page = "privacy" },
                        onAbout = { page = "about" }
                    )
                    "privacy" -> InfoScreen("الخصوصية",
                        "إشعارات TikTok التي يلتقطها MESTOK تُحفظ محلياً على هاتفك. لا نرفع محتوى الإشعارات إلى خادم في هذه النسخة. لا يمكن للتطبيق قراءة الرسائل التي لا تظهر في إشعارات النظام.",
                        { page = "settings" })
                    "about" -> InfoScreen("حول MESTOK",
                        "MESTOK تطبيق مرافق لمستخدمي TikTok، يهدف إلى تسهيل متابعة الإشعارات أثناء مشاهدة الفيديوهات والبث المباشر. هذه نسخة تجريبية قيد التطوير.",
                        { page = "settings" })
                    else -> HomeScreen(
                        latestTitle = latestTitle,
                        latestMessage = latestMessage,
                        hasLatest = hasLatest,
                        onSettings = { page = "settings" },
                        onOpenTikTok = { openTikTok() }
                    )
                }
                notice?.let { message ->
                    AlertDialog(
                        onDismissRequest = { notice = null },
                        title = { Text("MESTOK") },
                        text = { Text(message) },
                        confirmButton = { TextButton(onClick = { notice = null }) { Text("حسناً") } },
                        containerColor = Panel,
                        titleContentColor = Color.White,
                        textContentColor = Muted
                    )
                }
            }
        }
    }

    private fun openTikTok() {
        val intent = packageManager.getLaunchIntentForPackage("com.zhiliaoapp.musically")
            ?: packageManager.getLaunchIntentForPackage("com.ss.android.ugc.trill")
        intent?.let { startActivity(it) }
    }

    private fun isNotificationAccessEnabled(): Boolean {
        val enabled = Settings.Secure.getString(contentResolver, "enabled_notification_listeners") ?: return false
        val component = ComponentName(this, TikTokNotificationListener::class.java)
        return enabled.split(":").any { it.equals(component.flattenToString(), true) }
    }
}

private fun localized(ar: String, fr: String, en: String): String =
    when (Locale.getDefault().language.lowercase(Locale.ROOT)) {
        "fr" -> fr
        "en" -> en
        else -> ar
    }

@Composable
private fun WelcomeScreen(onContinue: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(1400)
        onContinue()
    }
    Box(Modifier.fillMaxSize().background(Color.Black).padding(18.dp),
        contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Image(painter = painterResource(id = R.mipmap.mestok_logo),
                contentDescription = "MESTOK",
                modifier = Modifier.fillMaxWidth().aspectRatio(1f).heightIn(max = 430.dp),
                contentScale = ContentScale.Fit)
            Text("MESTOK", fontSize = 34.sp, fontWeight = FontWeight.Black,
                letterSpacing = 4.sp, color = Color.White)
        }
    }
}

@Composable
private fun LoginScreen(onTikTok: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Color.Black).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center) {
        Text("MESTOK", fontSize = 34.sp, fontWeight = FontWeight.Black,
            letterSpacing = 4.sp, color = Color.White)
        Text(localized("تسجيل الدخول", "Connexion", "Log in"),
            fontSize = 25.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 24.dp, bottom = 10.dp))
        Text(localized("تابع باستخدام حساب TikTok الخاص بك",
            "Continuez avec votre compte TikTok",
            "Continue with your TikTok account"), color = Muted, fontSize = 14.sp)
        Button(onClick = onTikTok,
            modifier = Modifier.fillMaxWidth().padding(top = 28.dp).height(56.dp),
            shape = RoundedCornerShape(16.dp)) {
            Text(localized("المتابعة باستخدام TikTok",
                "Continuer avec TikTok", "Continue with TikTok"),
                fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}
@Composable
private fun AuthField(label: String, value: String, onValue: (String) -> Unit,
                      keyboard: KeyboardType = KeyboardType.Text, secret: Boolean = false) {
    Column(Modifier.fillMaxWidth().padding(bottom = 14.dp)) {
        Text(label, color = Muted, fontSize = 13.sp)
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(value, onValue, modifier = Modifier.fillMaxWidth(),
            singleLine = true, shape = RoundedCornerShape(14.dp),
            keyboardOptions = KeyboardOptions(keyboardType = keyboard),
            visualTransformation = if (secret) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Cyan, unfocusedBorderColor = Color(0xFF343747),
                focusedTextColor = Color.White, unfocusedTextColor = Color.White
            ))
    }
}

@Composable
private fun HomeScreen(latestTitle: String, latestMessage: String, hasLatest: Boolean,
                       onSettings: () -> Unit, onOpenTikTok: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize().background(Ink).padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item {
            Spacer(Modifier.height(22.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("MESTOK", fontSize = 26.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                    Text("Stay in the moment.", color = Muted, fontSize = 12.sp)
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onSettings, modifier = Modifier.size(48.dp)
                    .clip(CircleShape).background(Panel)) {
                    Icon(Icons.Default.SettingsIcon, "الإعدادات", tint = Cyan)
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(24.dp)) {
                Column(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Color(0xFF29204A), Panel))).padding(20.dp)) {
                    Text("خليك في الفيديو…", fontSize = 23.sp, fontWeight = FontWeight.Bold)
                    Text("إشعاراتك المهمة توصلك من غير ما تقطع المشاهدة.", color = Muted,
                        modifier = Modifier.padding(top = 8.dp), lineHeight = 21.sp)
                    Button(onClick = onOpenTikTok, modifier = Modifier.padding(top = 16.dp)) { Text("فتح TikTok") }
                }
            }
        }
        item {
            Text("آخر إشعار من TikTok", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Text(if (hasLatest) latestTitle.ifBlank { "TikTok" } else "لا يوجد إشعار بعد",
                        fontWeight = FontWeight.Bold)
                    Text(if (hasLatest) latestMessage.ifBlank { "لا يحتوي الإشعار على نص." }
                        else "فعّل صلاحية الإشعارات من ⚙️ الإعدادات.", color = Muted,
                        modifier = Modifier.padding(top = 6.dp), fontSize = 13.sp)
                }
            }
        }
        item {
            Text("ملاحظة", color = Cyan, fontWeight = FontWeight.Bold)
            Text("قراءة الإشعارات تعتمد على صلاحية Android، وبعض إشعارات TikTok لا تعرض محتوى الرسالة أو خيار الرد.",
                color = Muted, fontSize = 12.sp, lineHeight = 18.sp)
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun SettingsScreen(
    notificationEnabled: Boolean, overlayEnabled: Boolean,
    onBack: () -> Unit, onNotificationAccess: () -> Unit,
    onAppInfo: () -> Unit, onOverlay: () -> Unit,
    onPrivacy: () -> Unit, onAbout: () -> Unit
) {
    LazyColumn(Modifier.fillMaxSize().background(Ink).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "رجوع", tint = Color.White) }
                Text("الإعدادات", fontSize = 25.sp, fontWeight = FontWeight.Bold)
            }
        }
        item { Text("الأذونات", color = Cyan, fontWeight = FontWeight.Bold) }
        item { SettingItem("إشعارات TikTok", if (notificationEnabled) "مفعّلة" else "غير مفعّلة",
            if (notificationEnabled) "✓" else "!", onNotificationAccess) }
        item { SettingItem("الظهور فوق التطبيقات", if (overlayEnabled) "مفعّل" else "غير مفعّل",
            if (overlayEnabled) "✓" else "!", onOverlay) }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF252033)),
                shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Text("إذا ظهر لك «تم رفض الوصول إلى هذا التطبيق»", fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFD27A))
                    Text("1. اضغط «معلومات التطبيق» أدناه.\n2. افتح قائمة ⋮ أعلى الشاشة.\n3. اختر «السماح بالإعدادات المقيّدة / Allow restricted settings».\n4. ارجع إلى MESTOK ثم افتح «إشعارات TikTok» وفعّل MESTOK من القائمة.",
                        color = Muted, fontSize = 13.sp, lineHeight = 21.sp,
                        modifier = Modifier.padding(top = 8.dp))
                    OutlinedButton(onClick = onAppInfo, modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                        Text("فتح معلومات تطبيق MESTOK")
                    }
                }
            }
        }
        item { Text("عام", color = Cyan, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp)) }
        item { SettingItem("الخصوصية", "كيف نتعامل مع بياناتك", "›", onPrivacy) }
        item { SettingItem("حول التطبيق", "الإصدار التجريبي من MESTOK", "›", onAbout) }
    }
}

@Composable
private fun SettingItem(title: String, subtitle: String, trailing: String, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(16.dp)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(subtitle, color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
            }
            Text(trailing, color = Cyan, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun InfoScreen(title: String, body: String, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Ink).padding(22.dp)) {
        IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "رجوع", tint = Color.White) }
        Text(title, fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 24.dp))
        Text(body, color = Muted, fontSize = 15.sp, lineHeight = 24.sp, modifier = Modifier.padding(top = 16.dp))
    }
}
