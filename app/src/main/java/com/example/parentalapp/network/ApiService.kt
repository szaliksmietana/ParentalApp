package com.example.parentalapp.network

import android.content.Context
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.util.Properties
import java.util.concurrent.TimeUnit

object AppConfig {
    private var baseUrl: String = "http://10.0.2.2:2244/"

    fun init(context: Context) {
        try {
            val props = Properties()
            context.assets.open("config.properties").use { props.load(it) }
            baseUrl = props.getProperty("BASE_URL", baseUrl)
        } catch (e: Exception) {
            // Fallback
        }
    }

    fun getBaseUrl() = baseUrl
}

object TokenManager {
    var token: String? = null
    var tokenExpiry: Long = 0L

    fun isTokenExpired(): Boolean {
        return System.currentTimeMillis() > tokenExpiry - 5 * 60 * 1000
    }

    fun saveToken(accessToken: String) {
        token = accessToken
        tokenExpiry = System.currentTimeMillis() + 24 * 60 * 60 * 1000
    }
}

// --- Auth ---
data class RegisterRequest(
    val email: String,
    val password: String,
    val username: String,
    val role: String = "parent"
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class UserResponse(
    val id: String,
    val email: String,
    val username: String,
    val role: String
)

data class TokenResponse(
    val access_token: String,
    val token_type: String
)

// --- Devices ---
data class DeviceRegisterRequest(
    val device_name: String? = "Telefon rodzica",
    val platform: String = "android",
    val fcm_token: String? = null,
    val hardware_id: String? = null
)

data class DeviceResponse(
    val id: String,
    val user_id: String,
    val device_name: String?,
    val platform: String,
    val hardware_id: String?,
    val registered_at: String
)

// --- Pairing ---
data class PairedChildResponse(
    val pair_id: String,
    val child_device_id: String,
    val child_user_id: String,
    val username: String,
    val device_name: String?,
    val last_seen: String?,
    val paired_at: String
)

data class ConfirmPairingRequest(
    val code: String,
    val guardian_device_id: String
)

data class PairingConfirmResponse(
    val id: String,
    val child_device_id: String,
    val guardian_device_id: String,
    val paired_at: String,
    val is_active: Boolean
)

// --- Location ---
data class LocationResponse(
    val id: String,
    val device_id: String,
    val latitude: Double,
    val longitude: Double,
    val accuracy_meters: Double?,
    val battery_level: Int?,
    val recorded_at: String
)

// --- Dashboard ---
data class LatestLocation(
    val latitude: Double,
    val longitude: Double,
    val accuracy_meters: Double?,
    val battery_level: Int?,
    val recorded_at: String
)

data class MessageResponse(
    val id: String,
    val sender_device_id: String,
    val receiver_device_id: String,
    val content: String,
    val sent_at: String,
    val read_at: String?
)

data class ChildDashboardResponse(
    val child_device_id: String,
    val username: String,
    val device_name: String?,
    val last_seen: String?,
    val latest_location: LatestLocation?,
    val recent_messages: List<MessageResponse>
)

// --- Messages ---
data class SendMessageRequest(
    val sender_device_id: String,
    val receiver_device_id: String,
    val content: String
)

data class SosAlertResponse(
    val id: String,
    val child_device_id: String,
    val child_username: String,
    val created_at: String
)

interface FamilyGuardApi {
    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): UserResponse

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): TokenResponse

    @POST("auth/refresh")
    suspend fun refreshToken(): TokenResponse

    @POST("devices/register")
    suspend fun registerDevice(@Body request: DeviceRegisterRequest): DeviceResponse

    @GET("pairing/my-children")
    suspend fun getChildren(@Query("device_id") deviceId: String): List<PairedChildResponse>

    @POST("pairing/confirm")
    suspend fun confirmPairing(@Body request: ConfirmPairingRequest): PairingConfirmResponse

    @GET("location/{child_device_id}/latest")
    suspend fun getLatestLocation(@Path("child_device_id") childDeviceId: String): LocationResponse

    @GET("location/{child_device_id}/history")
    suspend fun getLocationHistory(
        @Path("child_device_id") childDeviceId: String,
        @Query("hours") hours: Int = 24
    ): List<LocationResponse>

    @GET("dashboard/child/{child_device_id}")
    suspend fun getChildDashboard(@Path("child_device_id") childDeviceId: String): ChildDashboardResponse

    @POST("messages")
    suspend fun sendMessage(@Body request: SendMessageRequest): MessageResponse

    @POST("messages/{message_id}/read")
    suspend fun markAsRead(
        @Path("message_id") messageId: String,
        @Query("device_id") deviceId: String
    ): MessageResponse

    @GET("sos/pending")
    suspend fun getPendingSos(@Query("device_id") deviceId: String): List<SosAlertResponse>

    // Potwierdzenie (odwołanie) alarmu SOS
    @POST("sos/{sos_id}/acknowledge")
    suspend fun acknowledgeSos(
        @Path("sos_id") sosId: String,
        @Query("device_id") deviceId: String
    )
}

object RetrofitInstance {
    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client = OkHttpClient.Builder()
        .protocols(listOf(Protocol.HTTP_1_1))
        .retryOnConnectionFailure(true)
        .connectionPool(
            ConnectionPool(
                maxIdleConnections = 5,
                keepAliveDuration = 5,
                timeUnit = TimeUnit.MINUTES
            )
        )
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            // Usuń Connection: close — pozwól OkHttp zarządzać połączeniami
            val request = chain.request().newBuilder()
                .removeHeader("Connection")
                .build()
            chain.proceed(request)
        }
        .addInterceptor { chain ->
            val requestBuilder = chain.request().newBuilder()
            TokenManager.token?.let {
                requestBuilder.addHeader("Authorization", "Bearer $it")
            }
            chain.proceed(requestBuilder.build())
        }
        .addInterceptor(logging)
        .build()

    val api: FamilyGuardApi by lazy {
        Retrofit.Builder()
            .baseUrl(AppConfig.getBaseUrl())
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(FamilyGuardApi::class.java)
    }
}