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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val Gold = Color(0xFFE8BE69)
private val Wood = Color(0xFF211811)
private val Panel = Color(0xFF39271B)
private val Green = Color(0xFF4BD58B)
private val Red = Color(0xFFFF6D6D)
private val dateFmt = DateTimeFormatter.ISO_LOCAL_DATE

data class Booking(val room: Int, val date: String, val start: String, val minutes: Int, val name: String) {
    val price: Int get() = minutes * 50
    val end: String get() = LocalTime.parse(start).plusMinutes(minutes.toLong()).toString()
}

private fun loadBookings(context: Context): List<Booking> = try {
    val array = JSONArray(context.getSharedPreferences("banya", Context.MODE_PRIVATE).getString("bookings", "[]"))
    (0 until array.length()).map { i ->
        val o = array.getJSONObject(i)
        Booking(o.getInt("room"), o.getString("date"), o.getString("start"), o.getInt("minutes"), o.getString("name"))
    }
} catch (_: Exception) { emptyList() }

private fun saveBookings(context: Context, bookings: List<Booking>) {
    val array = JSONArray()
    bookings.forEach { b ->
        array.put(JSONObject().put("room", b.room).put("date", b.date)
            .put("start", b.start).put("minutes", b.minutes).put("name", b.name))
    }
    context.getSharedPreferences("banya", Context.MODE_PRIVATE).edit()
        .putString("bookings", array.toString()).apply()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AuthGate { BanyaApp(this) } }
    }
}

