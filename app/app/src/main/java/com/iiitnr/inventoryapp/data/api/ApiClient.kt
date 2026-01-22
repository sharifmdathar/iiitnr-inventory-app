package com.iiitnr.inventoryapp.data.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    private const val BASE_URL = "https://iiitnr-inventory-backend.onrender.com"
//    private const val BASE_URL = "http://10.0.2.2:4000"   // For Testing on Emulator

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder().addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS).build()

    private val retrofit = Retrofit.Builder().baseUrl(BASE_URL).client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create()).build()

    val authApiService: AuthApiService = retrofit.create(AuthApiService::class.java)
    val componentApiService: ComponentApiService = retrofit.create(ComponentApiService::class.java)
    val requestApiService: RequestApiService = retrofit.create(RequestApiService::class.java)
}
