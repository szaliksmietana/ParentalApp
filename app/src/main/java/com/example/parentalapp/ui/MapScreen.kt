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
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
// Dodaliśmy parametr funkcji, który mówi "co zrobić po kliknięciu wstecz"
fun MapScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current

    // Inicjalizacja OSMdroida
    remember {
        Configuration.getInstance().load(context, PreferenceManager.getDefaultSharedPreferences(context))
        Configuration.getInstance().userAgentValue = context.packageName
    }

    // Scaffold pozwala na dodanie górnego paska (TopAppBar) w bardzo prosty sposób
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lokalizacja Dziecka") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        // Ikonka strzałki w lewo (wstecz)
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Wróć")
                    }
                }
            )
        }
    ) { paddingValues ->
        // Musimy przekazać paddingValues, żeby mapa nie chowała się pod paskiem!
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            factory = { ctx ->
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)

                    // Przykładowa lokalizacja dziecka
                    val childLocation = GeoPoint(52.2297, 21.0122)

                    // Ustawienie kamery
                    controller.setZoom(15.0)
                    controller.setCenter(childLocation)

                    // Tworzenie markera na mapie
                    val marker = Marker(this)
                    marker.position = childLocation
                    marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    marker.title = "Twoje Dziecko"
                    marker.snippet = "Ostatnia aktualizacja: przed chwilą"

                    // Dodanie markera do warstw mapy
                    overlays.add(marker)
                }
            },
            update = { view ->
                // Tu będziemy odświeżać mapę
            }
        )
    }
}