package com.iiitnr.inventoryapp.data.cache

import com.iiitnr.inventoryapp.data.models.RequestDraft
import com.iiitnr.inventoryapp.data.models.RequestDraftItem
import com.iiitnr.inventoryapp.db.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.time.Clock

private fun isoNow(): String = Clock.System.now().toString()

class DefaultDraftCache(
    database: AppDatabase,
) : DraftCache {
    private val queries = database.appDatabaseQueries

    override suspend fun getDraft(userId: String): RequestDraft? =
        withContext(Dispatchers.Default) {
            queries.selectDraft(userId).executeAsOneOrNull()?.let {
                RequestDraft(
                    userId = it.userId,
                    projectTitle = it.projectTitle,
                    targetFacultyId = it.targetFacultyId,
                    updatedAt = it.updatedAt,
                )
            }
        }

    override suspend fun getItems(userId: String): List<RequestDraftItem> =
        withContext(Dispatchers.Default) {
            queries.selectDraftItemsWithAvailability(userId).executeAsList().map { row ->
                RequestDraftItem(
                    componentId = row.componentId,
                    quantity = row.quantity.toInt(),
                    availableQuantity = row.availableQuantity?.toInt(),
                    name = row.name,
                )
            }
        }

    override suspend fun save(
        userId: String,
        quantities: Map<String, Int>,
        projectTitle: String?,
        targetFacultyId: String?,
    ) = withContext(Dispatchers.Default) {
        val now = isoNow()
        queries.transaction {
            queries.deleteAllDraftItems(userId)
            quantities.forEach { (componentId, quantity) ->
                if (quantity > 0) {
                    queries.upsertDraftItem(
                        userId = userId,
                        componentId = componentId,
                        quantity = quantity.toLong(),
                        updatedAt = now,
                    )
                }
            }
            queries.upsertDraft(
                userId = userId,
                projectTitle = projectTitle?.takeIf { it.isNotBlank() },
                targetFacultyId = targetFacultyId,
                updatedAt = now,
            )
        }
    }

    override suspend fun clear(userId: String) =
        withContext(Dispatchers.Default) {
            queries.transaction {
                queries.deleteAllDraftItems(userId)
                queries.deleteDraft(userId)
            }
        }
}
