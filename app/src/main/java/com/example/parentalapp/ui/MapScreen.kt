package com.example.parentalapp.ui

import android.preference.PreferenceManager
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.example.parentalapp.ChildData
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    childrenList: List<ChildData>, // NOWE
    selectedChild: ChildData?, // ZMIANA
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current

    remember {
        Configuration.getInstance().load(context, PreferenceManager.getDefaultSharedPreferences(context))
        Configuration.getInstance().userAgentValue = context.packageName
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
        AndroidView(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            factory = { ctx ->
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)

                    // Jeśli wybrano konkretne dziecko, pokazujemy tylko je. W przeciwnym razie wszystkie.
                    val toDisplay = if (selectedChild != null) listOf(selectedChild) else childrenList

                    toDisplay.forEachIndexed { index, currentChild ->
                        // Lekkie przesunięcie, żeby markery nie nakładały się w tym samym miejscu podczas testów
                        val childLocation = GeoPoint(52.2297 + (index * 0.005), 21.0122 + (index * 0.005))

                        // Centrujemy kamerę na pierwszym znaczniku
                        if (index == 0) {
                            controller.setZoom(14.0)
                            controller.setCenter(childLocation)
                        }

                        val marker = Marker(this)
                        marker.position = childLocation
                        marker.title = currentChild.name
                        marker.snippet = "Ostatnia aktualizacja: przed chwilą"
                        overlays.add(marker)
                    }
                }
            }
        )
    }
}