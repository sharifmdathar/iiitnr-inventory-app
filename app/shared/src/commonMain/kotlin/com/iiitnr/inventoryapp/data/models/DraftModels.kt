package com.iiitnr.inventoryapp.data.models

/** A request the user is still assembling. */
data class RequestDraft(
    val userId: String,
    val projectTitle: String? = null,
    val targetFacultyId: String? = null,
    val updatedAt: String? = null,
)

/**
 * One line of a draft cart.
 *
 * [availableQuantity] is the stock the component has *now* (from the cached
 * catalog) and is null when the component no longer exists — the caller uses
 * it to drop stale lines and clamp quantities that are no longer available.
 */
data class RequestDraftItem(
    val componentId: String,
    val quantity: Int,
    val availableQuantity: Int? = null,
    val name: String? = null,
)
