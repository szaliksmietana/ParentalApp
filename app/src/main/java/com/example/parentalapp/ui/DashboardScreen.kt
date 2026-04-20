package com.example.parentalapp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToMap: () -> Unit,
    onNavigateToChat: () -> Unit,
    onNavigateToAddChild: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    // Scaffold to specjalny układ w Compose, który ułatwia dodawanie
    // górnego paska (TopAppBar), dolnego menu czy wysuwanych szuflad.
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Panel Rodzica") },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        // Gotowa ikona zębatki z biblioteki Material Design
                        Icon(Icons.Filled.Settings, contentDescription = "Ustawienia")
                    }
                }
            )
        }
    ) { paddingValues ->
        // Właściwa zawartość ekranu (przyciski)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues) // Ważne: robi margines na górny pasek
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Button(
                onClick = onNavigateToMap,
                modifier = Modifier.fillMaxWidth().height(60.dp)
            ) {
                Text("Mapa (Lokalizacja)")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onNavigateToChat,
                modifier = Modifier.fillMaxWidth().height(60.dp)
            ) {
                Text("Czat")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onNavigateToAddChild,
                modifier = Modifier.fillMaxWidth().height(60.dp)
            ) {
                Text("Dodaj dziecko")
            }
        }
    }
}