package com.example.parentalapp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onLogoutClick: () -> Unit
) {
    val context = LocalContext.current
    var settings by remember { mutableStateOf(SettingsManager.settings) }

    fun update(new: AppSettings) {
        settings = new
        SettingsManager.save(context, new)
    }

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
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // ── 1. POLLING ────────────────────────────────────────────────
            SectionHeader("Częstotliwość odświeżania danych")
            Text(
                "Wpływa na zużycie baterii i transferu danych.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.height(8.dp))

            PollingInterval.entries.forEach { interval ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = settings.pollingInterval == interval,
                        onClick = { update(settings.copy(pollingInterval = interval)) }
                    )
                    Text(interval.label, style = MaterialTheme.typography.bodyMedium)
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            // ── 2. POWIADOMIENIA ──────────────────────────────────────────
            SectionHeader("Zarządzanie alertami")

            SettingSwitch(
                label = "Powiadomienia o wiadomościach (Czat)",
                checked = settings.chatNotificationsEnabled,
                onCheckedChange = { update(settings.copy(chatNotificationsEnabled = it)) }
            )

            Spacer(modifier = Modifier.height(8.dp))

            SettingSwitch(
                label = "Alerty Geofence (Wyjście ze strefy)",
                checked = settings.geofenceNotificationsEnabled,
                onCheckedChange = { update(settings.copy(geofenceNotificationsEnabled = it)) }
            )

            if (settings.geofenceNotificationsEnabled) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Tolerancja błędu GPS: +${settings.gpsToleranceMeters} m",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "Ignoruje drobne wyskoki lokalizacji (przydatne w budynkach)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
                Slider(
                    value = settings.gpsToleranceMeters.toFloat(),
                    onValueChange = { update(settings.copy(gpsToleranceMeters = it.toInt())) },
                    valueRange = 0f..200f,
                    steps = 7
                )
            }

            // SOS zawsze włączony — informacja dla rodzica
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "🚨 Alarm SOS jest zawsze aktywny i nie może być wyłączony.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            // ── 3. STYL MAPY ──────────────────────────────────────────────
            SectionHeader("Domyślny styl mapy")

            MapStyle.entries.forEach { style ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = settings.mapStyle == style,
                        onClick = { update(settings.copy(mapStyle = style)) }
                    )
                    Text(style.label, style = MaterialTheme.typography.bodyMedium)
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            // ── 4. MOTYW ──────────────────────────────────────────────────
            SectionHeader("Motyw aplikacji")

            AppTheme.entries.forEach { theme ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = settings.appTheme == theme,
                        onClick = { update(settings.copy(appTheme = theme)) }
                    )
                    Text(theme.label, style = MaterialTheme.typography.bodyMedium)
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            Button(
                onClick = onLogoutClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Wyloguj się")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 4.dp)
    )
}

@Composable
private fun SettingSwitch(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}