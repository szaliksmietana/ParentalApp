package com.example.parentalapp.ui

import android.app.NotificationManager
import android.content.Context
import android.graphics.drawable.Drawable
import android.preference.PreferenceManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.parentalapp.ChildData
import com.example.parentalapp.network.LocationResponse
import com.example.parentalapp.network.RetrofitInstance
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.Polyline
import kotlin.math.*

// --- Model strefy bezpieczeństwa ---
data class SafeZone(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Double
)

// --- Zapis/odczyt stref per-dziecko w SharedPreferences ---
object ZoneStorage {
    private const val PREFS_NAME = "safe_zones"
    private val gson = Gson()

    private fun key(childDeviceId: String) = "zones_$childDeviceId"

    fun saveZones(context: Context, childDeviceId: String, zones: List<SafeZone>) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(key(childDeviceId), gson.toJson(zones)).apply()
    }

    fun loadZones(context: Context, childDeviceId: String): List<SafeZone> {
        val json = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(key(childDeviceId), null) ?: return emptyList()
        return try {
            gson.fromJson(json, object : TypeToken<List<SafeZone>>() {}.type)
        } catch (e: Exception) {
            emptyList()
        }
    }
}

// --- Oblicz odległość między dwoma punktami (metry) ---
fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6371000.0
    val phi1 = Math.toRadians(lat1)
    val phi2 = Math.toRadians(lat2)
    val dPhi = Math.toRadians(lat2 - lat1)
    val dLambda = Math.toRadians(lon2 - lon1)
    val a = sin(dPhi / 2).pow(2) + cos(phi1) * cos(phi2) * sin(dLambda / 2).pow(2)
    return r * 2 * atan2(sqrt(a), sqrt(1 - a))
}

// --- Powiadomienie o wyjściu ze strefy ---
fun showGeofenceNotification(context: Context, childName: String, zoneName: String, distanceM: Double) {
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val notification = NotificationCompat.Builder(context, "geofence_alerts")
        .setSmallIcon(android.R.drawable.ic_dialog_alert)
        .setContentTitle("⚠️ $childName wyszło ze strefy!")
        .setContentText("Strefa: $zoneName • Odległość: ${distanceM.toInt()} m")
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)
        .build()
    manager.notify(("geofence_$childName$zoneName").hashCode(), notification)
}

// --- Generuj punkty okręgu do Polygon ---
fun circlePoints(center: GeoPoint, radiusMeters: Double, points: Int = 64): List<GeoPoint> {
    return (0 until points).map { i ->
        val angle = Math.toRadians(i * 360.0 / points)
        val dx = radiusMeters * cos(angle)
        val dy = radiusMeters * sin(angle)
        val dLat = dx / 111320.0
        val dLon = dy / (111320.0 * cos(Math.toRadians(center.latitude)))
        GeoPoint(center.latitude + dLat, center.longitude + dLon)
    }
}

// --- Źródła kafelków dla stylów mapy ---
private val TILE_SOURCE_SATELLITE = XYTileSource(
    "Esri WorldImagery",
    0, 19, 256, ".jpg",
    arrayOf("https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/")
)

private val TILE_SOURCE_TERRAIN = XYTileSource(
    "OpenTopoMap",
    0, 17, 256, ".png",
    arrayOf("https://tile.opentopomap.org/")
)

