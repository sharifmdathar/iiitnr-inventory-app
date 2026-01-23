package com.iiitnr.inventoryapp.data.models

import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(
    val email: String,
    val password: String,
    val name: String? = null,
    val role: String? = null
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class User(
    val id: String,
    val email: String,
    val name: String? = null,
    val role: String,
    val createdAt: String? = null
)

@Serializable
data class AuthResponse(
    val user: User,
    val token: String
)

@Serializable
data class ErrorResponse(
    val error: String
)

@Serializable
data class MeResponse(
    val user: User
)
