package com.iiitnr.inventoryapp.data.cache

import com.iiitnr.inventoryapp.data.models.Component
import kotlinx.coroutines.flow.Flow

interface ComponentsCache {
    suspend fun getCached(): List<Component>

    fun componentsFlow(): Flow<List<Component>>

    suspend fun save(
        components: List<Component>,
        lastModified: String?,
    )
}
