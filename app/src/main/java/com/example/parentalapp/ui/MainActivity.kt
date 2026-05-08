package com.example.parentalapp

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
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

enum class AppScreen {
    Login, Register, Dashboard, Map, Chat, AddChild, Settings
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            var currentScreen by remember { mutableStateOf(AppScreen.Login) }
            val context = LocalContext.current

            // Mechanizm do obsługi uprawnień powiadomień (Android 13+)
            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                if (isGranted) {
                    // Uprawnienia przyznane - rodzic będzie otrzymywać alerty
                }
            }

            when (currentScreen) {
                AppScreen.Login -> {
                    LoginScreen(
                        onLoginClick = { email, password ->
                            currentScreen = AppScreen.Dashboard
                        },
                        onRegisterClick = {
                            currentScreen = AppScreen.Register
                        }
                    )
                }
                AppScreen.Register -> {
                    RegisterScreen(
                        onRegisterClick = { email, password ->
                            currentScreen = AppScreen.Dashboard
                        },
                        onNavigateBack = {
                            currentScreen = AppScreen.Login
                        }
                    )
                }
                AppScreen.Dashboard -> {
                    // Uruchamia się raz po wejściu do Dashboardu
                    LaunchedEffect(Unit) {
                        createNotificationChannel(context)

                        // Prośba o uprawnienia dla Androida 13 (TIRAMISU) i nowszych
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }

                    DashboardScreen(
                        onNavigateToMap = { currentScreen = AppScreen.Map },
                        onNavigateToChat = { currentScreen = AppScreen.Chat },
                        onNavigateToAddChild = { currentScreen = AppScreen.AddChild },
                        onNavigateToSettings = { currentScreen = AppScreen.Settings },
                        onLogoutClick = {
                            currentScreen = AppScreen.Login
                        }
                    )
                }
                AppScreen.Map -> {
                    MapScreen(
                        onNavigateBack = { currentScreen = AppScreen.Dashboard }
                    )
                }
                AppScreen.Chat -> {
                    ChatScreen(
                        onNavigateBack = { currentScreen = AppScreen.Dashboard }
                    )
                }
                AppScreen.AddChild -> {
                    AddChildScreen(
                        onNavigateBack = { currentScreen = AppScreen.Dashboard }
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

    // Funkcja tworząca kanał powiadomień dla geofencingu
    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "geofence_alerts"
            val channelName = "Alerty lokalizacji"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, channelName, importance).apply {
                description = "Powiadomienia o opuszczeniu bezpiecznej strefy przez dziecko"
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}