@Composable
fun BanyaApp(context: Context) {
    var page by remember { mutableIntStateOf(0) }
    var selectedRoom by remember { mutableIntStateOf(1) }
    val bookings = remember { mutableStateListOf<Booking>() }
    val cloud = remember { CloudBookings() }
    var serverReady by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var syncStatus by remember { mutableStateOf("Серверга уланмоқда...") }
    DisposableEffect(cloud) {
        val registration = cloud.listen { records, error ->
            if (records != null) {
                bookings.clear()
                bookings.addAll(records)
                serverReady = true
                syncStatus = "Онлайн"
            } else {
                serverReady = false
                syncStatus = error ?: "Сервер кутилмоқда"
            }
        }
        onDispose { registration.remove() }
    }
    var date by remember { mutableStateOf(LocalDate.now().format(dateFmt)) }
    var time by remember { mutableStateOf("16:00") }
    var name by remember { mutableStateOf("") }
    var duration by remember { mutableIntStateOf(60) }
    var message by remember { mutableStateOf("") }
    val today = LocalDate.now().format(dateFmt)
    val now = LocalTime.now()

    Column(Modifier.fillMaxSize().background(Wood).padding(16.dp)) {
        Spacer(Modifier.height(16.dp))
        Text("♛ БАНЯ БРОН", color = Gold, fontSize = 29.sp,
            fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth())
        Text("16:00 – 23:00 • $syncStatus", color = Color.White, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 18.dp))
        when (page) {
            0 -> {
                LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items((1..6).toList()) { room ->
                        val occupied = bookings.any {
                            it.room == room && it.date == today &&
                            !now.isBefore(LocalTime.parse(it.start)) &&
                            now.isBefore(LocalTime.parse(it.start).plusMinutes(it.minutes.toLong()))
                        }
                        Column(Modifier.background(Panel, RoundedCornerShape(16.dp))
                            .border(1.dp, Gold, RoundedCornerShape(16.dp)).padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("$room-ХОНА", color = Gold, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(18.dp))
                            Text(if (occupied) "Банд" else "Бўш",
                                color = if (occupied) Red else Green, fontSize = 22.sp)
                            Spacer(Modifier.height(16.dp))
                            Button(onClick = { selectedRoom = room; message = ""; page = 1 },
                                colors = ButtonDefaults.buttonColors(containerColor = Gold)) {
                                Text("Брон қилиш", color = Wood, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
            1 -> {
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    Text("Брон қилиш", color = Gold, fontSize = 24.sp)
                    Spacer(Modifier.height(12.dp))
                    Text("Хона", color = Color.White)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        (1..6).forEach { r ->
                            OutlinedButton(onClick = { selectedRoom = r }, contentPadding = PaddingValues(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = if (r == selectedRoom) Gold else Color.White)) {
                                Text("$r")
                            }
                        }
                    }
                    BookingField("Сана (ЙЙЙЙ-ОО-КК)", date) { date = it }
                    BookingField("Бошланиш вақти (СС:ДД)", time) { time = it }
                    BookingField("Мижоз исми", name) { name = it }
                    Text("Давомийлиги", color = Color.White, modifier = Modifier.padding(top = 12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(60 to "1 соат", 90 to "1,5 соат", 120 to "2 соат").forEach { (m, label) ->
                            OutlinedButton(onClick = { duration = m }, contentPadding = PaddingValues(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = if (m == duration) Gold else Color.White)) {
                                Text(label, fontSize = 12.sp)
                            }
                        }
                    }
                    Text("Жами: ${duration * 50} тг", color = Gold, fontSize = 23.sp,
                        modifier = Modifier.padding(vertical = 14.dp))
                    Button(enabled = serverReady && !saving, onClick = {
                        val d = runCatching { LocalDate.parse(date, dateFmt) }.getOrNull()
                        val t = runCatching { LocalTime.parse(time, DateTimeFormatter.ofPattern("HH:mm")) }.getOrNull()
                        val end = t?.plusMinutes(duration.toLong())
                        message = when {
                            d == null || t == null -> "Сана ёки вақт формати нотўғри"
                            d.isBefore(LocalDate.now()) -> "Ўтган санага брон қилиб бўлмайди"
                            name.isBlank() -> "Мижоз исмини киритинг"
                            t.minute % 30 != 0 -> "Вақт 00 ёки 30 дақиқадан бошлансин"
                            t.isBefore(LocalTime.of(16, 0)) || end!!.isAfter(LocalTime.of(23, 0)) ||
                                end.isBefore(t) -> "Иш вақти 16:00–23:00"
                            bookings.any { b ->
                                b.room == selectedRoom && b.date == date &&
                                t.isBefore(LocalTime.parse(b.start).plusMinutes(b.minutes.toLong())) &&
                                LocalTime.parse(b.start).isBefore(end)
                            } -> "Бу вақтда хона банд. Бошқа вақтни танланг"
                            else -> {
                                saving = true
                                cloud.reserve(Booking(selectedRoom, date, time, duration, name.trim())) { ok, result ->
                                    saving = false
                                    message = result
                                    if (ok) name = ""
                                }
                                "Сервер тасдиғи кутилмоқда..."
                            }
                        }
                    }, colors = ButtonDefaults.buttonColors(containerColor = Gold),
                        modifier = Modifier.fillMaxWidth()) {
                        Text("Бронни сақлаш", color = Wood)
                    }
                    if (message.isNotBlank()) Text(message, color = Gold, modifier = Modifier.padding(top = 12.dp))
                    Text("Сақланган бронлар", color = Gold, fontSize = 19.sp, modifier = Modifier.padding(top = 18.dp))
                    bookings.sortedWith(compareBy<Booking> { it.date }.thenBy { it.start })
                        .forEach { b ->
                            Text("${b.date} • ${b.room}-хона • ${b.start}–${b.end} • ${b.name} • ${b.price} тг",
                                color = Color.White, modifier = Modifier.padding(vertical = 6.dp))
                        }
                }
            }
            else -> {
                Column(Modifier.weight(1f)) {
                    Text("Созламалар", color = Gold, fontSize = 24.sp)
                    Text("Иш вақти: 16:00–23:00", color = Color.White)
                    Text("1 соат — 3000 тг", color = Color.White)
                    Text("1,5 соат — 4500 тг", color = Color.White)
                    Text("2 соат — 6000 тг", color = Color.White)
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            listOf("Асосий", "Брон қилиш", "Созламалар").forEachIndexed { index, title ->
                TextButton(onClick = { page = index }) {
                    Text(title, color = if (page == index) Gold else Color.White, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun BookingField(label: String, value: String, onValueChange: (String) -> Unit) {
    Text(label, color = Color.White, modifier = Modifier.padding(top = 12.dp))
    OutlinedTextField(value = value, onValueChange = onValueChange,
        singleLine = true, modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White, unfocusedTextColor = Color.White,
            focusedBorderColor = Gold, unfocusedBorderColor = Gold,
            cursorColor = Gold))
}
