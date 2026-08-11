package com.iiitnr.inventoryapp.utils

import com.iiitnr.inventoryapp.data.models.User

fun compactUserLabel(user: User): String {
    val displayName = user.name?.takeIf { it.isNotBlank() } ?: user.email
    val branch = user.branch?.takeIf { it.isNotBlank() }
    val batch = user.batch?.takeIf { it.isNotBlank() }?.replace("-", "–")
    val suffix = listOfNotNull(branch, batch).joinToString(" ")
    return if (suffix.isBlank()) displayName else "$displayName ($suffix)"
}

fun buildUserDetailsLabel(
    prefix: String,
    user: User,
): String {
    val details = mutableListOf<String>()
    user.name?.takeIf { it.isNotBlank() }?.let { details += "Name: $it" }
    user.batch?.takeIf { it.isNotBlank() }?.let { details += "Batch: $it" }
    user.branch?.takeIf { it.isNotBlank() }?.let { details += "Branch: $it" }

    val label = details.joinToString(" • ")
    return if (label.isBlank()) {
        "$prefix: ${user.email}"
    } else {
        "$prefix: $label"
    }
}
