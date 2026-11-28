package com.iiitnr.inventoryapp.data.storage

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.iiitnr.inventoryapp.db.AppDatabase
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WhatsNewSettingsTest {
    private lateinit var driver: JdbcSqliteDriver
    private lateinit var database: AppDatabase

    @BeforeTest
    fun setUp() {
        val dir = Files.createTempDirectory("iiitnr-whatsnew-test").toFile()
        dir.deleteOnExit()
        val file = File(dir, "test.db")
        file.deleteOnExit()

        driver = JdbcSqliteDriver("jdbc:sqlite:${file.absolutePath}")
        AppDatabase.Schema.create(driver)
        database = AppDatabase(driver)
    }

    @AfterTest
    fun tearDown() {
        driver.close()
    }

    @Test
    fun defaultsToNullWhenNothingIsStored() {
        assertNull(WhatsNewSettings(database).lastSeenVersion())
    }

    @Test
    fun persistsTheLastSeenVersionAcrossInstances() =
        kotlinx.coroutines.runBlocking {
            WhatsNewSettings(database).setLastSeenVersion("1.17.10")

            // A fresh instance (next app launch) reads the stored row back.
            assertEquals("1.17.10", WhatsNewSettings(database).lastSeenVersion())
            assertEquals(
                "1.17.10",
                database.appDatabaseQueries.getSetting("last_seen_whats_new_version").executeAsOneOrNull(),
            )
        }

    @Test
    fun usesTheFallbackStoreWhenThereIsNoDatabase() =
        kotlinx.coroutines.runBlocking {
            val store = InMemoryKeyValueStore()
            WhatsNewSettings(fallbackStore = store).setLastSeenVersion("1.17.10")

            assertEquals("1.17.10", store.get("last_seen_whats_new_version"))
            assertEquals("1.17.10", WhatsNewSettings(fallbackStore = store).lastSeenVersion())
        }

    @Test
    fun prefersTheDatabaseOverTheFallbackStore() =
        kotlinx.coroutines.runBlocking {
            val store = InMemoryKeyValueStore()
            store.put("last_seen_whats_new_version", "1.16.0")
            WhatsNewSettings(database).setLastSeenVersion("1.17.10")

            assertEquals("1.17.10", WhatsNewSettings(database, store).lastSeenVersion())
        }

    private class InMemoryKeyValueStore : KeyValueStore {
        private val values = mutableMapOf<String, String>()

        override fun get(key: String): String? = values[key]

        override fun put(
            key: String,
            value: String,
        ) {
            values[key] = value
        }
    }
}
