package com.example.parentalapp.ui

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToPairing: () -> Unit,
    onNavigateToChat: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onLogoutClick: () -> Unit
) {
    val context = LocalContext.current
    // Stan kontrolujący wyświetlanie okienka potwierdzenia SOS
    var showSosDialog by remember { mutableStateOf(false) }

    // Okienko dialogowe upewniające się, czy wysłać alarm
    if (showSosDialog) {
        AlertDialog(
            onDismissRequest = { showSosDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Alarm SOS")
                }
            },
            text = { Text("Czy na pewno chcesz wysłać natychmiastowy alarm do rodzica? Używaj tej funkcji tylko w nagłych wypadkach.") },
            confirmButton = {
                Button(
                    onClick = {
                        showSosDialog = false
                        // Tutaj w przyszłości wyślesz zapytanie do FastAPI
                        Toast.makeText(context, "Wysłano alarm do rodzica!", Toast.LENGTH_LONG).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Wyślij SOS")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSosDialog = false }) {
                    Text("Anuluj")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Panel Dziecka") },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Ustawienia")
                    }
                    IconButton(onClick = onLogoutClick) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Wyloguj się")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // KARTA STATUSU
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LocationOn,
                            contentDescription = "Lokalizacja aktywna",
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "Ochrona aktywna",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Twoja lokalizacja jest udostępniana.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            // WIELKI PRZYCISK SOS
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { showSosDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp), // Zwiększona wysokość, żeby był łatwy do trafienia
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error, // Zawsze czerwony
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Icon(Icons.Filled.Warning, contentDescription = "SOS", modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("WYŚLIJ ALARM SOS", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
            }

            // Główne przyciski
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onNavigateToPairing,
                    modifier = Modifier.fillMaxWidth().height(60.dp)
                ) {
                    Text("Parowanie z rodzicem")
                }
            }

            item {
                Button(
                    onClick = onNavigateToChat,
                    modifier = Modifier.fillMaxWidth().height(60.dp)
                ) {
                    Text("Czat z rodzicem")
                }
            }
        }
    }
}