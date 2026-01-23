package com.iiitnr.inventoryapp.data.api

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

expect fun createHttpClient(block: HttpClientConfig<*>.() -> Unit = {}): HttpClient

object ApiClient {
    private const val BASE_URL = "https://iiitnr-inventory-backend.onrender.com"
    // For Android Emulator: "http://10.0.2.2:4000"
    // For Desktop: "http://localhost:4000"

    val client: HttpClient = createHttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
                encodeDefaults = false
            })
        }
        install(Logging) {
            level = LogLevel.BODY
        }
    }

    val authApiService: AuthApiService = AuthApiService(client, BASE_URL)
    val componentApiService: ComponentApiService = ComponentApiService(client, BASE_URL)
    val requestApiService: RequestApiService = RequestApiService(client, BASE_URL)
}
