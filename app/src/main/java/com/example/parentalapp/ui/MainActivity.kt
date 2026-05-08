package com.example.parentalapp

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.parentalapp.ui.AddChildScreen
import com.example.parentalapp.ui.ChatScreen
import com.example.parentalapp.ui.DashboardScreen
import com.example.parentalapp.ui.LoginScreen
import com.example.parentalapp.ui.MapScreen
import com.example.parentalapp.ui.SettingsScreen

// Lista dostępnych ekranów w naszej aplikacji
enum class AppScreen {
    Login, Dashboard, Map, Chat, AddChild, Settings
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            var currentScreen by remember { mutableStateOf(AppScreen.Login) }

            when (currentScreen) {
                AppScreen.Login -> {
                    LoginScreen(
                        onLoginClick = { email, password ->
                            currentScreen = AppScreen.Dashboard
                        }
                    )
                }
                AppScreen.Dashboard -> {
                    DashboardScreen(
                        onNavigateToMap = { currentScreen = AppScreen.Map },
                        onNavigateToChat = { currentScreen = AppScreen.Chat },
                        onNavigateToAddChild = { currentScreen = AppScreen.AddChild },
                        onNavigateToSettings = { currentScreen = AppScreen.Settings },
                        onLogoutClick = {
                            // Obsługa wylogowania z poziomu ekranu głównego
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
}