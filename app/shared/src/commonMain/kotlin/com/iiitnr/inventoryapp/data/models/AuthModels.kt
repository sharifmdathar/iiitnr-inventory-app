package com.iiitnr.inventoryapp.data.models

import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(
    val email: String,
    val password: String,
    val name: String? = null,
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
)

@Serializable
data class GoogleSignInRequest(
    val idToken: String,
)

@Serializable
data class User(
    val id: String,
    val email: String,
    val name: String? = null,
    val imageUrl: String? = null,
    val role: String,
    val batch: String? = null,
    val branch: String? = null,
    val createdAt: String? = null,
)

@Serializable
data class AuthResponse(
    val user: User,
    val token: String,
)

@Serializable
data class ErrorResponse(
    val error: String,
)

@Serializable
data class MeResponse(
    val user: User,
)

@Serializable
data class UserResponse(
    val user: User,
)

@Serializable
data class UsersResponse(
    val users: List<User>,
    val pagination: UserPagination,
)

@Serializable
data class UserPagination(
    val total: Int,
    val limit: Int,
    val offset: Int,
    val hasMore: Boolean,
)

@Serializable
data class UpdateUserRequest(
    val name: String? = null,
    val role: String? = null,
    val batch: String? = null,
    val branch: String? = null,
)
