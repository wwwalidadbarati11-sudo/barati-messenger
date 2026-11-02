package com.barati.messenger

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                MaterialTheme(
                    colorScheme = lightColorScheme(
                        primary = Color(0xFF1769AA),
                        secondary = Color(0xFF22A6A1),
                        background = Color(0xFFF5F7FA)
                    )
                ) { BaratiApp(this) }
            }
        }
    }
}

private data class ChatItem(val name: String, val preview: String, val time: String, val unread: Int = 0)
private val sampleChats = listOf(
    ChatItem("احمد", "سلام، خوبی؟", "۱۰:۴۲", 2),
    ChatItem("محمد", "بعداً تماس می‌گیرم", "دیروز"),
    ChatItem("گروه خانواده", "علی: عکس‌ها رسید", "دیروز", 4),
    ChatItem("دوست من", "ممنون از لطفت", "شنبه")
)

@Composable
fun BaratiApp(activity: ComponentActivity) {
    val auth = remember { FirebaseAuth.getInstance() }
    var phone by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var verificationId by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var loggedIn by remember { mutableStateOf(auth.currentUser != null) }

    fun finishLogin(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential).addOnCompleteListener(activity) { task ->
            loading = false
            if (task.isSuccessful) { loggedIn = true; message = "" }
            else message = task.exception?.localizedMessage ?: "ورود ناموفق بود"
        }
    }
    fun sendCode() {
        if (!phone.trim().startsWith("+") || phone.trim().length < 8) {
            message = "شماره را با کد کشور وارد کنید؛ نمونه: +989121234567"; return
        }
        loading = true; message = ""
        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) = finishLogin(credential)
            override fun onVerificationFailed(e: FirebaseException) {
                loading = false; message = e.localizedMessage ?: "ارسال کد انجام نشد"
            }
            override fun onCodeSent(id: String, token: PhoneAuthProvider.ForceResendingToken) {
                verificationId = id; loading = false; message = "کد تأیید ارسال شد."
            }
        }
        val options = PhoneAuthOptions.newBuilder(auth).setPhoneNumber(phone.trim())
            .setTimeout(60L, TimeUnit.SECONDS).setActivity(activity).setCallbacks(callbacks).build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }
    fun verify() {
        val id = verificationId
        if (id == null || code.length != 6) { message = "کد شش‌رقمی را وارد کنید."; return }
        loading = true; message = ""
        finishLogin(PhoneAuthProvider.getCredential(id, code.trim()))
    }

    if (loggedIn) {
        BaratiHome(onLogout = {
            auth.signOut(); loggedIn = false; phone = ""; code = ""; verificationId = null
        })
    } else {
        Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center) {
            Text("براتی", fontSize = 38.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text("پیام‌رسان براتی", fontSize = 18.sp)
            Spacer(Modifier.height(10.dp))
            Text("ارتباط آسان، همیشه و همه‌جا", color = Color.Gray)
            Spacer(Modifier.height(32.dp))
            Text(if (verificationId == null) "ورود با شماره موبایل" else "کد تأیید پیامکی را وارد کنید",
                fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(phone, { phone = it }, Modifier.fillMaxWidth(),
                enabled = verificationId == null && !loading, label = { Text("شماره موبایل با کد کشور") },
                placeholder = { Text("+989121234567") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true, shape = RoundedCornerShape(14.dp))
            if (verificationId != null) {
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(code, { code = it.filter(Char::isDigit).take(6) },
                    Modifier.fillMaxWidth(), label = { Text("کد شش‌رقمی") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
            }
            Spacer(Modifier.height(18.dp))
            Button(onClick = { if (verificationId == null) sendCode() else verify() },
                Modifier.fillMaxWidth().height(52.dp), enabled = !loading) {
                if (loading) CircularProgressIndicator(Modifier.size(22.dp), color = MaterialTheme.colorScheme.onPrimary)
                else Text(if (verificationId == null) "ارسال کد پیامکی" else "تأیید و ورود")
            }
            if (verificationId != null) TextButton(onClick = { verificationId = null; code = ""; message = "" }) {
                Text("تغییر شماره موبایل")
            }
            Spacer(Modifier.height(12.dp))
            Text(message, color = if (message.contains("ارسال شد")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BaratiHome(onLogout: () -> Unit) {
    var tab by remember { mutableStateOf("گفتگوها") }
    var selectedChat by remember { mutableStateOf<String?>(null) }
    var search by remember { mutableStateOf("") }
    var showNewChat by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf("") }

    if (selectedChat != null) {
        ChatScreen(name = selectedChat!!, onBack = { selectedChat = null })
        return
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Column { Text("براتی", fontWeight = FontWeight.Bold); Text("پیام‌رسان", fontSize = 11.sp) } },
                actions = {
                    IconButton(onClick = { showNewChat = true }) { Icon(Icons.Default.AddComment, "گفتگوی جدید") }
                    IconButton(onClick = onLogout) { Icon(Icons.Default.Logout, "خروج") }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                listOf("گفتگوها" to Icons.Default.Chat, "تماس‌ها" to Icons.Default.Call,
                    "استوری" to Icons.Default.PhotoLibrary, "کانال‌ها" to Icons.Default.Campaign,
                    "پروفایل" to Icons.Default.Person).forEach { (label, icon) ->
                    NavigationBarItem(selected = tab == label, onClick = { tab = label },
                        icon = { Icon(icon, contentDescription = label) }, label = { Text(label, fontSize = 10.sp) })
                }
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            when (tab) {
                "گفتگوها" -> {
                    OutlinedTextField(search, { search = it }, Modifier.fillMaxWidth().padding(top = 12.dp),
                        placeholder = { Text("جستجو در گفتگوها") }, leadingIcon = { Icon(Icons.Default.Search, null) },
                        singleLine = true, shape = RoundedCornerShape(16.dp))
                    LazyColumn {
                        items(sampleChats.filter { it.name.contains(search) || it.preview.contains(search) }) { chat ->
                            Row(Modifier.fillMaxWidth().clickable { selectedChat = chat.name }.padding(vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(48.dp)) {
                                    Box(contentAlignment = Alignment.Center) { Text(chat.name.take(1), fontSize = 20.sp, fontWeight = FontWeight.Bold) }
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(chat.name, fontWeight = FontWeight.SemiBold)
                                    Text(chat.preview, color = Color.Gray, fontSize = 13.sp)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(chat.time, fontSize = 11.sp, color = Color.Gray)
                                    if (chat.unread > 0) Text("${chat.unread}", color = Color.White,
                                        modifier = Modifier.padding(top = 4.dp).size(20.dp)
                                            .wrapContentSize().then(Modifier), fontSize = 11.sp)
                                }
                            }
                            HorizontalDivider()
                        }
                    }
                }
                "تماس‌ها" -> EmptySection(Icons.Default.Call, "تماس‌ها", "فهرست تماس‌ها پس از اتصال سرویس تماس نمایش داده می‌شود.")
                "استوری" -> EmptySection(Icons.Default.PhotoLibrary, "استوری‌ها", "بخش استوری در نسخه بعدی به سرور متصل می‌شود.")
                "کانال‌ها" -> EmptySection(Icons.Default.Campaign, "کانال‌ها", "کانال‌های عمومی پس از راه‌اندازی سرور فعال می‌شوند.")
                else -> Column(Modifier.fillMaxWidth().padding(top = 36.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.AccountCircle, null, Modifier.size(72.dp), tint = MaterialTheme.colorScheme.primary)
                    Text("پروفایل براتی", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text("حساب کاربری شما")
                    Spacer(Modifier.height(16.dp))
                    OutlinedButton(onClick = onLogout) { Text("خروج از حساب") }
                }
            }
        }
    }
    if (showNewChat) {
        var contact by remember { mutableStateOf("") }
        AlertDialog(onDismissRequest = { showNewChat = false },
            title = { Text("گفتگوی جدید") },
            text = { OutlinedTextField(contact, { contact = it }, label = { Text("نام مخاطب") }, singleLine = true) },
            confirmButton = { TextButton(onClick = {
                if (contact.isNotBlank()) selectedChat = contact.trim()
                showNewChat = false
            }) { Text("شروع") } },
            dismissButton = { TextButton(onClick = { showNewChat = false }) { Text("انصراف") } })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EmptySection(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, detail: String) {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center) {
        Icon(icon, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(12.dp))
        Text(title, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(detail, color = Color.Gray, textAlign = TextAlign.Center)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatScreen(name: String, onBack: () -> Unit) {
    var draft by remember { mutableStateOf("") }
    val messages = remember { mutableStateListOf("سلام! به براتی خوش آمدی.") }
    Scaffold(topBar = { TopAppBar(title = { Text(name) }, navigationIcon = {
        IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "بازگشت") }
    }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(12.dp)) {
            LazyColumn(Modifier.weight(1f).fillMaxWidth(), reverseLayout = true) {
                items(messages.reversed()) { msg ->
                    Surface(color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(14.dp), modifier = Modifier.padding(vertical = 5.dp)) {
                        Text(msg, Modifier.padding(12.dp))
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(draft, { draft = it }, Modifier.weight(1f),
                    placeholder = { Text("پیام بنویسید...") }, singleLine = true,
                    shape = RoundedCornerShape(24.dp))
                IconButton(onClick = { if (draft.isNotBlank()) { messages.add(draft.trim()); draft = "" } }) {
                    Icon(Icons.Default.Send, "ارسال", tint = MaterialTheme.colorScheme.primary)
                }
            }
            Text("این گفتگوی آزمایشی فقط روی همین دستگاه ذخیره می‌شود؛ ارسال اینترنتی هنوز فعال نیست.",
                fontSize = 10.sp, color = Color.Gray, modifier = Modifier.padding(top = 6.dp))
        }
    }
}
