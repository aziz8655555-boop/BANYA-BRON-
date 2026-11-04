package com.aziz.banya

import android.os.Bundle
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
    var page by remember { mutableIntStateOf(3) }
    var room by remember { mutableIntStateOf(1) }
    var day by remember { mutableStateOf(LocalDate.now()) }
    var minute by remember { mutableIntStateOf(16 * 60) }
    var duration by remember { mutableIntStateOf(60) }
    var name by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<Booking?>(null) }
    var deleting by remember { mutableStateOf<Booking?>(null) }
    var selectedBooking by remember { mutableStateOf<Booking?>(null) }
    var occupancyDay by remember { mutableStateOf(LocalDate.now()) }
    var busy by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf("") }
    var soundOn by remember { mutableStateOf(true) }
    var vibrationOn by remember { mutableStateOf(true) }
    var nightOn by remember { mutableStateOf(false) }
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
      Column(Modifier.fillMaxSize().navigationBarsPadding().padding(horizontal = 12.dp)) {
        LuxuryHeader(online, status)
        when (page) {
            1 -> {
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    LuxuryPanel(Modifier.fillMaxWidth()) {
                        Text(if (editing == null) "▦  Брон қилиш" else "✎  Бронни таҳрирлаш",
                            color = LuxGold, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                    }
                    Text("♨  Хона", color = LuxGold, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 5.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        (1..6).forEach { r ->
                            OutlinedButton(onClick = { room = r }, contentPadding = PaddingValues(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = if (room == r) gold else Color.White)) {
                                Text("$r")
                            }
                        }
                    }
                    LuxuryPanel(Modifier.fillMaxWidth().padding(top = 5.dp)) {
                        Text("◷  Бошланиш вақти", color = LuxGold, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SmallControl("−1 соат") {
                                minute = (minute - 60).coerceAtLeast(16 * 60)
                            }
                            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                SmallControl("⌃") {
                                    minute = (minute + 30).coerceAtMost(22 * 60 + 30)
                                }
                                Text("%02d:%02d".format(minute / 60, minute % 60),
                                    color = LuxGold, fontSize = 27.sp, fontWeight = FontWeight.Black)
                                SmallControl("⌄") {
                                    minute = (minute - 30).coerceAtLeast(16 * 60)
                                }
                            }
                            SmallControl("+1 соат") {
                                minute = (minute + 60).coerceAtMost(22 * 60 + 30)
                            }
                        }
                    }
                    Spacer(Modifier.height(5.dp))
                    LuxuryPanel(Modifier.fillMaxWidth()) {
                        OutlinedTextField(value = name, onValueChange = { name = it },
                            placeholder = { Text("Мижоз исмини киритинг...", color = Color.LightGray) },
                            singleLine = true, textStyle = androidx.compose.ui.text.TextStyle(fontSize = 21.sp),
                            modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                                focusedBorderColor = LuxGold, unfocusedBorderColor = LuxGold, cursorColor = LuxGold))
                    }
                    Text("⌛  Давомийлиги (вақт)", color = LuxGold, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 5.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        listOf(60 to "1 соат", 90 to "1,5 соат", 120 to "2 соат").forEach { (m, title) ->
                            OutlinedButton(onClick = { duration = m }, contentPadding = PaddingValues(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = if (m == duration) gold else Color.White)) {
                                Text(title, fontSize = 12.sp)
                            }
                        }
                    }
                    LuxuryPanel(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
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
                    Text("Сақланган бронлар", color = gold, fontSize = 17.sp,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
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
            2 -> {
                Settings3D(online = online, status = status, modifier = Modifier.weight(1f))
            }
            3 -> {
                Column(Modifier.weight(1f)) {
                    LuxuryPanel(Modifier.fillMaxWidth()) {
                        Text("▦  Умумий бандлик", color = LuxGold,
                            fontSize = 23.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SmallControl("❮") { occupancyDay = occupancyDay.minusDays(1) }
                        Text(occupancyDay.format(fmt), modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center, color = LuxGold, fontSize = 19.sp)
                        SmallControl("❯") { occupancyDay = occupancyDay.plusDays(1) }
                        SmallControl("Бугун") { occupancyDay = today }
                    }
                    LuxuryPanel(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically) {
                            Text("✓ Бўш", color = LuxGreen, fontSize = 12.sp)
                            Text("× Банд", color = LuxRed, fontSize = 12.sp)
                            Text("✓ Танланган", color = LuxGold, fontSize = 12.sp)
                        }
                        Text("◷ Соатлар бўйича жадвал", color = LuxGold,
                            fontSize = 12.sp, textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth())
                    }
                    val selectedDate = occupancyDay.format(fmt)
                    BoxWithConstraints(Modifier.fillMaxWidth()) {
                        // All ten hourly columns and six rooms fit the available screen width.
                        val roomWidth = maxWidth * 0.205f
                        val hourWidth = (maxWidth - roomWidth) / 10
                        Column(Modifier.fillMaxWidth()) {
                            Row {
                                Box(Modifier.width(roomWidth).height(31.dp),
                                    contentAlignment = Alignment.Center) {
                                    Text("♨ Хона", color = LuxGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                (0..9).forEach { index ->
                                    val m = 900 + index * 60
                                    Box(Modifier.width(hourWidth).height(31.dp),
                                        contentAlignment = Alignment.Center) {
                                        Text("%02d:00".format((m / 60) % 24),
                                            color = LuxGold, fontSize = 8.sp, maxLines = 1)
                                    }
                                }
                            }
                            (1..6).forEach { r ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.width(roomWidth).height(43.dp),
                                        contentAlignment = Alignment.Center) {
                                        Text("$r-хона", color = LuxGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                    (0..9).forEach { index ->
                                        val m = 900 + index * 60
                                        val existing = bookings.firstOrNull { b ->
                                            b.room == r && b.date == selectedDate &&
                                                runCatching {
                                                    val start = LocalTime.parse(b.start).toSecondOfDay() / 60
                                                    m < start + b.minutes && m + 60 > start
                                                }.getOrDefault(false)
                                        }
                                        Box(Modifier.padding(1.dp).width(hourWidth - 2.dp).height(36.dp)
                                            .background(
                                                if (existing == null) Color(0xFF087E2E) else Color(0xFFB91F1B),
                                                RoundedCornerShape(6.dp))
                                            .border(0.7.dp, LuxGold, RoundedCornerShape(6.dp))
                                            .clickable(enabled = online) {
                                                if (existing != null) selectedBooking = existing
                                                else {
                                                    openForm(targetRoom = r)
                                                    day = occupancyDay
                                                    minute = m.coerceIn(960, 22 * 60 + 30)
                                                }
                                            }, contentAlignment = Alignment.Center) {
                                            Text(if (existing == null) "✓" else "×",
                                                color = Color.White, fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    LuxuryPanel(Modifier.fillMaxWidth()) {
                        Text("Бўш вақтни босинг — брон қилиш ойнаси очилади.",
                            color = Color.White, fontSize = 12.sp)
                        Text("Банд вақтни босинг — брон маълумотлари кўринади.",
                            color = Color.White, fontSize = 12.sp)
                    }
                    Spacer(Modifier.weight(1f))
                }
            }
        }
LuxuryNav(if (page == 3) 0 else page) { i -> if (i == 1) openForm() else page = if (i == 0) 3 else i }
      }
    }

    selectedBooking?.let { b ->
        AlertDialog(onDismissRequest = { selectedBooking = null },
            title = { Text("Брон маълумоти", color = LuxGold) },
            text = {
                Column {
                    Text("${b.room}-хона • ${b.date}")
                    Text("Мижоз: ${b.name}")
                    Text("Вақт: ${b.start}–${b.end}")
                    Text("Давомийлиги: ${b.minutes} дақиқа")
                    Text("Нарх: ${b.price} тг")
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedBooking = null; openForm(b) }) {
                    Text("Таҳрирлаш")
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { selectedBooking = null; deleting = b }) { Text("Ўчириш") }
                    TextButton(onClick = { selectedBooking = null }) { Text("Ёпиш") }
                }
            })
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
