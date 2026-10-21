package com.iiitnr.inventoryapp.data.cache

import com.iiitnr.inventoryapp.data.models.RequestDraft
import com.iiitnr.inventoryapp.data.models.RequestDraftItem

/**
 * Local persistence for a request the user is still putting together.
 *
 * The draft is keyed by user id, so a shared lab device never shows one
 * student's cart to the next.
 */
interface DraftCache {
    suspend fun getDraft(userId: String): RequestDraft?

    /**
     * The cart of a user, each entry carrying the stock that is available for
     * that component right now so the caller can clamp stale quantities.
     */
    suspend fun getItems(userId: String): List<RequestDraftItem>

    suspend fun save(
        userId: String,
        quantities: Map<String, Int>,
        projectTitle: String?,
        targetFacultyId: String?,
    )

    suspend fun clear(userId: String)
}
