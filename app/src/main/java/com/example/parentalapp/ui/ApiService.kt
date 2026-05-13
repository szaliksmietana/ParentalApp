package com.example.parentalapp.network

import com.example.parentalapp.BuildConfig
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST


object TokenManager {
    var token: String? = null
}


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


data class ChildResponse(
    val id: String,
    val username: String,
    val email: String?
)

interface FamilyGuardApi {
    @Headers("Connection: close")
    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): UserResponse

    @Headers("Connection: close")
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): TokenResponse

    @Headers("Connection: close")
    @GET("children")
    suspend fun getChildren(): List<ChildResponse>
}


object RetrofitInstance {
    private const val BASE_URL = BuildConfig.BASE_URL

    private val client = OkHttpClient.Builder().addInterceptor { chain ->
        val requestBuilder = chain.request().newBuilder()
        TokenManager.token?.let {
            requestBuilder.addHeader("Authorization", "Bearer $it")
        }
        chain.proceed(requestBuilder.build())
    }.build()

    val api: FamilyGuardApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(FamilyGuardApi::class.java)
    }
}