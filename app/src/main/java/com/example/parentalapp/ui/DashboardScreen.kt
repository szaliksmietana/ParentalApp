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
                                        .background(Color.Green, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = child.name, style = MaterialTheme.typography.titleLarge)
                                    Text(text = "Status: Bezpieczny (w strefie)", style = MaterialTheme.typography.bodySmall)
                                }

                                // Poziom baterii po prawej stronie
                                child.batteryLevel?.let { battery ->
                                    val batteryColor = when {
                                        battery >= 50 -> Color(0xFF4CAF50) // zielony
                                        battery >= 20 -> Color(0xFFFF9800) // pomarańczowy
                                        else -> Color(0xFFF44336)          // czerwony
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

                            Spacer(modifier = Modifier.height(16.dp))

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
                                Spacer(modifier = Modifier.height(16.dp))
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