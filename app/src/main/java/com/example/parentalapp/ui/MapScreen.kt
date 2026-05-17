package com.example.parentalapp.ui

import android.preference.PreferenceManager
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.parentalapp.ChildData
import com.example.parentalapp.network.LocationResponse
import com.example.parentalapp.network.RetrofitInstance
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

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

    var childLocations by remember { mutableStateOf<List<ChildLocation>>(emptyList()) }
    var historyPoints by remember { mutableStateOf<List<LocationResponse>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isHistoryLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showHistory by remember { mutableStateOf(false) }
    var selectedHours by remember { mutableIntStateOf(24) }

    val hourOptions = listOf(1, 6, 24, 48)

    remember {
        Configuration.getInstance().load(context, PreferenceManager.getDefaultSharedPreferences(context))
        Configuration.getInstance().userAgentValue = context.packageName
    }

    LaunchedEffect(selectedChild) {
        isLoading = true
        errorMessage = null
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
            } catch (_: Exception) {
                emptyList()
            }
            isHistoryLoading = false
        } else {
            historyPoints = emptyList()
        }
    }

    val pathColor = MaterialTheme.colorScheme.primary.toArgb()

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
                                tint = if (showHistory) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (errorMessage != null && childLocations.isEmpty()) {
                Text(
                    text = errorMessage!!,
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        MapView(ctx).apply {
                            setTileSource(TileSourceFactory.MAPNIK)
                            setMultiTouchControls(true)
                        }
                    },
                    update = { mapView ->
                        mapView.overlays.clear()

                        // History polyline
                        if (historyPoints.size >= 2) {
                            val polyline = Polyline(mapView).apply {
                                outlinePaint.color = pathColor
                                outlinePaint.strokeWidth = 7f
                                setPoints(historyPoints.map { GeoPoint(it.latitude, it.longitude) })
                            }
                            mapView.overlays.add(polyline)

                            // Small markers for historical points (skip the last — current location gets its own marker)
                            historyPoints.dropLast(1).forEachIndexed { index, point ->
                                val marker = Marker(mapView)
                                marker.position = GeoPoint(point.latitude, point.longitude)
                                marker.title = "Punkt ${index + 1} z ${historyPoints.size}"
                                marker.snippet = point.recorded_at.take(16).replace("T", " ")
                                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                                mapView.overlays.add(marker)
                            }
                        }

                        // Current location markers
                        childLocations.forEachIndexed { index, childLoc ->
                            val geoPoint = GeoPoint(childLoc.location.latitude, childLoc.location.longitude)
                            if (index == 0) {
                                mapView.controller.setZoom(15.0)
                                mapView.controller.setCenter(geoPoint)
                            }
                            val marker = Marker(mapView)
                            marker.position = geoPoint
                            marker.title = childLoc.child.name
                            marker.snippet = buildString {
                                append("Ostatnia aktualizacja: ${childLoc.location.recorded_at.take(16).replace("T", " ")}")
                                childLoc.location.battery_level?.let { append("\nBateria: $it%") }
                                childLoc.location.accuracy_meters?.let { append("\nDokładność: ${it.toInt()} m") }
                            }
                            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            mapView.overlays.add(marker)
                        }

                        mapView.invalidate()
                    }
                )

                // History hour-selector bar
                if (showHistory && selectedChild != null) {
                    Card(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                        )
                    ) {
                        if (isHistoryLoading) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Text("Ładowanie historii...", style = MaterialTheme.typography.bodySmall)
                            }
                        } else {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    "Historia:",
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(end = 4.dp)
                                )
                                hourOptions.forEach { h ->
                                    FilterChip(
                                        selected = selectedHours == h,
                                        onClick = { selectedHours = h },
                                        label = { Text("${h}h") }
                                    )
                                }
                                if (historyPoints.isNotEmpty()) {
                                    Text(
                                        text = "${historyPoints.size} pkt",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.padding(start = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Battery cards at the bottom
                if (childLocations.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        childLocations.forEach { childLoc ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = childLoc.child.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        modifier = Modifier.weight(1f)
                                    )
                                    childLoc.location.battery_level?.let { battery ->
                                        val batteryColor = when {
                                            battery >= 50 -> MaterialTheme.colorScheme.primary
                                            battery >= 20 -> MaterialTheme.colorScheme.tertiary
                                            else -> MaterialTheme.colorScheme.error
                                        }
                                        Text(
                                            text = "🔋 $battery%",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = batteryColor
                                        )
                                    } ?: Text(
                                        text = "Brak danych baterii",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.secondary
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