package com.example.parentalapp

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
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
import androidx.lifecycle.lifecycleScope
import com.example.parentalapp.network.LoginRequest
import kotlinx.coroutines.launch
import com.example.parentalapp.network.RegisterRequest
import com.example.parentalapp.network.RetrofitInstance
import com.example.parentalapp.network.TokenManager
import com.example.parentalapp.ui.AddChildScreen
import com.example.parentalapp.ui.ChatScreen
import com.example.parentalapp.ui.DashboardScreen
import com.example.parentalapp.ui.LoginScreen
import com.example.parentalapp.ui.MapScreen
import com.example.parentalapp.ui.RegisterScreen
import com.example.parentalapp.ui.SettingsScreen
import retrofit2.HttpException

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
            var selectedChild by remember { mutableStateOf<ChildData?>(null) }

            val context = LocalContext.current

            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { _ -> }

            when (currentScreen) {
                AppScreen.Login -> {
                    LoginScreen(
                        onLoginClick = { email, password ->
                            lifecycleScope.launch {
                                try {
                                    val response = RetrofitInstance.api.login(LoginRequest(email, password))

                                    // ZAPISANIE TOKENA W PAMIĘCI
                                    TokenManager.token = response.access_token

                                    Toast.makeText(context, "Zalogowano pomyślnie!", Toast.LENGTH_SHORT).show()
                                    currentScreen = AppScreen.Dashboard
                                } catch (e: HttpException) {
                                    val errorBody = e.response()?.errorBody()?.string()
                                    Log.e("API_ERROR", "HTTP ${e.code()}: $errorBody")
                                    Toast.makeText(context, "Błąd: Nieprawidłowy email lub hasło.", Toast.LENGTH_LONG).show()
                                } catch (e: Exception) {
                                    Log.e("API_ERROR", "Exception: ${e.message}")
                                    Toast.makeText(context, "Błąd sieci: Brak połączenia z serwerem.", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        onRegisterClick = { currentScreen = AppScreen.Register }
                    )
                }
                AppScreen.Register -> {
                    RegisterScreen(
                        onRegisterClick = { email, password, username ->
                            lifecycleScope.launch {
                                try {
                                    val request = RegisterRequest(
                                        email = email,
                                        password = password,
                                        username = username,
                                        role = "parent" // lub "guardian" jeśli kolega to zmienił
                                    )
                                    val response = RetrofitInstance.api.register(request)
                                    Toast.makeText(context, "Konto ${response.username} utworzone!", Toast.LENGTH_SHORT).show()
                                    currentScreen = AppScreen.Login
                                } catch (e: HttpException) {
                                    val errorBody = e.response()?.errorBody()?.string()
                                    Log.e("API_ERROR", "HTTP ${e.code()}: $errorBody")
                                    Toast.makeText(context, "Błąd rejestracji.", Toast.LENGTH_LONG).show()
                                } catch (e: Exception) {
                                    Log.e("API_ERROR", "Exception: ${e.message}")
                                    Toast.makeText(context, "Błąd połączenia. Sprawdź IP lub status serwera.", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        onNavigateBack = { currentScreen = AppScreen.Login }
                    )
                }
                AppScreen.Dashboard -> {
                    // POBIERANIE DZIECI PO WEJŚCIU NA EKRAN DASHBOARDU
                    LaunchedEffect(currentScreen) {
                        createNotificationChannel(context)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }

                        try {
                            // Strzał do serwera po dzieci
                            val fetchedChildren = RetrofitInstance.api.getChildren()

                            // Zamiana obiektów z serwera na obiekty w Androidzie
                            childrenList = fetchedChildren.map { childResponse ->
                                ChildData(
                                    name = childResponse.username,
                                    code = childResponse.id // Zapisujemy ID z bazy jako "kod" (np. do czatu)
                                )
                            }
                            Log.d("API_SUCCESS", "Pobrano ${childrenList.size} dzieci z bazy.")

                        } catch (e: HttpException) {
                            Log.e("API_ERROR", "HTTP Error przy pobieraniu dzieci: ${e.response()?.errorBody()?.string()}")
                            if (e.code() == 401) {
                                Toast.makeText(context, "Sesja wygasła. Zaloguj się ponownie.", Toast.LENGTH_SHORT).show()
                                TokenManager.token = null
                                currentScreen = AppScreen.Login
                            }
                        } catch (e: Exception) {
                            Log.e("API_ERROR", "Błąd sieci: ${e.message}")
                            Toast.makeText(context, "Nie udało się odświeżyć listy dzieci.", Toast.LENGTH_SHORT).show()
                        }
                    }

                    DashboardScreen(
                        childrenList = childrenList,
                        onNavigateToMap = { child ->
                            selectedChild = child
                            currentScreen = AppScreen.Map
                        },
                        onNavigateToAllMap = {
                            selectedChild = null
                            currentScreen = AppScreen.Map
                        },
                        onNavigateToChat = { child ->
                            selectedChild = child
                            currentScreen = AppScreen.Chat
                        },
                        onNavigateToAddChild = { currentScreen = AppScreen.AddChild },
                        onNavigateToSettings = { currentScreen = AppScreen.Settings },
                        onLogoutClick = {
                            TokenManager.token = null // Wylogowanie
                            currentScreen = AppScreen.Login
                        }
                    )
                }
                AppScreen.Map -> {
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
                            // TODO: To też podepniemy pod API w przyszłości
                            childrenList = childrenList + ChildData(name, code)
                            Toast.makeText(context, "Dodano $name", Toast.LENGTH_SHORT).show()
                            currentScreen = AppScreen.Dashboard
                        }
                    )
                }
                AppScreen.Settings -> {
                    SettingsScreen(
                        onNavigateBack = { currentScreen = AppScreen.Dashboard },
                        onLogoutClick = {
                            TokenManager.token = null // Wylogowanie
                            currentScreen = AppScreen.Login
                        }
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