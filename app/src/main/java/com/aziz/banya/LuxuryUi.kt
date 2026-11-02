package com.aziz.banya

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val LuxGold = Color(0xFFF8C85B)
val LuxDark = Color(0xFF100C09)
val LuxGreen = Color(0xFF18C64D)
val LuxRed = Color(0xFFE42E2A)
private val GoldBrush = Brush.verticalGradient(listOf(Color(0xFFFFFFBE), Color(0xFFF9BF32), Color(0xFF995004)))

@Composable
fun WoodBackground(content: @Composable BoxScope.() -> Unit) {
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
        listOf(Color(0xFF070B12), Color(0xFF281204), Color(0xFF3E1C08), Color(0xFF251105), Color(0xFF090A10))
    ))) {
        Canvas(Modifier.fillMaxSize()) {
            val plank = size.width / 6f
            for (i in 0..6) {
                val x = plank * i
                drawLine(Color(0x334F2D13), Offset(x, 0f), Offset(x, size.height), strokeWidth = 5f)
                drawLine(Color(0x225F3A1D), Offset(x + 6f, 0f), Offset(x + 6f, size.height), strokeWidth = 2f)
            }
            for (i in 1..15) {
                val y = i * size.height / 15f
                drawLine(Color(0x1E8D5527), Offset(0f, y), Offset(size.width, y + 5f), strokeWidth = 2f)
            }
        }
        content()
    }
}

@Composable
fun LuxuryPanel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.clip(RoundedCornerShape(18.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFF08385D), Color(0xFF000A18), Color(0xFF00101E))))
            .border(1.5.dp, GoldBrush, RoundedCornerShape(18.dp))
            .padding(12.dp),
        content = content
    )
}

@Composable
fun LuxuryButton(title: String, onClick: () -> Unit, modifier: Modifier = Modifier,
                 enabled: Boolean = true, danger: Boolean = false, positive: Boolean = false) {
    val brush = when {
        danger -> Brush.verticalGradient(listOf(Color(0xFFFF7C64), Color(0xFFC81716), Color(0xFF6D0807)))
        positive -> Brush.verticalGradient(listOf(Color(0xFF72FF70), Color(0xFF159C1D), Color(0xFF086412)))
        else -> GoldBrush
    }
    Box(modifier.height(49.dp).clip(RoundedCornerShape(16.dp))
        .background(if (enabled) brush else Brush.verticalGradient(listOf(Color.DarkGray, Color.Gray)))
        .border(1.dp, if (enabled) LuxGold else Color.Gray, RoundedCornerShape(16.dp))
        .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center) {
        Text(title, color = if (danger || positive) Color.White else Color(0xFF201106),
            fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, textAlign = TextAlign.Center)
    }
}

@Composable
fun LuxuryHeader(online: Boolean, status: String) {
    Column(Modifier.fillMaxWidth().padding(top = 9.dp, bottom = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Text("♛", fontSize = 36.sp, color = LuxGold, lineHeight = 38.sp)
        Text("БАНЯ БРОН", color = LuxGold, fontSize = 29.sp, fontWeight = FontWeight.Black)
        Text("16:00 – 23:00  •  " + status, color = if (online) LuxGreen else Color(0xFFFFCC82),
            fontSize = 13.sp, textAlign = TextAlign.Center)
    }
}

@Composable
fun LuxuryNav(selected: Int, onSelect: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().background(Color(0xF0001326))
        .border(1.dp, Color(0xFFB58330), RoundedCornerShape(12.dp))
        .padding(vertical = 5.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
        listOf("⌂" to "Асосий", "◷" to "Брон қилиш", "⚙" to "Созламалар")
            .forEachIndexed { i, item ->
                Column(Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                    .background(if (selected == i) Color(0xFF99580A) else Color.Transparent)
                    .clickable { onSelect(i) }.padding(5.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(item.first, color = if (selected == i) LuxGold else Color.White, fontSize = 25.sp)
                    Text(item.second, color = if (selected == i) LuxGold else Color.White, fontSize = 11.sp)
                }
            }
    }
}
