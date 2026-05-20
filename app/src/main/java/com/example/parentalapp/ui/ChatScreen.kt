package com.example.parentalapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.parentalapp.ChildData
import com.example.parentalapp.network.MessageResponse
import com.example.parentalapp.network.RetrofitInstance
import com.example.parentalapp.network.SendMessageRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    childrenList: List<ChildData>,
    initialChild: ChildData?,
    guardianDeviceId: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val currentSettings = SettingsManager.settings  // Reaktywny odczyt flagi powiadomień

    var currentMessage by remember { mutableStateOf("") }
    var messages by remember { mutableStateOf<List<MessageResponse>>(emptyList()) }
    var expanded by remember { mutableStateOf(false) }
    var selectedChild by remember { mutableStateOf(initialChild ?: childrenList.firstOrNull()) }
    var isSending by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val seenMessageIds = remember { mutableSetOf<String>() }

    LaunchedEffect(selectedChild) {
        while (isActive) {
            selectedChild?.let { child ->
                try {
                    val dashboard = RetrofitInstance.api.getChildDashboard(child.code)
                    val newMessages = dashboard.recent_messages.sortedBy { it.sent_at }

                    val freshMessages = newMessages.filter { msg ->
                        msg.sender_device_id != guardianDeviceId && msg.id !in seenMessageIds
                    }

                    // Powiadomienia tylko jeśli flaga włączona w ustawieniach
                    if (seenMessageIds.isNotEmpty() && currentSettings.chatNotificationsEnabled) {
                        freshMessages.forEach { msg ->
                            showMessageNotification(context, child.name, msg.content)
                        }
                    }

                    newMessages.forEach { seenMessageIds.add(it.id) }

                    newMessages.filter {
                        it.receiver_device_id == guardianDeviceId && it.read_at == null
                    }.forEach { msg ->
                        try {
                            RetrofitInstance.api.markAsRead(msg.id, guardianDeviceId)
                        } catch (e: Exception) { }
                    }

                    messages = newMessages
                } catch (e: Exception) { }
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
                title = {
                    Box {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { if (childrenList.isNotEmpty()) expanded = true }
                        ) {
                            Text(selectedChild?.name ?: "Wybierz dziecko")
                            if (childrenList.size > 1) {
                                Icon(Icons.Filled.ArrowDropDown, contentDescription = "Rozwiń")
                            }
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            childrenList.forEach { child ->
                                DropdownMenuItem(
                                    text = { Text(child.name) },
                                    onClick = {
                                        selectedChild = child
                                        messages = emptyList()
                                        seenMessageIds.clear()
                                        expanded = false
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
                    enabled = selectedChild != null && !isSending
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        val child = selectedChild ?: return@IconButton
                        val content = currentMessage.trim()
                        if (content.isBlank() || isSending) return@IconButton
                        isSending = true
                        val optimisticMsg = currentMessage
                        currentMessage = ""
                        scope.launch {
                            try {
                                val sent = RetrofitInstance.api.sendMessage(
                                    SendMessageRequest(
                                        sender_device_id = guardianDeviceId,
                                        receiver_device_id = child.code,
                                        content = optimisticMsg
                                    )
                                )
                                messages = (messages + sent).sortedBy { it.sent_at }
                                seenMessageIds.add(sent.id)
                            } catch (e: Exception) {
                                currentMessage = optimisticMsg
                            } finally {
                                isSending = false
                            }
                        }
                    },
                    modifier = Modifier.background(
                        if (selectedChild != null && !isSending) MaterialTheme.colorScheme.primary
                        else Color.Gray,
                        RoundedCornerShape(50)
                    ),
                    enabled = selectedChild != null && !isSending
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
        if (selectedChild == null) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text("Dodaj i wybierz dziecko, aby rozpocząć czat.")
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(messages) { message ->
                    val isFromParent = message.sender_device_id == guardianDeviceId
                    ChatBubble(
                        content = message.content,
                        isFromParent = isFromParent,
                        isRead = message.read_at != null,
                        time = message.sent_at.take(16).replace("T", " ")
                    )
                }
            }
        }
    }
}

@Composable
fun ChatBubble(
    content: String,
    isFromParent: Boolean,
    isRead: Boolean,
    time: String
) {
    val alignment = if (isFromParent) Alignment.CenterEnd else Alignment.CenterStart
    val backgroundColor = if (isFromParent) MaterialTheme.colorScheme.primary else Color.LightGray
    val textColor = if (isFromParent) Color.White else Color.Black

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = alignment) {
        Column(horizontalAlignment = if (isFromParent) Alignment.End else Alignment.Start) {
            Text(
                text = content,
                color = textColor,
                modifier = Modifier.background(backgroundColor, RoundedCornerShape(16.dp)).padding(12.dp)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(text = time, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                if (isFromParent) {
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