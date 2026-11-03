package com.aziz.banya

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth

@Composable
fun Settings3D(online: Boolean, status: String, modifier: Modifier = Modifier) {
    val gold = LuxGold
    val prefs = androidx.compose.ui.platform.LocalContext.current
        .getSharedPreferences("banya_settings", android.content.Context.MODE_PRIVATE)
    var sound by remember { mutableStateOf(prefs.getBoolean("sound", true)) }
    var vibration by remember { mutableStateOf(prefs.getBoolean("vibration", true)) }
    var night by remember { mutableStateOf(prefs.getBoolean("night", false)) }
    var dialog by remember { mutableStateOf("") }
    var info by remember { mutableStateOf("") }
    var price60 by remember { mutableStateOf(prefs.getInt("price60", 3000).toString()) }
    var price90 by remember { mutableStateOf(prefs.getInt("price90", 4500).toString()) }
    var price120 by remember { mutableStateOf(prefs.getInt("price120", 6000).toString()) }
    var editValue by remember { mutableStateOf("") }
    fun edit(key: String, value: String) { dialog = key; editValue = value }
    Column(modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(9.dp)) {
        LuxuryPanel(Modifier.fillMaxWidth()) {
            Text("⚙  Созламалар", color = gold, fontSize = 24.sp, fontWeight = FontWeight.Black)
        }
        LuxuryPanel(Modifier.fillMaxWidth()) {
            Text("◷  Иш вақти", color = gold, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            Text("Бошланиш: 16:00     Тугаш: 23:00", color = Color.White, fontSize = 16.sp)
        }
        LuxuryPanel(Modifier.fillMaxWidth()) {
            Text("◉  Тарифлар (баня нархлари)", color = gold, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                listOf(Triple("1 соат", "price60", price60), Triple("1,5 соат", "price90", price90),
                    Triple("2 соат", "price120", price120)).forEach { (label, key, price) ->
                    Column(Modifier.weight(1f).border(1.dp, gold, RoundedCornerShape(12.dp))
                        .clickable { edit(key, price) }.padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(label, color = gold, fontSize = 12.sp)
                        Text("$price тг", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text("✎", color = gold)
                    }
                }
            }
        }
        LuxuryPanel(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("♨  Хоналар сони", color = gold, fontSize = 19.sp, modifier = Modifier.weight(1f))
                Text("6", color = Color.White, fontSize = 24.sp)
            }
        }
        LuxuryPanel(Modifier.fillMaxWidth()) {
            Text("☁  Синхронизация (онлайн)", color = gold, fontSize = 18.sp)
            Text("Firebase: $status", color = if (online) LuxGreen else LuxRed)
        }
        LuxuryPanel(Modifier.fillMaxWidth()) {
            Text("⚙  Қўшимча созламалар", color = gold, fontSize = 18.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Овозли билдириш", color = Color.White, modifier = Modifier.weight(1f))
                Switch(checked = sound, onCheckedChange = { sound=it; prefs.edit().putBoolean("sound",it).apply() })
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Вибрация", color = Color.White, modifier = Modifier.weight(1f))
                Switch(checked = vibration, onCheckedChange = { vibration=it; prefs.edit().putBoolean("vibration",it).apply() })
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Тунги режим", color = Color.White, modifier = Modifier.weight(1f))
                Switch(checked = night, onCheckedChange = { night=it; prefs.edit().putBoolean("night",it).apply() })
            }
            Text("Тил: Ўзбекча", color = Color.White)
        }
        LuxuryPanel(Modifier.fillMaxWidth()) {
            Text("ⓘ  Маълумот", color = gold, fontSize = 18.sp)
            Text("БАНЯ БРОН • 6 хона • Firebase", color = Color.White)
            Text("Иккита телефон учун брон синхронлашуви", color = Color.White)
        }
        TextButton(onClick = { FirebaseAuth.getInstance().signOut() }) {
            Text("Аккаунтдан чиқиш", color = gold)
        }
    }
    if (dialog.isNotEmpty()) AlertDialog(
        onDismissRequest = { dialog="" },
        title = { Text("Тарифни ўзгартириш") },
        text = { OutlinedTextField(value=editValue, onValueChange={editValue=it},
            label={Text("Нарх (тенге)")}, singleLine=true) },
        confirmButton = { TextButton(onClick = {
            val n = editValue.toIntOrNull()
            if (n != null && n >= 0) {
                prefs.edit().putInt(dialog,n).apply()
                when(dialog) { "price60"->price60=n.toString(); "price90"->price90=n.toString(); "price120"->price120=n.toString() }
            } else info="Нарх нотўғри киритилди"
            dialog=""
        }) { Text("Сақлаш") } },
        dismissButton = { TextButton(onClick={dialog=""}) { Text("Бекор қилиш") } }
    )
    if(info.isNotEmpty()) AlertDialog(onDismissRequest={info=""},title={Text("Хабар")},
        text={Text(info)},confirmButton={TextButton(onClick={info=""}){Text("Ёпиш")}})
}
