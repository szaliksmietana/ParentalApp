package com.example.parentalapp.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddChildScreen(
    onNavigateBack: () -> Unit,
    onChildAdded: (String, String) -> Unit // Zwraca Imię i Kod
) {
    var name by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dodaj dziecko") },
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
            // Nowe pole: Imię dziecka
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Imię dziecka") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Wprowadź 6-cyfrowy kod z urządzenia dziecka",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            BasicTextField(
                value = code,
                onValueChange = {
                    if (it.length <= 6) code = it.filter { char -> char.isDigit() }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                decorationBox = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        repeat(6) { index ->
                            val char = if (index < code.length) code[index].toString() else ""
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(0.8f)
                                    .border(
                                        width = 2.dp,
                                        color = if (index == code.length) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                        shape = RoundedCornerShape(8.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = char, style = MaterialTheme.typography.headlineMedium)
                            }
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { onChildAdded(name, code) },
                enabled = name.isNotBlank() && code.length == 6, // Wymaga imienia i 6 cyfr
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("Dodaj przypisanie")
            }
        }
    }
}