package com.example.parentalapp

import android.Manifest
import android.content.Intent
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
import com.example.parentalapp.ui.RegisterScreen // Nowy import!
import com.example.parentalapp.ui.SettingsScreen

// Dodany stan Register
enum class AppScreen {
    Login, Register, Dashboard, Chat, Pairing, Settings
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            var currentScreen by remember { mutableStateOf(AppScreen.Login) }
            val context = LocalContext.current

            // Mechanizm do obsługi uprawnień i odpalania serwisu GPS
            val permissionsLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestMultiplePermissions()
            ) { permissions ->
                val locationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true

                if (locationGranted) {
                    val serviceIntent = Intent(context, LocationTrackingService::class.java)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
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
                            // Tymczasowo po rejestracji od razu wpuszczamy do Panelu
                            currentScreen = AppScreen.Dashboard
                        },
                        onNavigateBack = {
                            currentScreen = AppScreen.Login
                        }
                    )
                }
                AppScreen.Dashboard -> {
                    LaunchedEffect(Unit) {
                        val permissionsToRequest = mutableListOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        permissionsLauncher.launch(permissionsToRequest.toTypedArray())
                    }

                    DashboardScreen(
                        onNavigateToPairing = { currentScreen = AppScreen.Pairing },
                        onNavigateToChat = { currentScreen = AppScreen.Chat },
                        onNavigateToSettings = { currentScreen = AppScreen.Settings },
                        onLogoutClick = {
                            val serviceIntent = Intent(context, LocationTrackingService::class.java)
                            context.stopService(serviceIntent)
                            currentScreen = AppScreen.Login
                        }
                    )
                }
                AppScreen.Chat -> {
                    ChatScreen(
                        onNavigateBack = { currentScreen = AppScreen.Dashboard }
                    )
                }
                AppScreen.Pairing -> {
                    AddChildScreen(
                        onNavigateBack = { currentScreen = AppScreen.Dashboard }
                    )
                }
                AppScreen.Settings -> {
                    SettingsScreen(
                        onNavigateBack = { currentScreen = AppScreen.Dashboard },
                        onLogoutClick = {
                            val serviceIntent = Intent(context, LocationTrackingService::class.java)
                            context.stopService(serviceIntent)
                            currentScreen = AppScreen.Login
                        }
                    )
                }
            }
        }
    }
}