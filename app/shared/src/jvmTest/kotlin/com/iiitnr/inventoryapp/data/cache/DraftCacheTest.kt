package com.iiitnr.inventoryapp.data.cache

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.iiitnr.inventoryapp.db.AppDatabase
import kotlinx.coroutines.runBlocking
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DraftCacheTest {
    private lateinit var driver: JdbcSqliteDriver
    private lateinit var database: AppDatabase
    private lateinit var cache: DefaultDraftCache

    @BeforeTest
    fun setUp() =
        runBlocking {
            val dir = Files.createTempDirectory("iiitnr-draft-test").toFile()
            dir.deleteOnExit()
            val file = File(dir, "test.db")
            file.deleteOnExit()

            driver = JdbcSqliteDriver("jdbc:sqlite:${file.absolutePath}")
            AppDatabase.Schema.create(driver)
            database = AppDatabase(driver)
            cache = DefaultDraftCache(database)
        }

    @AfterTest
    fun tearDown() =
        runBlocking {
            driver.close()
        }

    private fun insertComponent(
        id: String,
        availableQuantity: Int,
    ) {
        database.appDatabaseQueries.insertComponent(
            id = id,
            name = "Component $id",
            description = null,
            imageUrl = null,
            totalQuantity = availableQuantity.toLong(),
            availableQuantity = availableQuantity.toLong(),
            category = null,
            location = null,
            createdAt = "2026-01-01T00:00:00Z",
            updatedAt = "2026-01-01T00:00:00Z",
        )
    }

    @Test
    fun savesAndRestoresTheDraft() =
        runBlocking {
            cache.save(
                userId = "user-1",
                quantities = mapOf("comp-1" to 2, "comp-2" to 5),
                projectTitle = "IoT weather station",
                targetFacultyId = "faculty-1",
            )

            val items = cache.getItems("user-1")
            assertEquals(2, items.size)
            assertEquals(2, items.first { it.componentId == "comp-1" }.quantity)
            assertEquals(5, items.first { it.componentId == "comp-2" }.quantity)

            val draft = cache.getDraft("user-1")
            assertNotNull(draft)
            assertEquals("IoT weather station", draft.projectTitle)
            assertEquals("faculty-1", draft.targetFacultyId)
        }

    @Test
    fun keepsTheDraftOfEachUserApart() =
        runBlocking {
            cache.save("user-1", mapOf("comp-1" to 2), "First project", "faculty-1")
            cache.save("user-2", mapOf("comp-2" to 1), "Second project", "faculty-2")

            assertEquals("First project", cache.getDraft("user-1")?.projectTitle)
            assertEquals("Second project", cache.getDraft("user-2")?.projectTitle)
            assertEquals(1, cache.getItems("user-1").size)
            assertEquals(1, cache.getItems("user-2").size)
        }

    @Test
    fun reportsTheStockThatIsAvailableNowSoStaleQuantitiesCanBeClamped() =
        runBlocking {
            insertComponent("comp-1", availableQuantity = 3)
            cache.save("user-1", mapOf("comp-1" to 9), null, null)

            val item = cache.getItems("user-1").single()
            assertEquals(9, item.quantity)
            assertEquals(3, item.availableQuantity)
            assertEquals("Component comp-1", item.name)
        }

    @Test
    fun reportsAMissingComponentAsUnavailable() =
        runBlocking {
            cache.save("user-1", mapOf("gone" to 1), null, null)

            val item = cache.getItems("user-1").single()
            assertNull(item.availableQuantity)
        }

    @Test
    fun savingAgainReplacesThePreviousCart() =
        runBlocking {
            cache.save("user-1", mapOf("comp-1" to 2, "comp-2" to 1), "Project", null)
            cache.save("user-1", mapOf("comp-3" to 4), "Project", null)

            val items = cache.getItems("user-1")
            assertEquals(1, items.size)
            assertEquals("comp-3", items.single().componentId)
        }

    @Test
    fun clearRemovesTheDraft() =
        runBlocking {
            cache.save("user-1", mapOf("comp-1" to 2), "Project", "faculty-1")
            cache.clear("user-1")

            assertTrue(cache.getItems("user-1").isEmpty())
            assertNull(cache.getDraft("user-1"))
        }

    @Test
    fun anEmptyCartIsNotStored() =
        runBlocking {
            cache.save("user-1", mapOf("comp-1" to 0), null, null)

            assertTrue(cache.getItems("user-1").isEmpty())
        }
}
