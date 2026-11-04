package com.aziz.banya

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate

@Composable
fun DailyRevenueReport(ledger: PaymentLedger = remember { PaymentLedger() }) {
    var day by remember { mutableStateOf(LocalDate.now()) }
    var receipts by remember { mutableStateOf<List<PaymentReceipt>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }

    DisposableEffect(day, ledger) {
        loading = true
        error = ""
        val registration = ledger.listenForDate(day.toString()) { data, failure ->
            if (data != null) {
                receipts = data
                loading = false
            } else {
                error = failure ?: "Ҳисоботни олиб бўлмади"
                loading = false
            }
        }
        onDispose { registration.remove() }
    }

    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Кунлик даромад ҳисоботи", color = LuxGold, fontSize = 22.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TextButton(onClick = { day = day.minusDays(1) }) { Text("❮", color = LuxGold) }
            Text(day.toString(), color = Color.White, modifier = Modifier.weight(1f))
            TextButton(onClick = { day = day.plusDays(1) }) { Text("❯", color = LuxGold) }
        }
        if (loading) {
            Text("Ҳисобот юкланмоқда...", color = LuxGold)
        } else if (error.isNotEmpty()) {
            Text(error, color = Color.Red)
        } else {
            LuxuryPanel(Modifier.fillMaxWidth()) {
                Text("Жами: ${receipts.sumOf { it.amountKzt.toLong() }} тг",
                    color = LuxGold, fontSize = 22.sp)
                Text("Тўловлар сони: ${receipts.size}", color = Color.White)
            }
            listOf("cash" to "Нақд", "kaspi" to "Kaspi", "card" to "Карта").forEach { (method, title) ->
                LuxuryPanel(Modifier.fillMaxWidth()) {
                    Text("$title: ${receipts.filter { it.method == method }.sumOf { it.amountKzt.toLong() }} тг",
                        color = Color.White)
                }
            }
            LuxuryPanel(Modifier.fillMaxWidth()) {
                Text("Хоналар бўйича", color = LuxGold)
                (1..6).forEach { room ->
                    Text("$room-хона: ${receipts.filter { it.room == room }.sumOf { it.amountKzt.toLong() }} тг",
                        color = Color.White)
                }
            }
            Text("Фақат сақланган тўловлар ҳисобланади. Ўчирилган броннинг тўлови сақланади.",
                color = LuxGold, fontSize = 12.sp)
        }
    }
}
