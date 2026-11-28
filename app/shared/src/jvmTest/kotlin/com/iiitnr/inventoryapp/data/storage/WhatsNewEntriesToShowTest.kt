package com.iiitnr.inventoryapp.data.storage

import com.iiitnr.inventoryapp.data.Change
import com.iiitnr.inventoryapp.data.ChangeKind
import com.iiitnr.inventoryapp.data.ChangelogEntry
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WhatsNewEntriesToShowTest {
    private val entries =
        listOf(
            ChangelogEntry(
                version = "1.17.10",
                changes = listOf(Change(ChangeKind.NEW, "A")),
            ),
            ChangelogEntry(
                version = "1.17.9",
                changes = listOf(Change(ChangeKind.FIXED, "B")),
            ),
        )

    private fun settings(lastSeen: String?): WhatsNewSettings =
        WhatsNewSettings().apply { lastSeen?.let { runBlocking { setLastSeenVersion(it) } } }

    @Test
    fun freshInstallsSeeTheCurrentEntry() {
        assertEquals(
            listOf("1.17.10"),
            whatsNewEntriesToShow(settings(null), entries, "1.17.10")?.map { it.version },
        )
    }

    @Test
    fun freshInstallsSeeNothingWhenTheCurrentVersionHasNoEntry() {
        assertNull(whatsNewEntriesToShow(settings(null), entries, "1.17.11"))
    }

    @Test
    fun updatedInstallsSeeNothingWhenThereAreNoNewerEntries() {
        assertNull(whatsNewEntriesToShow(settings("1.17.10"), entries, "1.17.11"))
    }

    @Test
    fun updatedInstallsSeeTheCurrentEntry() {
        assertEquals(
            listOf("1.17.10"),
            whatsNewEntriesToShow(settings("1.17.9"), entries, "1.17.10")?.map { it.version },
        )
    }

    @Test
    fun sameVersionSeesNothing() {
        assertNull(whatsNewEntriesToShow(settings("1.17.10"), entries, "1.17.10"))
    }

    @Test
    fun olderThanLastSeenSeesNothing() {
        assertNull(whatsNewEntriesToShow(settings("1.18.0"), entries, "1.17.10"))
    }

    @Test
    fun fallsBackToEntriesSinceLastSeenWhenCurrentHasNoEntry() {
        assertEquals(
            listOf("1.17.10", "1.17.9"),
            whatsNewEntriesToShow(settings("1.17.8"), entries, "1.17.11")?.map { it.version },
        )
    }
}
