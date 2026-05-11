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

// Prosty model przechowujący dane sparowanego dziecka
data class ChildData(val name: String, val code: String)

enum class AppScreen {
    Login, Register, Dashboard, Map, Chat, AddChild, Settings
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            var currentScreen by remember { mutableStateOf(AppScreen.Login) }
            // Stan przechowujący listę dodanych dzieci
            var childrenList by remember { mutableStateOf(listOf<ChildData>()) }
            val context = LocalContext.current

            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                // Obsługa zgody na powiadomienia
            }

            when (currentScreen) {
                AppScreen.Login -> {
                    LoginScreen(
                        onLoginClick = { email, password -> currentScreen = AppScreen.Dashboard },
                        onRegisterClick = { currentScreen = AppScreen.Register }
                    )
                }
                AppScreen.Register -> {
                    RegisterScreen(
                        onRegisterClick = { email, password -> currentScreen = AppScreen.Dashboard },
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
                        childrenList = childrenList, // Przekazanie listy
                        onNavigateToMap = { currentScreen = AppScreen.Map },
                        onNavigateToChat = { currentScreen = AppScreen.Chat },
                        onNavigateToAddChild = { currentScreen = AppScreen.AddChild },
                        onNavigateToSettings = { currentScreen = AppScreen.Settings },
                        onLogoutClick = { currentScreen = AppScreen.Login }
                    )
                }
                AppScreen.Map -> {
                    MapScreen(onNavigateBack = { currentScreen = AppScreen.Dashboard })
                }
                AppScreen.Chat -> {
                    ChatScreen(
                        childrenList = childrenList, // Przekazanie listy
                        onNavigateBack = { currentScreen = AppScreen.Dashboard }
                    )
                }
                AppScreen.AddChild -> {
                    AddChildScreen(
                        onNavigateBack = { currentScreen = AppScreen.Dashboard },
                        onChildAdded = { name, code ->
                            // Dodajemy dziecko, pokazujemy Toast i wracamy
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
            val channel = NotificationChannel(channelId, "Alerty lokalizacji", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Powiadomienia o opuszczeniu bezpiecznej strefy przez dziecko"
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}