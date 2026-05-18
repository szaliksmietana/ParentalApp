package com.example.parentalapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.parentalapp.ChildData

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    childrenList: List<ChildData>,
    onNavigateToMap: (ChildData) -> Unit,
    onNavigateToAllMap: () -> Unit,
    onNavigateToChat: (ChildData) -> Unit,
    onNavigateToAddChild: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onLogoutClick: () -> Unit
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Panel Rodzica") },
                actions = {
                    if (childrenList.isNotEmpty()) {
                        IconButton(onClick = onNavigateToAllMap) {
                            Icon(Icons.Filled.LocationOn, contentDescription = "Mapa wszystkich dzieci")
                        }
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Ustawienia")
                    }
                    IconButton(onClick = onLogoutClick) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Wyloguj się")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToAddChild,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Text("+ Dodaj dziecko")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
        ) {
            item {
                Text(text = "Twoje dzieci:", style = MaterialTheme.typography.titleMedium)
            }

            if (childrenList.isEmpty()) {
                item {
                    Text(text = "Brak dodanych dzieci.", color = MaterialTheme.colorScheme.secondary)
                }
            } else {
                items(childrenList) { child ->
                    // Pobierz strefy i oblicz status dla tego dziecka
                    val zones = ZoneStorage.loadZones(context, child.code)
                    val zoneStatuses = zones.mapNotNull { zone ->
                        val lat = child.latitude ?: return@mapNotNull null
                        val lon = child.longitude ?: return@mapNotNull null
                        val dist = distanceMeters(lat, lon, zone.latitude, zone.longitude)
                        Triple(zone.name, dist, dist <= zone.radiusMeters)
                    }

                    // Bezpieczny jeśli jest w przynajmniej jednej strefie
                    val inAnyZone = zoneStatuses.any { it.third }
                    val statusColor = when {
                        zones.isEmpty() -> Color(0xFF9E9E9E)
                        inAnyZone -> Color(0xFF4CAF50)
                        else -> Color(0xFFF44336)
                    }
                    val statusText = when {
                        zones.isEmpty() -> "Brak stref (dodaj na mapie)"
                        inAnyZone -> "Bezpieczny — " + zoneStatuses.first { it.third }.first
                        else -> "⚠️ Poza wszystkimi strefami!"
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .background(statusColor, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = child.name, style = MaterialTheme.typography.titleLarge)
                                    Text(
                                        text = statusText,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (inAnyZone || zones.isEmpty()) MaterialTheme.colorScheme.secondary
                                        else MaterialTheme.colorScheme.error
                                    )
                                }

                                // Bateria po prawej
                                child.batteryLevel?.let { battery ->
                                    val batteryColor = when {
                                        battery >= 50 -> Color(0xFF4CAF50)
                                        battery >= 20 -> Color(0xFFFF9800)
                                        else -> Color(0xFFF44336)
                                    }
                                    Text(
                                        text = "🔋 $battery%",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = batteryColor
                                    )
                                } ?: Text(
                                    text = "🔋 --",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }

                            // Szczegółowy status stref
                            if (zoneStatuses.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                zoneStatuses.forEach { (name, dist, inZone) ->
                                    Text(
                                        text = "${if (inZone) "✅" else "⚠️"} $name: ${dist.toInt()} m",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (inZone) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.error
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Pasek baterii
                            child.batteryLevel?.let { battery ->
                                val batteryColor = when {
                                    battery >= 50 -> Color(0xFF4CAF50)
                                    battery >= 20 -> Color(0xFFFF9800)
                                    else -> Color(0xFFF44336)
                                }
                                Text(
                                    text = "Bateria: $battery%",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { battery / 100f },
                                    modifier = Modifier.fillMaxWidth().height(8.dp),
                                    color = batteryColor
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            Row(horizontalArrangement = Arrangement.SpaceBetween) {
                                OutlinedButton(
                                    onClick = { onNavigateToChat(child) },
                                    modifier = Modifier.weight(1f)
                                ) { Text("Napisz") }

                                Spacer(modifier = Modifier.width(8.dp))

                                Button(
                                    onClick = { onNavigateToMap(child) },
                                    modifier = Modifier.weight(1f)
                                ) { Text("Lokalizuj") }
                            }
                        }
                    }
                }
            }
        }
    }
}