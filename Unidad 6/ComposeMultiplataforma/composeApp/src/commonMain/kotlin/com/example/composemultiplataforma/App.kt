package com.example.composemultiplataforma

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import composemultiplataforma.composeapp.generated.resources.Res
import composemultiplataforma.composeapp.generated.resources.eg
import composemultiplataforma.composeapp.generated.resources.fr
import composemultiplataforma.composeapp.generated.resources.id
import composemultiplataforma.composeapp.generated.resources.jp
import composemultiplataforma.composeapp.generated.resources.mx
import kotlin.time.Clock
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

data class Country(val name: String, val zone: TimeZone, val image: DrawableResource)

fun currentTimeAt(location: String, zone: TimeZone): String {
    // Usamos el Clock del sistema de forma segura para evitar conflictos en Wasm
    val time = Clock.System.now()
    val localDateTime = time.toLocalDateTime(zone)

    val hours = localDateTime.hour.toString().padStart(2, '0')
    val minutes = localDateTime.minute.toString().padStart(2, '0')
    val seconds = localDateTime.second.toString().padStart(2, '0')

    return "The time in $location is $hours:$minutes:$seconds"
}

@Composable
@Preview
fun App() {
    MaterialTheme {
        var countries by remember { mutableStateOf<List<Country>>(emptyList()) }
        var showCountries by remember { mutableStateOf(false) }
        var timeAtLocation by remember { mutableStateOf("No location selected") }

        // Inicialización segura de zonas horarias diferida
        LaunchedEffect(Unit) {
            val list = mutableListOf<Country>()

            fun addCountry(name: String, zoneId: String, fallbackOffset: String, resource: DrawableResource) {
                try {
                    list.add(Country(name, TimeZone.of(zoneId), resource))
                } catch (e: Exception) {
                    try {
                        // Fallback a offset si el ID de zona no es reconocido por el navegador
                        list.add(Country(name, TimeZone.of(fallbackOffset), resource))
                    } catch (e2: Exception) {
                        list.add(Country("$name (UTC)", TimeZone.UTC, resource))
                    }
                }
            }

            addCountry("Japan", "Asia/Tokyo", "GMT+09:00", Res.drawable.jp)
            addCountry("France", "Europe/Paris", "GMT+01:00", Res.drawable.fr)
            addCountry("Mexico", "America/Mexico_City", "GMT-06:00", Res.drawable.mx)
            addCountry("Indonesia", "Asia/Jakarta", "GMT+07:00", Res.drawable.id)
            addCountry("Egypt", "Africa/Cairo", "GMT+02:00", Res.drawable.eg)

            if (list.isEmpty()) {
                list.add(Country("Universal Time", TimeZone.UTC, Res.drawable.fr))
            }

            countries = list
        }

        Column(
            modifier = Modifier
                .padding(20.dp)
                .safeContentPadding()
                .fillMaxSize(),
        ) {
            Text(
                timeAtLocation,
                style = TextStyle(fontSize = 20.sp),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().align(Alignment.CenterHorizontally)
            )

            Box(modifier = Modifier.padding(start = 20.dp, top = 10.dp)) {
                Button(
                    onClick = { showCountries = !showCountries }
                ) {
                    Text("Select Location")
                }

                DropdownMenu(
                    expanded = showCountries,
                    onDismissRequest = { showCountries = false }
                ) {
                    countries.forEach { country ->
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Image(
                                        painterResource(country.image),
                                        modifier = Modifier.size(50.dp).padding(end = 10.dp),
                                        contentDescription = "${country.name} flag"
                                    )
                                    Text(country.name)
                                }
                            },
                            onClick = {
                                timeAtLocation = currentTimeAt(country.name, country.zone)
                                showCountries = false
                            }
                        )
                    }
                }
            }
        }
    }
}