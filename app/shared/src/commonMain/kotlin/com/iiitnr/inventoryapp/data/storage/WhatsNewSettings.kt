package com.iiitnr.inventoryapp.data.storage

import com.iiitnr.inventoryapp.data.ChangelogEntry
import com.iiitnr.inventoryapp.data.Version
import com.iiitnr.inventoryapp.data.Version.isVersionNewer
import com.iiitnr.inventoryapp.data.entriesSince
import com.iiitnr.inventoryapp.data.entryFor
import com.iiitnr.inventoryapp.db.AppDatabase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WhatsNewSettings(
    database: AppDatabase? = null,
    fallbackStore: KeyValueStore? = null,
) {
    private val queries = database?.appDatabaseQueries
    private val store = fallbackStore

    private var persisted = readPersisted()

    suspend fun setLastSeenVersion(version: String) {
        persisted = version

        val settingsQueries = queries
        val fallback = store
        when {
            settingsQueries != null ->
                withContext(Dispatchers.Default) {
                    runCatching {
                        settingsQueries.upsertSetting(LAST_SEEN_VERSION_KEY, version)
                    }.onFailure { failure ->
                        if (failure is CancellationException) throw failure
                    }
                }

            fallback != null -> runCatching { fallback.put(LAST_SEEN_VERSION_KEY, version) }
        }
    }

    fun lastSeenVersion(): String? = persisted

    private fun readPersisted(): String? {
        val fromDb =
            queries?.let { settings ->
                runCatching { settings.getSetting(LAST_SEEN_VERSION_KEY).executeAsOneOrNull() }.getOrNull()
            }
        if (fromDb != null) return fromDb
        return store?.get(LAST_SEEN_VERSION_KEY)
    }

    private companion object {
        const val LAST_SEEN_VERSION_KEY = "last_seen_whats_new_version"
    }
}

interface KeyValueStore {
    fun get(key: String): String?

    fun put(
        key: String,
        value: String,
    )
}

expect fun createWebKeyValueStore(): KeyValueStore?

fun whatsNewEntriesToShow(
    settings: WhatsNewSettings,
    entries: List<ChangelogEntry>,
    currentVersion: String = Version.CURRENT_VERSION,
): List<ChangelogEntry>? {
    val lastSeen = settings.lastSeenVersion()

    return when {
        // There is no reliable way to distinguish a fresh install from an upgrade
        // that predates this feature, so show the current release once either way.
        lastSeen == null -> entries.entryFor(currentVersion)?.let { listOf(it) }
        !isVersionNewer(lastSeen, currentVersion) -> null
        else ->
            entries.entryFor(currentVersion)?.let { listOf(it) }
                ?: entries.entriesSince(lastSeen).takeIf { it.isNotEmpty() }
    }
}
