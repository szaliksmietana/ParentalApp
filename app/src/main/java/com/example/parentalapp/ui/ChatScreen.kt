package com.example.parentalapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.parentalapp.network.MessageResponse
import com.example.parentalapp.network.RetrofitInstance
import com.example.parentalapp.network.SendMessageRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    childDeviceId: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val currentSettings = ChildSettingsManager.settings // Reaktywny odczyt flagi powiadomień

    var currentMessage by remember { mutableStateOf("") }
    var messages by remember { mutableStateOf<List<MessageResponse>>(emptyList()) }
    var guardianDeviceId by remember { mutableStateOf<String?>(null) }
    var isSending by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val seenMessageIds = remember { mutableSetOf<String>() }

    LaunchedEffect(childDeviceId) {
        while (isActive) {
            try {
                if (guardianDeviceId == null) {
                    val guardian = RetrofitInstance.api.getMyGuardian(childDeviceId)
                    guardianDeviceId = guardian.guardian_device_id
                }

                val history = RetrofitInstance.api.getMessageHistory(childDeviceId)
                val sorted = history.sortedBy { it.sent_at }

                val freshFromGuardian = sorted.filter { msg ->
                    msg.sender_device_id != childDeviceId && msg.id !in seenMessageIds
                }

                // Powiadomienia tylko jeśli flaga włączona
                if (seenMessageIds.isNotEmpty() && currentSettings.messageNotificationsEnabled) {
                    freshFromGuardian.forEach { msg ->
                        NotificationHelper.showMessageNotification(context, msg.content)
                    }
                }

                sorted.forEach { seenMessageIds.add(it.id) }

                sorted.filter {
                    it.receiver_device_id == childDeviceId && it.read_at == null
                }.forEach { msg ->
                    try {
                        RetrofitInstance.api.markAsRead(msg.id, childDeviceId)
                    } catch (_: Exception) {}
                }

                messages = sorted

            } catch (_: Exception) {
            } finally {
                isLoading = false
            }
            delay(10000)
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Czat z rodzicem") },
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
                    enabled = guardianDeviceId != null && !isSending
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        val guardian = guardianDeviceId ?: return@IconButton
                        val content = currentMessage.trim()
                        if (content.isBlank() || isSending) return@IconButton
                        isSending = true
                        val optimisticContent = currentMessage
                        currentMessage = ""
                        scope.launch {
                            try {
                                val sent = RetrofitInstance.api.sendMessage(
                                    SendMessageRequest(
                                        sender_device_id = childDeviceId,
                                        receiver_device_id = guardian,
                                        content = optimisticContent
                                    )
                                )
                                messages = (messages + sent).sortedBy { it.sent_at }
                                seenMessageIds.add(sent.id)
                            } catch (_: Exception) {
                                currentMessage = optimisticContent
                            } finally {
                                isSending = false
                            }
                        }
                    },
                    modifier = Modifier.background(
                        if (guardianDeviceId != null && !isSending) MaterialTheme.colorScheme.primary
                        else Color.Gray,
                        RoundedCornerShape(50)
                    ),
                    enabled = guardianDeviceId != null && !isSending
                ) {
                    if (isSending) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Wyślij", tint = Color.White)
                    }
                }
            }
        }
    ) { paddingValues ->
        when {
            isLoading -> {
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            guardianDeviceId == null -> {
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                    Text(
                        "Brak parowania z rodzicem.\nSparuj urządzenie najpierw.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
            else -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    items(messages) { message ->
                        val isFromChild = message.sender_device_id == childDeviceId
                        ChildChatBubble(
                            content = message.content,
                            isFromChild = isFromChild,
                            isRead = message.read_at != null,
                            time = message.sent_at.take(16).replace("T", " ")
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChildChatBubble(
    content: String,
    isFromChild: Boolean,
    isRead: Boolean,
    time: String
) {
    val alignment = if (isFromChild) Alignment.CenterEnd else Alignment.CenterStart
    val backgroundColor = if (isFromChild) MaterialTheme.colorScheme.primary else Color.LightGray
    val textColor = if (isFromChild) Color.White else Color.Black

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = alignment) {
        Column(horizontalAlignment = if (isFromChild) Alignment.End else Alignment.Start) {
            Text(
                text = content,
                color = textColor,
                modifier = Modifier.background(backgroundColor, RoundedCornerShape(16.dp)).padding(12.dp)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(text = time, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                if (isFromChild) {
                    Text(
                        text = if (isRead) "✓✓" else "✓",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isRead) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}