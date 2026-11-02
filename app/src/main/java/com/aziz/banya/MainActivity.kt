package com.aziz.banya

import android.os.Bundle
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

private val Gold = Color(0xFFE8BE69)
private val Wood = Color(0xFF211811)
private val Panel = Color(0xFF39271B)
private val Green = Color(0xFF299F61)
private val Red = Color(0xFFBF4343)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { BanyaApp() }
    }
}

@Composable
fun BanyaApp() {
    var page by remember { mutableStateOf(0) }
    // Фақат маҳаллий намойиш ҳолати. Ҳақиқий брон ва икки телефон синхронизацияси кейин қўшилади.
    val busy = remember { mutableStateListOf<Int>() }
    Column(Modifier.fillMaxSize().background(Wood).padding(16.dp)) {
        Spacer(Modifier.height(20.dp))
        Text("♛ БАНЯ БРОН", color = Gold, fontSize = 30.sp,
            fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth())
        Text("16:00 – 23:00", color = Color.White, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 22.dp))
        when (page) {
            0 -> {
                LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items((1..6).toList()) { room ->
                        val occupied = room in busy
                        Column(Modifier.background(Panel, RoundedCornerShape(16.dp))
                            .border(1.dp, Gold, RoundedCornerShape(16.dp))
                            .padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("$room-ХОНА", color = Gold, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(20.dp))
                            Text(if (occupied) "Банд" else "Бўш",
                                color = if (occupied) Red else Green,
                                fontSize = 22.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(18.dp))
                            Button(onClick = { page = 1 },
                                colors = ButtonDefaults.buttonColors(containerColor = Gold)) {
                                Text("Брон қилиш", color = Wood, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
            1 -> {
                Text("Брон қилиш", color = Gold, fontSize = 24.sp)
                Text("Бу экран кейинги босқичда тўлиқ ишга туширилади.",
                    color = Color.White, modifier = Modifier.padding(top = 20.dp))
                Spacer(Modifier.weight(1f))
            }
            else -> {
                Text("Созламалар", color = Gold, fontSize = 24.sp)
                Text("Иш вақти: 16:00–23:00", color = Color.White)
                Text("1 соат — 3000 тг", color = Color.White)
                Text("1,5 соат — 4500 тг", color = Color.White)
                Text("2 соат — 6000 тг", color = Color.White)
                Spacer(Modifier.weight(1f))
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
