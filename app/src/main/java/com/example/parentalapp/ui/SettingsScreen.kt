package com.example.parentalapp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onLogoutClick: () -> Unit // Dodajemy akcję wylogowania
) {
    // Stany przechowujące aktualne ustawienia (później można je zapisać w pamięci telefonu)
    var geofenceNotifications by remember { mutableStateOf(true) }
    var chatNotifications by remember { mutableStateOf(true) }
    var locationUpdateFreq by remember { mutableFloatStateOf(15f) } // Wartość w minutach

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ustawienia") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Wróć")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Sekcja Powiadomień
            Text("Powiadomienia", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Wyjście dziecka ze strefy")
                Switch(checked = geofenceNotifications, onCheckedChange = { geofenceNotifications = it })
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Nowe wiadomości na czacie")
                Switch(checked = chatNotifications, onCheckedChange = { chatNotifications = it })
            }

            Divider(modifier = Modifier.padding(vertical = 24.dp))

            // Sekcja Oszczędzania Baterii / GPS
            Text("Lokalizacja i Bateria", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(16.dp))

            Text("Częstotliwość odświeżania mapy: ${locationUpdateFreq.toInt()} min")
            Slider(
                value = locationUpdateFreq,
                onValueChange = { locationUpdateFreq = it },
                valueRange = 1f..60f, // Od 1 do 60 minut
                steps = 59
            )

            // Wypycha przycisk wylogowania na sam dół ekranu
            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = onLogoutClick,
                modifier = Modifier.fillMaxWidth(),
                // Ustawiamy kolor przycisku na czerwony (error), aby sugerował akcję destrukcyjną
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Wyloguj się")
            }
        }
    }
}