package com.aziz.banya

import android.content.Context
import android.os.SystemClock
import android.util.Base64
import androidx.compose.foundation.layout.*
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
    var lockedUntil by remember { mutableLongStateOf(0L) }
    var message by remember { mutableStateOf("") }
    val configured = prefs.contains("hash") && prefs.contains("salt")

    if (unlocked) {
        content()
        return
    }

    WoodBackground {
        Column(Modifier.fillMaxSize().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center) {
            Text("♛", color = LuxGold, fontSize = 55.sp)
            Text("БАНЯ БРОН", color = LuxGold, fontSize = 30.sp,
                fontWeight = FontWeight.Black)
            Spacer(Modifier.height(22.dp))
            LuxuryPanel(Modifier.fillMaxWidth()) {
                Text(if (!configured) {
                    if (setupStep == 0) "4 рақамли PIN ўрнатинг" else "PIN-кодни такрорланг"
                } else "PIN-кодни киритинг", color = LuxGold, fontSize = 19.sp)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(value = pin, onValueChange = { next ->
                    if (next.length <= 4 && next.all(Char::isDigit)) pin = next
                }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                        focusedBorderColor = LuxGold, unfocusedBorderColor = LuxGold))
                Spacer(Modifier.height(12.dp))
                LuxuryButton("Тасдиқлаш", enabled = pin.length == 4,
                    modifier = Modifier.fillMaxWidth(), onClick = {
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
                    } else if (SystemClock.elapsedRealtime() < lockedUntil) {
                        message = "Кейинроқ қайта уриниб кўринг"
                        pin = ""
                    } else {
                        val salt = Base64.decode(prefs.getString("salt", ""), Base64.NO_WRAP)
                        val expected = Base64.decode(prefs.getString("hash", ""), Base64.NO_WRAP)
                        if (MessageDigest.isEqual(pinDigest(pin, salt), expected)) {
                            attempts = 0
                            pin = ""
                            unlocked = true
                        } else {
                            attempts++
                            pin = ""
                            if (attempts >= 5) {
                                lockedUntil = SystemClock.elapsedRealtime() + 60000L
                                attempts = 0
                                message = "5 марта хато. 1 дақиқадан кейин урининг"
                            } else message = "PIN нотўғри. Қолган уринишлар: ${5 - attempts}"
                        }
                    }
                })
                if (message.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Text(message, color = Color(0xFFFF8080))
                }
            }
        }
    }
}
