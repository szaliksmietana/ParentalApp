package com.example.parentalapp

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.parentalapp.ui.DashboardScreen
import com.example.parentalapp.ui.LoginScreen
import com.example.parentalapp.ui.MapScreen

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
                        onNavigateToChat = {
                            Toast.makeText(this, "Tu zrobimy Czat", Toast.LENGTH_SHORT).show()
                        },
                        onNavigateToAddChild = {
                            Toast.makeText(this, "Tu zrobimy dodawanie", Toast.LENGTH_SHORT).show()
                        },
                        onNavigateToSettings = {
                            Toast.makeText(this, "Tu zrobimy ustawienia", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
                AppScreen.Map -> {
                    MapScreen(
                        onNavigateBack = {
                            // Po kliknięciu strzałki wstecz, zmieniamy ekran na Dashboard
                            currentScreen = AppScreen.Dashboard
                        }
                    )
                }
                else -> {
                    // Pozostałe ekrany uzupełnimy później
                }
            }
        }
    }
}