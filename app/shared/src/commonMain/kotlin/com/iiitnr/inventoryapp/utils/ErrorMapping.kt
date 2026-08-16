package com.iiitnr.inventoryapp.utils

import com.iiitnr.inventoryapp.data.models.ApiErrorResponse
import com.iiitnr.inventoryapp.data.models.AppError
import io.ktor.client.plugins.ResponseException
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }

suspend fun Throwable.toAppError(): AppError {
    return when (this) {
        is ResponseException -> {
            val status = response.status
            val body = runCatching { response.bodyAsText() }.getOrNull()
            val apiError = body?.let {
                runCatching { json.decodeFromString<ApiErrorResponse>(it) }.getOrNull()
            }

            when (status) {
                HttpStatusCode.Unauthorized -> AppError.Unauthorized
                HttpStatusCode.Forbidden -> AppError.Forbidden(apiError?.error ?: "Access denied")
                HttpStatusCode.NotFound -> AppError.NotFound(apiError?.error ?: "Resource")
                else -> AppError.ApiError(
                    code = apiError?.code, status = status.value, message = apiError?.error ?: "API Error"
                )
            }
        }

        else -> AppError.Unknown(this)
    }
}
