package com.aziz.banya

import android.os.Bundle
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val gold = Color(0xFFE8BE69)
private val dark = Color(0xFF211811)
private val panel = Color(0xFF39271B)
private val green = Color(0xFF4BD58B)
private val red = Color(0xFFFF6D6D)
private val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")

data class Booking(val room: Int, val date: String, val start: String, val minutes: Int, val name: String) {
    val price get() = minutes * 50
    val end get() = LocalTime.parse(start).plusMinutes(minutes.toLong()).toString()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AuthGate { BanyaApp() } }
    }
}

@Composable
fun BanyaApp() {
    val cloud = remember { CloudBookings() }
    val bookings = remember { mutableStateListOf<Booking>() }
    var online by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Серверга уланмоқда") }
    var page by remember { mutableIntStateOf(0) }
    var room by remember { mutableIntStateOf(1) }
    var day by remember { mutableStateOf(LocalDate.now()) }
    var minute by remember { mutableIntStateOf(16 * 60) }
    var duration by remember { mutableIntStateOf(60) }
    var name by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<Booking?>(null) }
    var deleting by remember { mutableStateOf<Booking?>(null) }
    var busy by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf("") }
    val today = LocalDate.now()
    val now = LocalTime.now()

    DisposableEffect(cloud) {
        val listener = cloud.listen { data, error ->
            if (data != null) {
                bookings.clear()
                bookings.addAll(data)
                online = true
                status = "Онлайн"
            } else {
                online = false
                status = error ?: "Сервер кутилмоқда"
            }
        }
        onDispose { listener.remove() }
    }

    fun openForm(b: Booking? = null, targetRoom: Int = 1) {
        editing = b
        room = b?.room ?: targetRoom
        day = runCatching { LocalDate.parse(b?.date ?: today.format(fmt), fmt) }.getOrDefault(today)
        minute = b?.let { LocalTime.parse(it.start).toSecondOfDay() / 60 } ?: 16 * 60
        duration = b?.minutes ?: 60
        name = b?.name ?: ""
        notice = ""
        page = 1
    }

    WoodBackground {
      Column(Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        LuxuryHeader(online, status)
        when (page) {
            0 -> {
                Column(Modifier.weight(1f)) {
                    LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(vertical = 5.dp)) {
                        items((1..6).toList()) { r ->
                            val current = bookings.firstOrNull {
                                it.room == r && it.date == today.format(fmt) &&
                                !now.isBefore(LocalTime.parse(it.start)) &&
                                now.isBefore(LocalTime.parse(it.end))
                            }
                            LuxuryPanel(Modifier.fillMaxWidth().height(164.dp)) {
                                Text("$r-ХОНА", modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center, color = LuxGold,
                                    fontSize = 23.sp, fontWeight = FontWeight.Black)
                                Spacer(Modifier.height(12.dp))
                                Text(if (!online) "Текширилмоқда" else if (current != null) "Банд" else "Бўш",
                                    modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
                                    color = if (!online) LuxGold else if (current != null) LuxRed else LuxGreen,
                                    fontSize = if (online) 23.sp else 13.sp, fontWeight = FontWeight.Bold)
                                if (current != null) {
                                    Text("${current.start}–${current.end}", color = Color.White,
                                        modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontSize = 12.sp)
                                }
                                Spacer(Modifier.weight(1f))
                                LuxuryButton("Брон қилиш", { openForm(targetRoom = r) },
                                    Modifier.fillMaxWidth(), enabled = online)
                            }
                        }
                    }
                    LuxuryButton("▦  Брон қилиш", { openForm() }, Modifier.fillMaxWidth().padding(vertical = 10.dp),
                        enabled = online)
                }
            }
            1 -> {
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    LuxuryPanel(Modifier.fillMaxWidth()) {
                        Text(if (editing == null) "▦  Брон қилиш" else "✎  Бронни таҳрирлаш",
                            color = LuxGold, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                    }
                    Text("Хона", color = Color.White, modifier = Modifier.padding(top = 10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        (1..6).forEach { r ->
                            OutlinedButton(onClick = { room = r }, contentPadding = PaddingValues(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = if (room == r) gold else Color.White)) {
                                Text("$r")
                            }
                        }
                    }
                    Text("Сана", color = Color.White, modifier = Modifier.padding(top = 12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SmallControl("−") { day = day.minusDays(1) }
                        Text(day.format(fmt), color = gold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                        SmallControl("+") { day = day.plusDays(1) }
                    }
                    Text("Бошланиш вақти", color = Color.White, modifier = Modifier.padding(top = 12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SmallControl("−30") { minute = (minute - 30).coerceAtLeast(16 * 60) }
                        Text("%02d:%02d".format(minute / 60, minute % 60),
                            color = gold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontSize = 21.sp)
                        SmallControl("+30") { minute = (minute + 30).coerceAtMost(22 * 60 + 30) }
                    }
                    Text("Мижоз исми", color = Color.White, modifier = Modifier.padding(top = 12.dp))
                    OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                            focusedBorderColor = gold, unfocusedBorderColor = gold, cursorColor = gold))
                    Text("Давомийлиги", color = Color.White, modifier = Modifier.padding(top = 12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        listOf(60 to "1 соат", 90 to "1,5 соат", 120 to "2 соат").forEach { (m, title) ->
                            OutlinedButton(onClick = { duration = m }, contentPadding = PaddingValues(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = if (m == duration) gold else Color.White)) {
                                Text(title, fontSize = 12.sp)
                            }
                        }
                    }
                    LuxuryPanel(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                        Text("◉  Жами: ${duration * 50} тг", color = LuxGold,
                            fontSize = 23.sp, fontWeight = FontWeight.Bold)
                    }
                    LuxuryButton(if (editing == null) "✓  Бронни сақлаш" else "✓  Ўзгаришни сақлаш", enabled = online && !busy, positive = true, modifier = Modifier.fillMaxWidth(), onClick = {
                        val date = day.format(fmt)
                        val time = "%02d:%02d".format(minute / 60, minute % 60)
                        val proposed = Booking(room, date, time, duration, name.trim())
                        notice = when {
                            day.isBefore(today) -> "Ўтган кунга брон қўйиб бўлмайди"
                            minute < 960 || minute + duration > 1380 -> "Иш вақти 16:00–23:00"
                            name.isBlank() -> "Мижоз исмини киритинг"
                            name.length > 120 -> "Исм жуда узун"
                            else -> {
                                busy = true
                                val previous = editing
                                val cb: (Boolean, String) -> Unit = { ok, msg ->
                                    busy = false
                                    notice = msg
                                    if (ok) { editing = null; name = "" }
                                }
                                if (previous == null) cloud.reserve(proposed, cb)
                                else cloud.edit(previous, proposed, cb)
                                "Сервер тасдиғи кутилмоқда..."
                            }
                        }
                    })
                    if (notice.isNotBlank()) Text(notice, color = gold, modifier = Modifier.padding(vertical = 10.dp))
                    Text("Сақланган бронлар", color = gold, fontSize = 20.sp,
                        modifier = Modifier.padding(top = 12.dp, bottom = 6.dp))
                    bookings.sortedWith(compareBy<Booking> { it.date }.thenBy { it.start })
                        .forEach { b ->
                            LuxuryPanel(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                                Text("${b.date} • ${b.room}-хона • ${b.start}–${b.end}",
                                    color = gold, fontWeight = FontWeight.Bold)
                                Text("${b.name} • ${b.price} тг", color = Color.White)
                                Row {
                                    TextButton(enabled = online && !busy, onClick = { openForm(b) }) {
                                        Text("Таҳрирлаш", color = gold)
                                    }
                                    TextButton(enabled = online && !busy, onClick = { deleting = b }) {
                                        Text("Ўчириш", color = red)
                                    }
                                }
                            }
                        }
                }
            }
            else -> {
                Column(Modifier.weight(1f)) {
                    LuxuryPanel(Modifier.fillMaxWidth()) {
                        Text("⚙  Созламалар", color = LuxGold, fontSize = 24.sp)
                    }
                    Spacer(Modifier.height(14.dp))
                    LuxuryPanel(Modifier.fillMaxWidth()) {
                        Text("◷  Иш вақти", color = LuxGold, fontSize = 20.sp)
                        Text("16:00 – 23:00", color = Color.White, fontSize = 18.sp)
                    }
                    Spacer(Modifier.height(12.dp))
                    LuxuryPanel(Modifier.fillMaxWidth()) {
                        Text("◉  Тарифлар", color = LuxGold, fontSize = 20.sp)
                        Text("1 соат                  3 000 тг", color = Color.White)
                        Text("1,5 соат               4 500 тг", color = Color.White)
                        Text("2 соат                  6 000 тг", color = Color.White)
                    }
                    Spacer(Modifier.height(12.dp))
                    LuxuryPanel(Modifier.fillMaxWidth()) {
                        Text("▣  Дастур", color = LuxGold, fontSize = 20.sp)
                        Text("Firebase: $status", color = if (online) LuxGreen else LuxGold)
                        Text("6 та хона • Икки телефон • 3D версия", color = Color.White)
                    }
                    Spacer(Modifier.height(20.dp))
                    TextButton(onClick = { FirebaseAuth.getInstance().signOut() }) {
                        Text("Аккаунтдан чиқиш", color = gold)
                    }
                    Spacer(Modifier.weight(1f))
                }
            }
        }
        LuxuryNav(page) { i -> if (i == 1) openForm() else page = i }
      }
    }

    deleting?.let { b ->
        AlertDialog(onDismissRequest = { if (!busy) deleting = null },
            title = { Text("Бронни ўчириш?") },
            text = { Text("${b.room}-хона, ${b.date} ${b.start}–${b.end}, ${b.name}") },
            confirmButton = {
                TextButton(enabled = !busy, onClick = {
                    busy = true
                    cloud.delete(b) { ok, msg ->
                        busy = false
                        notice = msg
                        if (ok) deleting = null
                    }
                }) { Text("Ўчириш") }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Бекор қилиш") } })
    }
}

@Composable
private fun SmallControl(label: String, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, contentPadding = PaddingValues(horizontal = 12.dp)) {
        Text(label, color = gold)
    }
}
