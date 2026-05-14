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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.lifecycleScope
import com.example.parentalapp.network.AppConfig
import com.example.parentalapp.network.ConfirmPairingRequest
import com.example.parentalapp.network.DeviceRegisterRequest
import com.example.parentalapp.network.LoginRequest
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
import kotlinx.coroutines.launch
import retrofit2.HttpException

data class ChildData(
    val name: String,
    val code: String,
    val batteryLevel: Int? = null
)

enum class AppScreen {
    Login, Register, Dashboard, Map, Chat, AddChild, Settings
}

class MainActivity : ComponentActivity() {

    private var parentDeviceId: String? = null

    private val PREFS_NAME = "parentalapp_prefs"
    private val KEY_DEVICE_ID = "device_id"

    private fun saveDeviceId(context: Context, deviceId: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_DEVICE_ID, deviceId).apply()
    }

    private fun loadDeviceId(context: Context): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_DEVICE_ID, null)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AppConfig.init(this)

        setContent {
            var currentScreen by remember { mutableStateOf(AppScreen.Login) }
            var childrenList by remember { mutableStateOf(listOf<ChildData>()) }
            var selectedChild by remember { mutableStateOf<ChildData?>(null) }
            var pairingLoading by remember { mutableStateOf(false) }
            var pairingError by remember { mutableStateOf<String?>(null) }
            // Licznik odświeżeń — zmiana wartości wymusza ponowne wykonanie LaunchedEffect
            var dashboardRefreshKey by remember { mutableIntStateOf(0) }

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
                                    TokenManager.token = response.access_token

                                    val savedDeviceId = loadDeviceId(context)
                                    if (savedDeviceId != null) {
                                        parentDeviceId = savedDeviceId
                                        Log.d("API_SUCCESS", "Używam zapisanego device_id: $parentDeviceId")
                                    } else {
                                        val device = RetrofitInstance.api.registerDevice(DeviceRegisterRequest())
                                        parentDeviceId = device.id
                                        saveDeviceId(context, device.id)
                                        Log.d("API_SUCCESS", "Zarejestrowano device_id: $parentDeviceId")
                                    }

                                    Toast.makeText(context, "Zalogowano pomyślnie!", Toast.LENGTH_SHORT).show()
                                    dashboardRefreshKey++
                                    currentScreen = AppScreen.Dashboard
                                } catch (e: HttpException) {
                                    Log.e("API_ERROR", "HTTP ${e.code()}: ${e.response()?.errorBody()?.string()}")
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
                                    val response = RetrofitInstance.api.register(
                                        RegisterRequest(email = email, password = password, username = username, role = "parent")
                                    )
                                    Toast.makeText(context, "Konto ${response.username} utworzone!", Toast.LENGTH_SHORT).show()
                                    currentScreen = AppScreen.Login
                                } catch (e: HttpException) {
                                    Log.e("API_ERROR", "HTTP ${e.code()}: ${e.response()?.errorBody()?.string()}")
                                    Toast.makeText(context, "Błąd rejestracji: ${e.code()}", Toast.LENGTH_LONG).show()
                                } catch (e: Exception) {
                                    Log.e("API_ERROR", "Exception: ${e.message}")
                                    Toast.makeText(context, "Błąd połączenia. Sprawdź sieć.", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        onNavigateBack = { currentScreen = AppScreen.Login }
                    )
                }

                AppScreen.Dashboard -> {
                    LaunchedEffect(dashboardRefreshKey) {
                        createNotificationChannel(context)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }

                        val deviceId = parentDeviceId
                        Log.d("DEBUG", "parentDeviceId = $deviceId, refreshKey = $dashboardRefreshKey")

                        if (deviceId != null) {
                            try {
                                val fetchedChildren = RetrofitInstance.api.getChildren(deviceId)
                                childrenList = fetchedChildren.map { child ->
                                    val battery = try {
                                        RetrofitInstance.api.getLatestLocation(child.child_device_id).battery_level
                                    } catch (e: Exception) {
                                        null
                                    }
                                    ChildData(
                                        name = child.username,
                                        code = child.child_device_id,
                                        batteryLevel = battery
                                    )
                                }
                                Log.d("API_SUCCESS", "Pobrano ${childrenList.size} dzieci.")
                            } catch (e: HttpException) {
                                Log.e("API_ERROR", "HTTP ${e.code()}: ${e.response()?.errorBody()?.string()}")
                                if (e.code() == 401) {
                                    TokenManager.token = null
                                    currentScreen = AppScreen.Login
                                }
                            } catch (e: Exception) {
                                Log.e("API_ERROR", "Błąd pobierania dzieci: ${e.message}")
                            }
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
                        onNavigateToAddChild = {
                            pairingError = null
                            currentScreen = AppScreen.AddChild
                        },
                        onNavigateToSettings = { currentScreen = AppScreen.Settings },
                        onLogoutClick = {
                            TokenManager.token = null
                            parentDeviceId = null
                            currentScreen = AppScreen.Login
                        }
                    )
                }

                AppScreen.Map -> {
                    MapScreen(
                        childrenList = childrenList,
                        selectedChild = selectedChild,
                        onNavigateBack = {
                            dashboardRefreshKey++ // Odśwież dashboard po powrocie z mapy
                            currentScreen = AppScreen.Dashboard
                        }
                    )
                }

                AppScreen.Chat -> {
                    ChatScreen(
                        childrenList = childrenList,
                        initialChild = selectedChild,
                        onNavigateBack = {
                            dashboardRefreshKey++
                            currentScreen = AppScreen.Dashboard
                        }
                    )
                }

                AppScreen.AddChild -> {
                    val deviceId = parentDeviceId ?: ""
                    AddChildScreen(
                        guardianDeviceId = deviceId,
                        isLoading = pairingLoading,
                        errorMessage = pairingError,
                        onNavigateBack = { currentScreen = AppScreen.Dashboard },
                        onPairingConfirmed = { code ->
                            lifecycleScope.launch {
                                pairingLoading = true
                                pairingError = null
                                try {
                                    RetrofitInstance.api.confirmPairing(
                                        ConfirmPairingRequest(
                                            code = code,
                                            guardian_device_id = deviceId
                                        )
                                    )
                                    Toast.makeText(context, "Sparowano pomyślnie!", Toast.LENGTH_SHORT).show()
                                    dashboardRefreshKey++
                                    currentScreen = AppScreen.Dashboard
                                } catch (e: HttpException) {
                                    val body = e.response()?.errorBody()?.string()
                                    Log.e("API_ERROR", "HTTP ${e.code()}: $body")
                                    when (e.code()) {
                                        409 -> {
                                            Toast.makeText(context, "Sparowano pomyślnie!", Toast.LENGTH_SHORT).show()
                                            dashboardRefreshKey++
                                            currentScreen = AppScreen.Dashboard
                                        }
                                        404 -> pairingError = "Kod nieprawidłowy lub wygasł"
                                        else -> pairingError = "Błąd serwera: ${e.code()}"
                                    }
                                } catch (e: Exception) {
                                    Log.w("API_WARN", "Exception po confirm: ${e.message}")
                                    Toast.makeText(context, "Sparowano pomyślnie!", Toast.LENGTH_SHORT).show()
                                    dashboardRefreshKey++
                                    currentScreen = AppScreen.Dashboard
                                } finally {
                                    pairingLoading = false
                                }
                            }
                        }
                    )
                }

                AppScreen.Settings -> {
                    SettingsScreen(
                        onNavigateBack = {
                            dashboardRefreshKey++
                            currentScreen = AppScreen.Dashboard
                        },
                        onLogoutClick = {
                            TokenManager.token = null
                            parentDeviceId = null
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