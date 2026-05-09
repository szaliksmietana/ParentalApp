package com.example.parentalapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Tymczasowy model danych wiadomości
data class ChatMessage(val text: String, val isFromParent: Boolean)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(onNavigateBack: () -> Unit) {
    // Stan dla wpisywanej wiadomości
    var currentMessage by remember { mutableStateOf("") }

    // Stan dla listy wiadomości (testowe)
    var messages by remember {
        mutableStateOf(listOf(
            ChatMessage("Cześć, wszystko w porządku?", true), // Od rodzica (po lewej, szare)
            ChatMessage("Tak, wracam już do domu!", false)    // Od dziecka (po prawej, kolorowe)
        ))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Czat z rodzicem") }, // Zmieniony tytuł
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Wróć")
                    }
                }
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = currentMessage,
                    onValueChange = { currentMessage = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Napisz wiadomość...") },
                    shape = RoundedCornerShape(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (currentMessage.isNotBlank()) {
                            // Dziecko wysyła wiadomość, więc isFromParent = false
                            messages = messages + ChatMessage(currentMessage, false)
                            currentMessage = ""
                        }
                    },
                    modifier = Modifier.background(MaterialTheme.colorScheme.primary, RoundedCornerShape(50))
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Wyślij", tint = Color.White)
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            items(messages) { message ->
                ChatBubble(message = message)
            }
        }
    }
}

// Komponent pojedynczego "dymka" z wiadomością
@Composable
fun ChatBubble(message: ChatMessage) {
    // Odwrócona logika: jeśli to rodzic (true), to po lewej; jeśli dziecko (false), to po prawej
    val alignment = if (message.isFromParent) Alignment.CenterStart else Alignment.CenterEnd
    val backgroundColor = if (message.isFromParent) Color.LightGray else MaterialTheme.colorScheme.primary
    val textColor = if (message.isFromParent) Color.Black else Color.White

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