data class ChildLocation(
    val child: ChildData,
    val location: LocationResponse
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    childrenList: List<ChildData>,
    selectedChild: ChildData?,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val currentSettings = SettingsManager.settings  // Reaktywny odczyt stylu mapy

    var childLocations by remember { mutableStateOf<List<ChildLocation>>(emptyList()) }
    var historyPoints by remember { mutableStateOf<List<LocationResponse>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isHistoryLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showHistory by remember { mutableStateOf(false) }
    var selectedHours by remember { mutableIntStateOf(24) }

    var safeZones by remember {
        mutableStateOf(
            if (selectedChild != null) ZoneStorage.loadZones(context, selectedChild.code)
            else emptyList()
        )
    }

    var showAddZoneDialog by remember { mutableStateOf(false) }
    var pendingZonePoint by remember { mutableStateOf<GeoPoint?>(null) }
    var newZoneName by remember { mutableStateOf("") }
    var newZoneRadius by remember { mutableStateOf("500") }

    val hourOptions = listOf(1, 6, 24, 48)
    val primaryColor = MaterialTheme.colorScheme.primary.toArgb()

    val zoneIcon: Drawable? = remember {
        ContextCompat.getDrawable(context, android.R.drawable.ic_menu_compass)
    }

    remember {
        Configuration.getInstance().load(context, PreferenceManager.getDefaultSharedPreferences(context))
        Configuration.getInstance().userAgentValue = context.packageName
    }

    LaunchedEffect(selectedChild) {
        isLoading = true
        errorMessage = null
        safeZones = if (selectedChild != null) ZoneStorage.loadZones(context, selectedChild.code) else emptyList()

        val toFetch = if (selectedChild != null) listOf(selectedChild) else childrenList
        val results = mutableListOf<ChildLocation>()
        for (child in toFetch) {
            try {
                val loc = RetrofitInstance.api.getLatestLocation(child.code)
                results.add(ChildLocation(child, loc))
            } catch (_: Exception) {}
        }

        childLocations = results
        isLoading = false
        if (results.isEmpty()) errorMessage = "Brak danych lokalizacji"
    }

    LaunchedEffect(showHistory, selectedHours, selectedChild) {
        if (showHistory && selectedChild != null) {
            isHistoryLoading = true
            historyPoints = try {
                RetrofitInstance.api.getLocationHistory(selectedChild.code, selectedHours)
            } catch (_: Exception) { emptyList() }
            isHistoryLoading = false
        } else {
            historyPoints = emptyList()
        }
    }

    if (showAddZoneDialog && pendingZonePoint != null) {
        AlertDialog(
            onDismissRequest = { showAddZoneDialog = false; newZoneName = ""; newZoneRadius = "500" },
            title = { Text("Dodaj strefę dla ${selectedChild?.name}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Lokalizacja: ${String.format("%.4f", pendingZonePoint!!.latitude)}, ${String.format("%.4f", pendingZonePoint!!.longitude)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    OutlinedTextField(
                        value = newZoneName,
                        onValueChange = { newZoneName = it },
                        label = { Text("Nazwa strefy (np. Dom, Szkoła)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newZoneRadius,
                        onValueChange = { newZoneRadius = it.filter { c -> c.isDigit() } },
                        label = { Text("Promień (metry)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val radius = newZoneRadius.toDoubleOrNull() ?: 500.0
                        if (newZoneName.isNotBlank() && selectedChild != null) {
                            val zone = SafeZone(
                                name = newZoneName,
                                latitude = pendingZonePoint!!.latitude,
                                longitude = pendingZonePoint!!.longitude,
                                radiusMeters = radius.coerceIn(50.0, 5000.0)
                            )
                            val updated = safeZones + zone
                            safeZones = updated
                            ZoneStorage.saveZones(context, selectedChild.code, updated)
                        }
                        showAddZoneDialog = false; newZoneName = ""; newZoneRadius = "500"
                    },
                    enabled = newZoneName.isNotBlank()
                ) { Text("Dodaj") }
            },
            dismissButton = {
                TextButton(onClick = { showAddZoneDialog = false; newZoneName = ""; newZoneRadius = "500" }) {
                    Text("Anuluj")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(selectedChild?.let { "Lokalizacja: ${it.name}" } ?: "Wszystkie dzieci") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Wróć")
                    }
                },
                actions = {
                    if (selectedChild != null) {
                        IconButton(onClick = { showHistory = !showHistory }) {
                            Icon(
                                Icons.Filled.Home,
                                contentDescription = "Historia trasy",
                                tint = if (showHistory) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (errorMessage != null && childLocations.isEmpty()) {
                Text(text = errorMessage!!, modifier = Modifier.align(Alignment.Center), color = MaterialTheme.colorScheme.error)
            } else {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        MapView(ctx).apply {
                            // Styl mapy z ustawień
                            setTileSource(when (currentSettings.mapStyle) {
                                MapStyle.TERRAIN -> TILE_SOURCE_TERRAIN
                                MapStyle.STANDARD -> TileSourceFactory.MAPNIK
                            })
                            setMultiTouchControls(true)
                        }
                    },
                    update = { mapView ->
                        // Aktualizuj styl mapy gdy zmieni się ustawienie
                        mapView.setTileSource(when (currentSettings.mapStyle) {
                            MapStyle.TERRAIN -> TILE_SOURCE_TERRAIN
                            MapStyle.STANDARD -> TileSourceFactory.MAPNIK
                        })

                        mapView.overlays.clear()

                        val eventsOverlay = MapEventsOverlay(object : MapEventsReceiver {
                            override fun singleTapConfirmedHelper(p: GeoPoint?) = false
                            override fun longPressHelper(p: GeoPoint?): Boolean {
                                if (selectedChild != null) {
                                    p?.let { pendingZonePoint = it; showAddZoneDialog = true }
                                }
                                return true
                            }
                        })
                        mapView.overlays.add(eventsOverlay)

                        if (selectedChild != null) {
                            safeZones.forEach { zone ->
                                val center = GeoPoint(zone.latitude, zone.longitude)
                                val polygon = Polygon(mapView).apply {
                                    points = circlePoints(center, zone.radiusMeters)
                                    fillPaint.color = (primaryColor and 0x00FFFFFF) or 0x33000000
                                    outlinePaint.color = primaryColor
                                    outlinePaint.strokeWidth = 3f
                                    title = zone.name
                                    snippet = "Promień: ${zone.radiusMeters.toInt()} m"
                                }
                                mapView.overlays.add(polygon)

                                val zoneMarker = Marker(mapView).apply {
                                    position = center
                                    title = "🏠 ${zone.name}"
                                    snippet = "Promień: ${zone.radiusMeters.toInt()} m"
                                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                    zoneIcon?.let { icon = it }
                                }
                                mapView.overlays.add(zoneMarker)
                            }
                        }

                        if (historyPoints.size >= 2) {
                            val polyline = Polyline(mapView).apply {
                                outlinePaint.color = primaryColor
                                outlinePaint.strokeWidth = 7f
                                setPoints(historyPoints.map { GeoPoint(it.latitude, it.longitude) })
                            }
                            mapView.overlays.add(polyline)
                            historyPoints.dropLast(1).forEachIndexed { index, point ->
                                val marker = Marker(mapView).apply {
                                    position = GeoPoint(point.latitude, point.longitude)
                                    title = "Punkt ${index + 1} z ${historyPoints.size}"
                                    snippet = point.recorded_at.take(16).replace("T", " ")
                                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                                }
                                mapView.overlays.add(marker)
                            }
                        }

                        childLocations.forEachIndexed { index, childLoc ->
                            val geoPoint = GeoPoint(childLoc.location.latitude, childLoc.location.longitude)
                            if (index == 0) { mapView.controller.setZoom(15.0); mapView.controller.setCenter(geoPoint) }

                            val childZones = if (selectedChild != null) ZoneStorage.loadZones(context, childLoc.child.code) else emptyList()
                            val statusText = childZones.joinToString("\n") { zone ->
                                val dist = distanceMeters(childLoc.location.latitude, childLoc.location.longitude, zone.latitude, zone.longitude)
                                "${if (dist <= zone.radiusMeters) "✅" else "⚠️"} ${zone.name}: ${dist.toInt()} m"
                            }

                            val marker = Marker(mapView).apply {
                                position = geoPoint
                                title = childLoc.child.name
                                snippet = buildString {
                                    append("Ostatnia aktualizacja: ${childLoc.location.recorded_at.take(16).replace("T", " ")}")
                                    childLoc.location.battery_level?.let { append("\nBateria: $it%") }
                                    childLoc.location.accuracy_meters?.let { append("\nDokładność: ${it.toInt()} m") }
                                    if (statusText.isNotEmpty()) append("\n$statusText")
                                }
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            }
                            mapView.overlays.add(marker)
                        }

                        mapView.invalidate()
                    }
                )

                if (selectedChild != null && safeZones.isEmpty()) {
                    Card(
                        modifier = Modifier.align(Alignment.TopCenter).padding(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Text(
                            text = "💡 Przytrzymaj punkt na mapie aby dodać strefę dla ${selectedChild.name}",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }

                if (selectedChild != null && safeZones.isNotEmpty()) {
                    Card(
                        modifier = Modifier.align(Alignment.TopCenter).padding(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text("Strefy ${selectedChild.name}:", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(bottom = 4.dp))
                            safeZones.forEach { zone ->
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = "🏠 ${zone.name} (${zone.radiusMeters.toInt()} m)",
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.weight(1f)
                                    )
                                    TextButton(onClick = {
                                        val updated = safeZones.filter { it.id != zone.id }
                                        safeZones = updated
                                        ZoneStorage.saveZones(context, selectedChild.code, updated)
                                    }) {
                                        Text("Usuń", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }
                }

                if (showHistory && selectedChild != null) {
                    Card(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = if (childLocations.isNotEmpty()) 80.dp else 8.dp)
                            .padding(horizontal = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
                    ) {
                        if (isHistoryLoading) {
                            Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Text("Ładowanie historii...", style = MaterialTheme.typography.bodySmall)
                            }
                        } else {
                            Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Historia:", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(end = 4.dp))
                                hourOptions.forEach { h ->
                                    FilterChip(selected = selectedHours == h, onClick = { selectedHours = h }, label = { Text("${h}h") })
                                }
                                if (historyPoints.isNotEmpty()) {
                                    Text("${historyPoints.size} pkt", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(start = 4.dp))
                                }
                            }
                        }
                    }
                }

                if (childLocations.isNotEmpty()) {
                    Column(modifier = Modifier.align(Alignment.BottomCenter).padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        childLocations.forEach { childLoc ->
                            val childZones = if (selectedChild != null) ZoneStorage.loadZones(context, childLoc.child.code) else emptyList()
                            val zoneStatuses = childZones.map { zone ->
                                val dist = distanceMeters(childLoc.location.latitude, childLoc.location.longitude, zone.latitude, zone.longitude)
                                Triple(zone.name, dist, dist <= zone.radiusMeters)
                            }
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                        Text(text = childLoc.child.name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                                        childLoc.location.battery_level?.let { battery ->
                                            val batteryColor = when {
                                                battery >= 50 -> MaterialTheme.colorScheme.primary
                                                battery >= 20 -> MaterialTheme.colorScheme.tertiary
                                                else -> MaterialTheme.colorScheme.error
                                            }
                                            Text(text = "🔋 $battery%", style = MaterialTheme.typography.bodyMedium, color = batteryColor)
                                        } ?: Text(text = "Brak danych baterii", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                                    }
                                    zoneStatuses.forEach { (name, dist, inZone) ->
                                        Text(
                                            text = "${if (inZone) "✅" else "⚠️"} $name: ${dist.toInt()} m",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (inZone) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}