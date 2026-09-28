package com.callankan.poloapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.callankan.poloapp.ui.theme.SpaceGrotesk

/** Matrícula española en miniatura: franja azul europea con la "E". */
@Composable
fun SpanishPlate(plate: String, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(5.dp)
    Row(
        modifier
            .height(26.dp)
            .clip(shape)
            .background(Color.White)
            .border(1.dp, Color(0xFF1B1B1B), shape),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            Modifier
                .fillMaxHeight()
                .width(14.dp)
                .background(Color(0xFF1F3F9E)),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom,
        ) {
            Text("E", style = TextStyle(color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold), modifier = Modifier.padding(bottom = 2.dp))
        }
        Box(Modifier.padding(horizontal = 8.dp)) {
            Text(
                plate.ifBlank { "SIN MATRÍCULA" }.uppercase(),
                style = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 14.sp, letterSpacing = 1.5.sp, color = Color(0xFF111111)),
                maxLines = 1,
            )
        }
    }
}
