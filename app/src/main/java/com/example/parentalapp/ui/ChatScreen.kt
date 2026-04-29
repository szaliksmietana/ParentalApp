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

    // Stan dla listy wiadomości (na start dajemy dwie testowe)
    var messages by remember {
        mutableStateOf(listOf(
            ChatMessage("Cześć, wszystko w porządku?", true),
            ChatMessage("Tak, wracam już do domu!", false)
        ))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Czat z dzieckiem") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        // Automirrored zapewnia poprawne odwrócenie strzałki w językach czytanych od prawej do lewej
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Wróć")
                    }
                }
            )
        },
        bottomBar = {
            // Pasek wpisywania wiadomości na dole ekranu
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
                            // Dodajemy nową wiadomość do listy i czyścimy pole
                            messages = messages + ChatMessage(currentMessage, true)
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
        // Lista wiadomości
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp), // Odstępy między dymkami
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
    // Wyrównanie: prawe dla rodzica, lewe dla dziecka
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