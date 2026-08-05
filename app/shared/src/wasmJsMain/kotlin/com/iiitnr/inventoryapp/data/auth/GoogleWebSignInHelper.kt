package com.iiitnr.inventoryapp.data.auth

import kotlinx.coroutines.await
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsString
import kotlin.js.Promise
import kotlin.js.js

@OptIn(ExperimentalWasmJsInterop::class)
class GoogleWebSignInHelper {
    suspend fun signIn(): String? =
        try {
            startGoogleSignIn().await()?.toString()
        } catch (_: Throwable) {
            null
        }
}

@OptIn(ExperimentalWasmJsInterop::class)
private fun startGoogleSignIn(): Promise<JsString?> = js("window.iiitnrGoogleSignIn()")
