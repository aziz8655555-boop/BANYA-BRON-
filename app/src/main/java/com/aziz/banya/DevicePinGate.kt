package com.aziz.banya

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.EmailAuthProvider
import android.util.Base64
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import java.security.MessageDigest

private fun pinDigest(pin: String, salt: ByteArray): ByteArray {
    val spec = PBEKeySpec(pin.toCharArray(), salt, 120000, 256)
    return try { SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded }
    finally { spec.clearPassword() }
}

@Composable
fun DevicePinGate(content: @Composable () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember(context) {
        context.getSharedPreferences("banya_device_pin", Context.MODE_PRIVATE)
    }
    var unlocked by remember { mutableStateOf(false) }
    var pin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var setupStep by remember { mutableIntStateOf(0) }
    var attempts by remember { mutableIntStateOf(0) }
    var lockedUntil by remember { mutableLongStateOf(prefs.getLong("lockedUntil", 0L)) }
    var resetDialog by remember { mutableStateOf(false) }
    var resetPassword by remember { mutableStateOf("") }
    var resetting by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    val configured = prefs.contains("hash") && prefs.contains("salt")

    fun submitPin() {
                    if (!configured) {
                        if (setupStep == 0) {
                            confirm = pin
                            pin = ""
                            setupStep = 1
                            message = ""
                        } else if (confirm != pin) {
                            message = "PIN-кодлар бир хил эмас"
                            pin = ""
                            confirm = ""
                            setupStep = 0
                        } else {
                            val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
                            val hash = pinDigest(pin, salt)
                            prefs.edit()
                                .putString("salt", Base64.encodeToString(salt, Base64.NO_WRAP))
                                .putString("hash", Base64.encodeToString(hash, Base64.NO_WRAP))
                                .apply()
                            pin = ""
                            confirm = ""
                            unlocked = true
                        }
                    } else if (System.currentTimeMillis() < lockedUntil) {
                        message = "Кейинроқ қайта уриниб кўринг"
                        pin = ""
                    } else {
                        val salt = Base64.decode(prefs.getString("salt", ""), Base64.NO_WRAP)
                        val expected = Base64.decode(prefs.getString("hash", ""), Base64.NO_WRAP)
                        if (MessageDigest.isEqual(pinDigest(pin, salt), expected)) {
                            attempts = 0
                            lockedUntil = 0L
                            prefs.edit().remove("lockedUntil").apply()
                            pin = ""
                            unlocked = true
                        } else {
                            attempts++
                            pin = ""
                            if (attempts >= 5) {
                                lockedUntil = System.currentTimeMillis() + 60000L
                                prefs.edit().putLong("lockedUntil", lockedUntil).apply()
                                attempts = 0
                                message = "5 марта хато. 1 дақиқадан кейин урининг"
                            } else message = "PIN нотўғри. Қолган уринишлар: ${5 - attempts}"
                        }
                    }
    }

    LaunchedEffect(pin) {
        if (pin.length == 4) submitPin()
    }

    if (unlocked) {
        content()
        return
    }

    WoodBackground {
        Column(Modifier.fillMaxSize().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center) {
            Text("♛", color = LuxGold, fontSize = 64.sp)
            Text("БАНЯ БРОН", color = LuxGold, fontSize = 33.sp,
                fontWeight = FontWeight.Black)
            Spacer(Modifier.height(22.dp))
            LuxuryPanel(Modifier.fillMaxWidth()) {
                Text(if (!configured) {
                    if (setupStep == 0) "4 рақамли PIN ўрнатинг" else "PIN-кодни такрорланг"
                } else "PIN-кодни киритинг", color = LuxGold, fontSize = 19.sp)
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    repeat(4) { index ->
                        Text(if (index < pin.length) "●" else "○",
                            color = LuxGold, fontSize = 34.sp,
                            modifier = Modifier.padding(horizontal = 9.dp))
                    }
                }
                Spacer(Modifier.height(16.dp))
                listOf(listOf("1", "2", "3"), listOf("4", "5", "6"),
                    listOf("7", "8", "9"), listOf("", "0", "⌫")).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        row.forEach { digit ->
                            if (digit.isEmpty()) {
                                Spacer(Modifier.weight(1f).height(60.dp))
                            } else {
                                Box(Modifier.weight(1f).height(72.dp), contentAlignment = Alignment.Center) {
                                    Box(Modifier.size(68.dp)
                                        .shadow(7.dp, CircleShape,
                                            ambientColor = LuxGold, spotColor = LuxGold)
                                        .background(
                                            Brush.verticalGradient(
                                                if (digit == "⌫") listOf(
                                                    Color(0xFFFF6B5A), Color(0xFFB50000), Color(0xFF610000))
                                                else listOf(
                                                    Color(0xFF136B9A), Color(0xFF063557), Color(0xFF010F20))
                                            ), CircleShape)
                                        .border(2.5.dp, LuxGold, CircleShape)
                                        .clickable {
                                            if (digit == "⌫") pin = pin.dropLast(1)
                                            else if (pin.length < 4) pin += digit
                                        }, contentAlignment = Alignment.Center) {
                                        Text(digit, color = Color.White, fontSize = 29.sp,
                                            fontWeight = FontWeight.Black)
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(9.dp))
                }
                if (configured) {
                    TextButton(onClick = { resetDialog = true }) {
                        Text("PIN-кодни унутдим", color = LuxGold)
                    }
                }
                if (message.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Text(message, color = Color(0xFFFF8080))
                }
            }
        }
    }
    if (resetDialog) {
        AlertDialog(
            onDismissRequest = { if (!resetting) resetDialog = false },
            title = { Text("PIN-кодни тиклаш") },
            text = {
                Column {
                    Text("Firebase аккаунтингиз паролини киритинг.")
                    OutlinedTextField(
                        value = resetPassword,
                        onValueChange = { resetPassword = it },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        label = { Text("Аккаунт пароли") }
                    )
                }
            },
            confirmButton = {
                TextButton(enabled = !resetting && resetPassword.isNotBlank(), onClick = {
                    val user = FirebaseAuth.getInstance().currentUser
                    val email = user?.email
                    if (user == null || email.isNullOrBlank()) {
                        message = "Аккаунт маълумоти топилмади"
                    } else {
                        resetting = true
                        user.reauthenticate(
                            EmailAuthProvider.getCredential(email, resetPassword)
                        ).addOnCompleteListener { task ->
                            resetting = false
                            resetPassword = ""
                            if (task.isSuccessful) {
                                prefs.edit().remove("hash").remove("salt")
                                    .remove("lockedUntil").apply()
                                lockedUntil = 0L
                                attempts = 0
                                setupStep = 0
                                pin = ""
                                confirm = ""
                                message = "Янги PIN-код ўрнатинг"
                                resetDialog = false
                            } else {
                                message = "Парол текширилмади. Интернет ва паролни текширинг"
                            }
                        }
                    }
                }) { Text("Тасдиқлаш") }
            },
            dismissButton = {
                TextButton(enabled = !resetting, onClick = {
                    resetPassword = ""
                    resetDialog = false
                }) { Text("Бекор қилиш") }
            }
        )
    }

}
