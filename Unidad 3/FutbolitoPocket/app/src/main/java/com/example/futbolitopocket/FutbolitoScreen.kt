package com.example.futbolitopocket

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.ricknout.composesensors.accelerometer.rememberAccelerometerSensorValueAsState

@Composable
fun FutbolitoPocket() {
    // 1. Leer el acelerómetro
    val sensorValue by rememberAccelerometerSensorValueAsState()

    // 2. Estados del juego
    var ballPosition by remember { mutableStateOf(Offset(200f, 200f)) }
    var score by remember { mutableIntStateOf(0) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    val ballRadius = with(LocalDensity.current) { 15.dp.toPx() }
    val goalWidth = with(LocalDensity.current) { 120.dp.toPx() }

    // 3. Lógica de movimiento y colisiones
    LaunchedEffect(sensorValue) {
        val (accelX, accelY) = sensorValue.value
        if (canvasSize != IntSize.Zero) {
            // Movimiento basado en inclinación
            var newX = ballPosition.x - (accelX * 7f)
            var newY = ballPosition.y + (accelY * 7f)

            // Rebote en paredes laterales
            if (newX < ballRadius) newX = ballRadius
            if (newX > canvasSize.width - ballRadius) newX = canvasSize.width.toFloat() - ballRadius

            // Lógica de Portería (Abajo)
            val center = canvasSize.width / 2f
            val isInGoalRange = newX > (center - goalWidth / 2) && newX < (center + goalWidth / 2)

            if (newY > canvasSize.height - ballRadius) {
                if (isInGoalRange) {
                    score++ // ¡GOL!
                    newX = canvasSize.width / 2f // Reset al centro
                    newY = canvasSize.height / 2f
                } else {
                    newY = canvasSize.height - ballRadius // Rebota
                }
            }
            if (newY < ballRadius) newY = ballRadius

            ballPosition = Offset(newX, newY)
        }
    }

    // 4. Interfaz Visual
    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFF2E7D32)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "GOLES: $score",
            color = Color.White,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(16.dp)
        )

        Box(
            modifier = Modifier.fillMaxSize().padding(16.dp).onSizeChanged { canvasSize = it }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height

                // Cancha
                drawRect(Color.White, style = Stroke(5f))
                drawLine(Color.White, Offset(0f, height/2), Offset(width, height/2), strokeWidth = 5f)
                drawCircle(Color.White, center = Offset(width/2, height/2), radius = 100f, style = Stroke(5f))

                // Portería Amarilla
                drawRect(
                    color = Color.Yellow,
                    topLeft = Offset(width / 2 - goalWidth / 2, height - 15f),
                    size = androidx.compose.ui.geometry.Size(goalWidth, 30f)
                )

                // Pelota
                drawCircle(color = Color.White, radius = ballRadius, center = ballPosition)
            }
        }
    }
}