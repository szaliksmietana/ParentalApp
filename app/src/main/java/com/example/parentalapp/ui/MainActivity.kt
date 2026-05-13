package com.example.parentalapp

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.example.parentalapp.ui.AddChildScreen
import com.example.parentalapp.ui.ChatScreen
import com.example.parentalapp.ui.DashboardScreen
import com.example.parentalapp.ui.LoginScreen
import com.example.parentalapp.ui.MapScreen
import com.example.parentalapp.ui.RegisterScreen
import com.example.parentalapp.ui.SettingsScreen

data class ChildData(val name: String, val code: String)

enum class AppScreen {
    Login, Register, Dashboard, Map, Chat, AddChild, Settings
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            var currentScreen by remember { mutableStateOf(AppScreen.Login) }
            var childrenList by remember { mutableStateOf(listOf<ChildData>()) }
            // Stan przechowujący dziecko, które właśnie kliknęliśmy (null oznacza "wszystkie dzieci")
            var selectedChild by remember { mutableStateOf<ChildData?>(null) }

            val context = LocalContext.current

            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { _ -> }

            when (currentScreen) {
                AppScreen.Login -> {
                    LoginScreen(
                        onLoginClick = { _, _ -> currentScreen = AppScreen.Dashboard },
                        onRegisterClick = { currentScreen = AppScreen.Register }
                    )
                }
                AppScreen.Register -> {
                    RegisterScreen(
                        onRegisterClick = { _, _ -> currentScreen = AppScreen.Dashboard },
                        onNavigateBack = { currentScreen = AppScreen.Login }
                    )
                }
                AppScreen.Dashboard -> {
                    LaunchedEffect(Unit) {
                        createNotificationChannel(context)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }

                    DashboardScreen(
                        childrenList = childrenList,
                        onNavigateToMap = { child ->
                            selectedChild = child
                            currentScreen = AppScreen.Map
                        },
                        onNavigateToAllMap = { // NOWE: Przycisk z górnego paska
                            selectedChild = null // null oznacza, że chcemy zobaczyć wszystkich
                            currentScreen = AppScreen.Map
                        },
                        onNavigateToChat = { child ->
                            selectedChild = child
                            currentScreen = AppScreen.Chat
                        },
                        onNavigateToAddChild = { currentScreen = AppScreen.AddChild },
                        onNavigateToSettings = { currentScreen = AppScreen.Settings },
                        onLogoutClick = { currentScreen = AppScreen.Login }
                    )
                }
                AppScreen.Map -> {
                    // Przekazujemy listę i ewentualnie wybrane dziecko
                    MapScreen(
                        childrenList = childrenList,
                        selectedChild = selectedChild,
                        onNavigateBack = { currentScreen = AppScreen.Dashboard }
                    )
                }
                AppScreen.Chat -> {
                    ChatScreen(
                        childrenList = childrenList,
                        initialChild = selectedChild,
                        onNavigateBack = { currentScreen = AppScreen.Dashboard }
                    )
                }
                AppScreen.AddChild -> {
                    AddChildScreen(
                        onNavigateBack = { currentScreen = AppScreen.Dashboard },
                        onChildAdded = { name, code ->
                            childrenList = childrenList + ChildData(name, code)
                            Toast.makeText(context, "Dodano $name", Toast.LENGTH_SHORT).show()
                            currentScreen = AppScreen.Dashboard
                        }
                    )
                }
                AppScreen.Settings -> {
                    SettingsScreen(
                        onNavigateBack = { currentScreen = AppScreen.Dashboard },
                        onLogoutClick = { currentScreen = AppScreen.Login }
                    )
                }
            }
        }
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "geofence_alerts"
            val channel = NotificationChannel(channelId, "Alerty lokalizacji", NotificationManager.IMPORTANCE_HIGH)
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}