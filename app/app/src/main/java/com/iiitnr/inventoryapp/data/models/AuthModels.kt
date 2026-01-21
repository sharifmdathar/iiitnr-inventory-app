package com.iiitnr.inventoryapp.data.models

data class RegisterRequest(
    val email: String, val password: String, val name: String? = null, val role: String? = null
)

data class LoginRequest(
    val email: String, val password: String
)

data class User(
    val id: String,
    val email: String,
    val name: String?,
    val role: String,
    val createdAt: String? = null
)

data class AuthResponse(
    val user: User, val token: String
)

data class ErrorResponse(
    val error: String
)

data class MeResponse(
    val user: User
)
