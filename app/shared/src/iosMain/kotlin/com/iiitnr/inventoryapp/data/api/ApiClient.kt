package com.iiitnr.inventoryapp.data.api

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.darwin.Darwin

actual fun createHttpClient(block: HttpClientConfig<*>.() -> Unit): HttpClient =
    HttpClient(Darwin) {
        block(this)
        engine {
            configureRequest {
                setAllowsCellularAccess(true)
            }
        }
    }
