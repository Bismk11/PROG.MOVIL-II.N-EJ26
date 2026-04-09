package com.example.mapasgeolocalizacion

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.mapasgeolocalizacion.ui.theme.MapasGeolocalizacionTheme
import android.Manifest
import android.content.pm.PackageManager
import android.preference.PreferenceManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.mapasgeolocalizacion.data.RetrofitClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //Configuración para Osmdroid
        Configuration.getInstance().load(
            applicationContext,
            PreferenceManager.getDefaultSharedPreferences(applicationContext)
        )
        Configuration.getInstance().userAgentValue = packageName

        setContent {
            MaterialTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    MapLocationScreen()
                }
            }
        }
    }
}

@Composable
fun MapLocationScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    //Estado para la ubicación actual y la configuración de "Casa"
    var currentLocation by remember { mutableStateOf<GeoPoint?>(null) }

    //Dirección de casa configurable
    var homeLatText by remember { mutableStateOf("37.403317") }

    var homeLonText by remember { mutableStateOf("-121.969377") }

    var routePoints by remember { mutableStateOf<List<GeoPoint>>(emptyList()) }
    var mapReference by remember { mutableStateOf<MapView?>(null) }

    //Launcher para solicitar permisos de ubicación
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            //Permiso concedido, obtener ubicación
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null) {
                        currentLocation = GeoPoint(location.latitude, location.longitude)
                        mapReference?.controller?.setCenter(currentLocation)
                    }
                }
            } catch (e: SecurityException) { e.printStackTrace() }
        }
    }

    //Solicitar permisos al iniciar el Composable
    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) currentLocation = GeoPoint(location.latitude, location.longitude)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        //Menú de Configuración de Casa
        Card(modifier = Modifier.padding(8.dp).fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Configurar Dirección de Casa", style = MaterialTheme.typography.titleMedium)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = homeLatText,
                        onValueChange = { homeLatText = it },
                        label = { Text("Latitud") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = homeLonText,
                        onValueChange = { homeLonText = it },
                        label = { Text("Longitud") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Button(
                    onClick = {
                        coroutineScope.launch {
                            val start = currentLocation
                            val homeLat = homeLatText.toDoubleOrNull()
                            val homeLon = homeLonText.toDoubleOrNull()

                            if (start != null && homeLat != null && homeLon != null) {
                                try {
                                    val apiKey = "eyJvcmciOiI1YjNjZTM1OTc4NTExMTAwMDFjZjYyNDgiLCJpZCI6IjdlMzExZDRkYjI3MTQzNGY4Yzc2MWMyYTRiNzVhNTA1IiwiaCI6Im11cm11cjY0In0="

                                    //OpenRouteService requiere formato "Longitud,Latitud"
                                    val startStr = "${start.longitude},${start.latitude}"
                                    val endStr = "$homeLon,$homeLat"

                                    val response = RetrofitClient.apiService.getDirections(apiKey, startStr, endStr)

                                    if (response.features.isNotEmpty()) {
                                        val coordinates = response.features[0].geometry.coordinates
                                        //Se convierte [Lon, Lat] a GeoPoint(Lat, Lon) de Osmdroid
                                        routePoints = coordinates.map { GeoPoint(it[1], it[0]) }
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }
                    },
                    modifier = Modifier.padding(top = 8.dp).fillMaxWidth()
                ) {
                    Text("Trazar Ruta a Casa")
                }
            }
        }

        //Contenedor del Mapa (Osmdroid en Compose)
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                MapView(ctx).apply {
                    setMultiTouchControls(true)
                    controller.setZoom(15.0)
                    mapReference = this
                }
            },
            update = { mapView ->
                mapView.overlays.clear() //Limpiar trazados anteriores

                //Marcador de Ubicación Actual
                currentLocation?.let { loc ->
                    val startMarker = Marker(mapView)
                    startMarker.position = loc
                    startMarker.title = "Mi Ubicación"
                    mapView.overlays.add(startMarker)
                    mapView.controller.setCenter(loc)
                }

                //Marcador de Casa
                val homeLat = homeLatText.toDoubleOrNull()
                val homeLon = homeLonText.toDoubleOrNull()
                if (homeLat != null && homeLon != null) {
                    val homeMarker = Marker(mapView)
                    homeMarker.position = GeoPoint(homeLat, homeLon)
                    homeMarker.title = "Casa"
                    mapView.overlays.add(homeMarker)
                }

                //Dibujar la Polilínea de la Ruta
                if (routePoints.isNotEmpty()) {
                    val line = Polyline()
                    line.setPoints(routePoints)
                    line.color = android.graphics.Color.BLUE
                    line.width = 8f
                    mapView.overlays.add(line)
                }

                mapView.invalidate() //Refrescar el mapa
            }
        )
    }
}