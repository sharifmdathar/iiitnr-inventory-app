package com.iiitnr.inventoryapp.data.api

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.android.Android

actual fun createHttpClient(block: HttpClientConfig<*>.() -> Unit): HttpClient =
    HttpClient(Android) {
        engine {
            connectTimeout = 30_000
            socketTimeout = 30_000
        }
        block()
    }
