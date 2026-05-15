package com.example.parentalapp.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.parentalapp.network.GeneratePairingCodeRequest
import com.example.parentalapp.network.RetrofitInstance
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddChildScreen(
    childDeviceId: String,
    onNavigateBack: () -> Unit,
    onPaired: () -> Unit
) {
    var pairingCode by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isPaired by remember { mutableStateOf(false) }

    // Pobierz kod parowania z API i co 5 sekund sprawdzaj status
    LaunchedEffect(childDeviceId) {
        // Krok 1: Wygeneruj kod
        try {
            val response = RetrofitInstance.api.generatePairingCode(
                GeneratePairingCodeRequest(device_id = childDeviceId)
            )
            pairingCode = response.code
            isLoading = false
        } catch (e: Exception) {
            errorMessage = "Nie udało się wygenerować kodu: ${e.message}"
            isLoading = false
            return@LaunchedEffect
        }

        // Krok 2: Co 5 sekund sprawdzaj czy rodzic potwierdził parowanie
        while (isActive && !isPaired) {
            delay(5000)
            try {
                val status = RetrofitInstance.api.getPairingStatus(childDeviceId)
                if (status.is_paired) {
                    isPaired = true
                    onPaired()
                }
            } catch (e: Exception) {
                // Ignoruj błędy pollingu — spróbuj ponownie za 5 sekund
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Parowanie z rodzicem") },
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
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text("Generowanie kodu...", color = MaterialTheme.colorScheme.secondary)
            } else if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onNavigateBack) { Text("Wróć") }
            } else {
                Text(
                    text = "Twój kod parowania",
                    style = MaterialTheme.typography.titleLarge
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Przekaż ten kod rodzicowi, aby połączyć konta.",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Kod wygasa po 10 minutach.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )

                Spacer(modifier = Modifier.height(32.dp))


                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    pairingCode?.forEachIndexed { index, char ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(0.8f)
                                .border(
                                    width = 2.dp,
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = RoundedCornerShape(8.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = char.toString(), style = MaterialTheme.typography.headlineMedium)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(48.dp))

                CircularProgressIndicator(modifier = Modifier.size(32.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text("Oczekiwanie na rodzica...", color = MaterialTheme.colorScheme.secondary)
            }
        }
    }
}