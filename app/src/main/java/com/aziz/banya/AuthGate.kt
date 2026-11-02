package com.aziz.banya

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth

@Composable
fun AuthGate(content: @Composable () -> Unit) {
    val auth = remember { FirebaseAuth.getInstance() }
    var signedIn by remember { mutableStateOf(auth.currentUser != null) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color.White, unfocusedTextColor = Color.White,
        focusedBorderColor = LuxGold, unfocusedBorderColor = LuxGold,
        focusedPlaceholderColor = Color.White, unfocusedPlaceholderColor = Color.LightGray,
        cursorColor = LuxGold
    )

    DisposableEffect(auth) {
        val listener = FirebaseAuth.AuthStateListener { signedIn = it.currentUser != null }
        auth.addAuthStateListener(listener)
        onDispose { auth.removeAuthStateListener(listener) }
    }
    if (signedIn) {
        content()
    } else {
        WoodBackground {
            Column(Modifier.fillMaxSize().navigationBarsPadding().verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center) {
                Text("♛", color = LuxGold, fontSize = 70.sp)
                Text("БАНЯ БРОН", color = LuxGold, fontSize = 34.sp,
                    fontWeight = FontWeight.Black)
                Spacer(Modifier.height(34.dp))
                LuxuryPanel(Modifier.fillMaxWidth()) {
                    OutlinedTextField(value = email, onValueChange = { email = it },
                        placeholder = { Text("✉  Email") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth(), colors = fieldColors,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
                }
                Spacer(Modifier.height(12.dp))
                LuxuryPanel(Modifier.fillMaxWidth()) {
                    OutlinedTextField(value = password, onValueChange = { password = it },
                        placeholder = { Text("♙  Пароль") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth(), colors = fieldColors,
                        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            TextButton(onClick = { visible = !visible }) {
                                Text(if (visible) "Яшириш" else "Кўриш", color = LuxGold)
                            }
                        })
                }
                TextButton(modifier = Modifier.align(Alignment.End), enabled = !busy,
                    onClick = {
                        if (email.isBlank()) {
                            error = "Аввал Email манзилини киритинг"
                        } else {
                            busy = true
                            error = ""
                            auth.sendPasswordResetEmail(email.trim()).addOnCompleteListener { task ->
                                busy = false
                                error = if (task.isSuccessful)
                                    "Паролни тиклаш ҳаволаси Email'га юборилди"
                                else "Паролни тиклаш сўрови бажарилмади"
                            }
                        }
                    }) { Text("Паролни унутдим?", color = LuxGold, fontSize = 16.sp) }
                Spacer(Modifier.height(8.dp))
                LuxuryButton(if (busy) "Кутинг..." else "➜  Кириш", enabled = !busy &&
                    email.isNotBlank() && password.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().height(65.dp), onClick = {
                        busy = true
                        error = ""
                        auth.signInWithEmailAndPassword(email.trim(), password)
                            .addOnCompleteListener { task ->
                                busy = false
                                if (!task.isSuccessful) error = "Email ёки пароль нотўғри"
                                password = ""
                            }
                    })
                if (error.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Text(error, color = LuxGold)
                }
                Spacer(Modifier.height(45.dp))
                Text("♨  БАНЯ БРОН  •  3D", color = LuxGold, fontSize = 17.sp)
            }
        }
    }
}
