package com.example.parentalapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.parentalapp.ChildData

data class ChatMessage(val text: String, val isFromParent: Boolean)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    childrenList: List<ChildData>, // Odbieramy listę
    onNavigateBack: () -> Unit
) {
    var currentMessage by remember { mutableStateOf("") }
    var messages by remember { mutableStateOf(listOf<ChatMessage>()) }

    // Stan do rozwijanego menu
    var expanded by remember { mutableStateOf(false) }
    var selectedChild by remember { mutableStateOf(childrenList.firstOrNull()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Box {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { if (childrenList.isNotEmpty()) expanded = true }
                        ) {
                            Text(selectedChild?.name ?: "Wybierz dziecko")
                            if (childrenList.isNotEmpty()) {
                                Icon(Icons.Filled.ArrowDropDown, contentDescription = "Rozwiń")
                            }
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            childrenList.forEach { child ->
                                DropdownMenuItem(
                                    text = { Text(child.name) },
                                    onClick = {
                                        selectedChild = child
                                        expanded = false
                                        messages = emptyList() // Opcjonalnie: czyścimy czat po zmianie dziecka
                                    }
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Wróć")
                    }
                }
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = currentMessage,
                    onValueChange = { currentMessage = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Napisz wiadomość...") },
                    shape = RoundedCornerShape(24.dp),
                    enabled = selectedChild != null // Zablokuj, jeśli brak dziecka
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (currentMessage.isNotBlank() && selectedChild != null) {
                            messages = messages + ChatMessage(currentMessage, true)
                            currentMessage = ""
                        }
                    },
                    modifier = Modifier.background(
                        if (selectedChild != null) MaterialTheme.colorScheme.primary else Color.Gray,
                        RoundedCornerShape(50)
                    ),
                    enabled = selectedChild != null
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Wyślij", tint = Color.White)
                }
            }
        }
    ) { paddingValues ->
        if (selectedChild == null) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text("Dodaj i wybierz dziecko, aby rozpocząć czat.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(messages) { message ->
                    ChatBubble(message = message)
                }
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    val alignment = if (message.isFromParent) Alignment.CenterEnd else Alignment.CenterStart
    val backgroundColor = if (message.isFromParent) MaterialTheme.colorScheme.primary else Color.LightGray
    val textColor = if (message.isFromParent) Color.White else Color.Black

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = alignment
    ) {
        Text(
            text = message.text,
            color = textColor,
            modifier = Modifier
                .background(backgroundColor, RoundedCornerShape(16.dp))
                .padding(12.dp)
        )
    }
}