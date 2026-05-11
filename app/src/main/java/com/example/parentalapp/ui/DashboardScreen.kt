package com.example.parentalapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.parentalapp.ChildData

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    childrenList: List<ChildData>,
    onNavigateToMap: () -> Unit,
    onNavigateToChat: () -> Unit,
    onNavigateToAddChild: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onLogoutClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Panel Rodzica") },
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
        // Zmieniliśmy Column na LazyColumn dla CAŁEGO ekranu, co włącza przewijanie!
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp), // Automatyczne odstępy między elementami
            contentPadding = PaddingValues(vertical = 16.dp) // Marginesy góra/dół
        ) {
            // Przyciski ujęte w pojedyncze "item"
            item {
                Button(onClick = onNavigateToMap, modifier = Modifier.fillMaxWidth().height(60.dp)) {
                    Text("Mapa (Lokalizacja)")
                }
            }
            item {
                Button(onClick = onNavigateToChat, modifier = Modifier.fillMaxWidth().height(60.dp)) {
                    Text("Czat")
                }
            }
            item {
                Button(onClick = onNavigateToAddChild, modifier = Modifier.fillMaxWidth().height(60.dp)) {
                    Text("Dodaj dziecko")
                }
            }

            // Nagłówek sekcji listy
            item {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Twoje dzieci:",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            // Sekcja renderowania dodanych dzieci (lub komunikatu o braku)
            if (childrenList.isEmpty()) {
                item {
                    Text(
                        text = "Brak dodanych dzieci. Dodaj dziecko, aby rozpocząć śledzenie.",
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            } else {
                items(childrenList) { child ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Person, contentDescription = "Dziecko")
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(text = child.name, style = MaterialTheme.typography.titleMedium)
                            Text(text = "Kod: ${child.code}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}