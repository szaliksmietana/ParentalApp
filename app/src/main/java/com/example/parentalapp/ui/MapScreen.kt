package com.example.parentalapp.ui

import android.preference.PreferenceManager
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.parentalapp.ChildData
import com.example.parentalapp.network.LocationResponse
import com.example.parentalapp.network.RetrofitInstance
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

// Przechowuje lokalizację + poziom baterii dla dziecka
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
    val scope = rememberCoroutineScope()

    var childLocations by remember { mutableStateOf<List<ChildLocation>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    remember {
        Configuration.getInstance().load(context, PreferenceManager.getDefaultSharedPreferences(context))
        Configuration.getInstance().userAgentValue = context.packageName
    }

    // Pobierz lokalizacje z API przy wejściu na ekran
    LaunchedEffect(selectedChild) {
        isLoading = true
        errorMessage = null
        val toFetch = if (selectedChild != null) listOf(selectedChild) else childrenList

        val results = mutableListOf<ChildLocation>()
        for (child in toFetch) {
            try {
                val loc = RetrofitInstance.api.getLatestLocation(child.code) // child.code = child_device_id
                results.add(ChildLocation(child, loc))
            } catch (e: Exception) {
                // Brak lokalizacji dla tego dziecka — pomijamy
            }
        }
        childLocations = results
        isLoading = false
        if (results.isEmpty()) {
            errorMessage = "Brak danych lokalizacji"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(selectedChild?.let { "Lokalizacja: ${it.name}" } ?: "Wszystkie dzieci") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Wróć")
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
                                childLoc.location.battery_level?.let {
                                    append("\nBateria: $it%")
                                }
                                childLoc.location.accuracy_meters?.let {
                                    append("\nDokładność: ${it.toInt()} m")
                                }
                            }
                            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            mapView.overlays.add(marker)
                        }

                        mapView.invalidate()
                    }
                )

                // Karty z poziomem baterii na dole ekranu
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