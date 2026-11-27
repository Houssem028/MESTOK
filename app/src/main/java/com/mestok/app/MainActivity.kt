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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.LayoutDirection
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
    private var pendingTikTokResult by mutableStateOf<String?>(null)
    private var pendingNavigation by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleTikTokCallback(intent)
        render()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleTikTokCallback(intent)
    }

    private fun handleTikTokCallback(callbackIntent: Intent?) {
        val callbackUri = callbackIntent?.data ?: return
        val isVerifiedWebLink = callbackUri.scheme == "https" &&
            callbackUri.host == "houssem028.github.io" &&
            callbackUri.path.orEmpty().startsWith("/mestok/auth/callback")
        val isAppSchemeLink = callbackUri.scheme == "mestok" &&
            callbackUri.host == "auth" && callbackUri.path == "/callback"
        if (!isVerifiedWebLink && !isAppSchemeLink) return

        val loginStatus = callbackUri.getQueryParameter("login")
        val authorizationError = callbackUri.getQueryParameter("error")
        if (loginStatus == "success") {
            pendingNavigation = "home"
            val displayName = callbackUri.getQueryParameter("display_name").orEmpty()
            getSharedPreferences("mestok_local", MODE_PRIVATE).edit()
                .putString("tiktok_display_name", displayName)
                .putString("tiktok_avatar_url", callbackUri.getQueryParameter("avatar_url").orEmpty())
                .putBoolean("tiktok_linked", true)
                .putBoolean("mestok_logged_in", true)
                .apply()
            pendingTikTokResult = localized(
                "تم ربط حساب TikTok بنجاح: $displayName",
                "Compte TikTok connecté avec succès : $displayName",
                "TikTok account connected successfully: $displayName"
            )
        } else {
            pendingTikTokResult = if (!authorizationError.isNullOrBlank()) localized(
                "لم يكتمل تفويض TikTok. يمكنك المحاولة لاحقاً.",
                "L'autorisation TikTok n'a pas abouti. Réessaie plus tard.",
                "TikTok authorization was not completed. You can try again later."
            ) else localized(
                "لم تصل نتيجة تسجيل الدخول إلى MESTOK.",
                "Le résultat de connexion n'est pas arrivé dans MESTOK.",
                "The sign-in result did not reach MESTOK."
            )
        }
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
            var page by remember { mutableStateOf(if (prefs.getBoolean("tiktok_linked", false)) "home" else "welcome") }
            var settingsReturnPage by remember { mutableStateOf("login") }
            var notice by remember { mutableStateOf<String?>(null) }

            LaunchedEffect(pendingTikTokResult, pendingNavigation) {
                pendingNavigation?.let {
                    page = it
                    pendingNavigation = null
                }
                pendingTikTokResult?.let {
                    notice = it
                    pendingTikTokResult = null
                }
            }

            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            MaterialTheme(colorScheme = darkColorScheme(
                background = Ink, surface = Panel, primary = Purple,
                secondary = Cyan, onBackground = Color.White, onSurface = Color.White
            )) {
                when (page) {
                    "welcome" -> WelcomeScreen(onContinue = { page = "login" })
                    "login" -> LoginScreen(
                        onSettings = { settingsReturnPage = "login"; page = "settings" },
                        onGuest = { page = "home" },
                        onTikTok = {
                            startActivity(Intent(Intent.ACTION_VIEW,
                                Uri.parse("https://mestok-tiktok-auth.mmzhoussem.workers.dev/oauth")))
                        }
                    )
                    "settings" -> SettingsScreen(
                        notificationEnabled = isNotificationAccessEnabled(),
                        overlayEnabled = Settings.canDrawOverlays(this),
                        onBack = { page = settingsReturnPage },
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
                        isAccountLinked = prefs.getBoolean("tiktok_linked", false),
                        onLogout = {
                            prefs.edit().remove("tiktok_display_name")
                                .remove("tiktok_avatar_url")
                                .putBoolean("tiktok_linked", false)
                                .putBoolean("mestok_logged_in", false).apply()
                            page = "login"
                            settingsReturnPage = "login"
                            notice = "تم تسجيل الخروج من حسابك."
                        },
                        onPrivacy = { page = "privacy" },
                        onTerms = { page = "terms" },
                        onSupport = { page = "support" },
                        onAbout = { page = "about" }
                    )
                    "privacy" -> InfoScreen("الخصوصية",
                        "إشعارات TikTok التي يلتقطها MESTOK تُحفظ محلياً على هاتفك. لا نرفع محتوى الإشعارات إلى خادم في هذه النسخة. لا يمكن للتطبيق قراءة الرسائل التي لا تظهر في إشعارات النظام.",
                        { page = "settings" },
                        "https://houssem028.github.io/privacy.html", "قراءة سياسة الخصوصية الكاملة")
                    "terms" -> InfoScreen("شروط الاستخدام",
                        "MESTOK تطبيق تجريبي مستقل. استخدامه يخضع لشروط الاستخدام المنشورة على موقعنا.",
                        { page = "settings" },
                        "https://houssem028.github.io/terms.html", "قراءة شروط الاستخدام")
                    "support" -> InfoScreen("الدعم والمساعدة",
                        "للمساعدة والإبلاغ عن مشكلة، يمكنك زيارة صفحة الدعم الرسمية.",
                        { page = "settings" },
                        "https://houssem028.github.io/support.html", "فتح صفحة الدعم")
                    "about" -> InfoScreen("حول MESTOK",
                        "MESTOK تطبيق مرافق لمستخدمي TikTok، يهدف إلى تسهيل متابعة الإشعارات أثناء مشاهدة الفيديوهات والبث المباشر. هذه نسخة تجريبية قيد التطوير.",
                        { page = "settings" })
                    else -> HomeScreen(
                        latestTitle = latestTitle,
                        latestMessage = latestMessage,
                        hasLatest = hasLatest,
                        tiktokDisplayName = prefs.getString("tiktok_display_name", null).orEmpty(),
                        tiktokAvatarUrl = prefs.getString("tiktok_avatar_url", null).orEmpty(),
                        onSettings = { settingsReturnPage = "home"; page = "settings" },
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
            Image(painter = painterResource(id = R.drawable.mestok_logo),
                contentDescription = "MESTOK",
                modifier = Modifier.fillMaxWidth().aspectRatio(1f).heightIn(max = 430.dp),
                contentScale = ContentScale.Fit)
            Text("MESTOK", fontSize = 34.sp, fontWeight = FontWeight.Black,
                letterSpacing = 4.sp, color = Color.White)
        }
    }
}

@Composable
private fun LoginScreen(onTikTok: () -> Unit, onSettings: () -> Unit, onGuest: () -> Unit) {
    val uriHandler = LocalUriHandler.current

    Column(
        Modifier.fillMaxSize().background(Color.White).padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(24.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("MESTOK", color = Color(0xFF111111), fontSize = 25.sp,
                fontWeight = FontWeight.Black, letterSpacing = 3.sp)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onSettings) {
                Icon(Icons.Default.SettingsIcon, contentDescription = "الإعدادات",
                    tint = Color(0xFF252033))
            }
        }
        Spacer(Modifier.weight(1f))
        Text("سجّل الدخول باستخدام TikTok", color = Color(0xFF111111),
            fontSize = 27.sp, fontWeight = FontWeight.ExtraBold,
            lineHeight = 34.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Text("استخدم حساب TikTok للوصول إلى MESTOK.",
            color = Color(0xFF777777), fontSize = 14.sp,
            modifier = Modifier.padding(top = 12.dp, bottom = 28.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        LoginChoice("♪", "المتابعة باستخدام TikTok") { onTikTok() }
        Text("سيتم تحويلك إلى TikTok للتحقق من حسابك ومنح MESTOK الإذن.",
            color = Color(0xFF777777), fontSize = 12.sp, lineHeight = 18.sp,
            modifier = Modifier.padding(top = 14.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Spacer(Modifier.weight(1f))
        Text(
            "En continuant, tu acceptes nos Conditions d'utilisation et notre Politique de confidentialité.",
            color = Color(0xFF888888), fontSize = 11.sp, lineHeight = 17.sp,
            modifier = Modifier.padding(top = 8.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Row(
            Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 22.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            TextButton(onClick = {
                uriHandler.openUri("https://houssem028.github.io/terms.html")
            }) { Text("Conditions d'utilisation", color = Color(0xFF315D91), fontSize = 11.sp) }
            Text("•", color = Color(0xFF999999), modifier = Modifier.padding(top = 12.dp))
            TextButton(onClick = {
                uriHandler.openUri("https://houssem028.github.io/privacy.html")
            }) { Text("Confidentialité", color = Color(0xFF315D91), fontSize = 11.sp) }
        }
    }
}

@Composable
private fun LoginChoice(symbol: String, label: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().heightIn(min = 62.dp),
        shape = RoundedCornerShape(15.dp),
        color = Color(0xFFF1F1F1)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(symbol, color = if (symbol == "f") Color(0xFF1877F2)
                else if (symbol == "G") Color(0xFF4285F4) else Color(0xFF111111),
                fontSize = 22.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.width(34.dp))
            Text(label, color = Color(0xFF111111), fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold)
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
                       tiktokDisplayName: String, tiktokAvatarUrl: String,
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
        if (tiktokDisplayName.isNotBlank()) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Panel),
                    shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        Text("حساب TikTok المرتبط", color = Cyan, fontWeight = FontWeight.Bold,
                            fontSize = 12.sp)
                        Text(tiktokDisplayName, fontWeight = FontWeight.Bold,
                            fontSize = 17.sp, modifier = Modifier.padding(top = 5.dp))
                        Text("تم التحقق من الحساب عبر Login Kit التجريبي",
                            color = Muted, fontSize = 11.sp,
                            modifier = Modifier.padding(top = 4.dp))
                    }
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
    isAccountLinked: Boolean, onLogout: () -> Unit,
    onPrivacy: () -> Unit, onTerms: () -> Unit, onSupport: () -> Unit, onAbout: () -> Unit
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
        item { Text("الحساب", color = Cyan, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp)) }
        if (isAccountLinked) {
            item { SettingItem("تسجيل الخروج", "الخروج من حساب TikTok المرتبط بهذا التطبيق", "↪", onLogout) }
        }
        item { Text("اللغة", color = Cyan, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp)) }
        item { SettingItem("لغة التطبيق", "العربية — مستقلة عن لغة الهاتف", "ع", {}) }
        item { Text("عام", color = Cyan, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp)) }
        item { SettingItem("الخصوصية", "سياسة الخصوصية الكاملة", "›", onPrivacy) }
        item { SettingItem("شروط الاستخدام", "شروط استخدام MESTOK", "›", onTerms) }
        item { SettingItem("الدعم والمساعدة", "التواصل والإبلاغ عن مشكلة", "›", onSupport) }
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
private fun InfoScreen(title: String, body: String, onBack: () -> Unit,
                       externalUrl: String? = null, linkLabel: String? = null) {
    val uriHandler = LocalUriHandler.current
    Column(Modifier.fillMaxSize().background(Ink).padding(22.dp)) {
        IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "رجوع", tint = Color.White) }
        Text(title, fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 24.dp))
        Text(body, color = Muted, fontSize = 15.sp, lineHeight = 24.sp, modifier = Modifier.padding(top = 16.dp))
        if (externalUrl != null && linkLabel != null) {
            Button(onClick = { uriHandler.openUri(externalUrl) },
                modifier = Modifier.fillMaxWidth().padding(top = 22.dp)) { Text(linkLabel) }
        }
    }
}
