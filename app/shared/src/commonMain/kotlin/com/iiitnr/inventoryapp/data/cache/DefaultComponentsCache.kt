package com.iiitnr.inventoryapp.data.cache

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.iiitnr.inventoryapp.data.models.Component
import com.iiitnr.inventoryapp.db.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class DefaultComponentsCache(
    private val database: AppDatabase,
) : ComponentsCache {
    private val queries = database.appDatabaseQueries

    override suspend fun getCached(): List<Component> =
        withContext(Dispatchers.IO) {
            queries.selectAll().executeAsList().map { dbComponent ->
                dbComponent.toAppComponent()
            }
        }

    override fun componentsFlow(): Flow<List<Component>> =
        queries
            .selectAll()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { list -> list.map { it.toAppComponent() } }

    override suspend fun save(
        components: List<Component>,
        lastModified: String?,
    ) = withContext(Dispatchers.IO) {
        queries.transaction {
            queries.deleteAllComponents()
            components.forEach { c ->
                queries.insertComponent(
                    id = c.id,
                    name = c.name,
                    description = c.description,
                    imageUrl = c.imageUrl,
                    totalQuantity = c.totalQuantity.toLong(),
                    availableQuantity = c.availableQuantity.toLong(),
                    category = c.category,
                    location = c.location,
                    createdAt = c.createdAt,
                    updatedAt = c.updatedAt,
                )
            }
            queries.saveLastModified(lastModified)
        }
    }
}
