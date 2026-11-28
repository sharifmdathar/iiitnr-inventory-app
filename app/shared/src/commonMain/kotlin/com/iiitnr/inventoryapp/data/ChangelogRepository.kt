package com.iiitnr.inventoryapp.data

import com.iiitnr.inventoryapp.shared.Res
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ChangelogRepository {
    private val mutex = Mutex()
    private var cached: List<ChangelogEntry>? = null

    suspend fun entries(): List<ChangelogEntry> {
        cached?.let { return it }
        return mutex.withLock {
            cached ?: readEntries()?.also { cached = it } ?: emptyList()
        }
    }

    // Resource readers surface platform-specific failures; a missing or corrupt
    // asset must not take down the client, but cancellation still has to propagate.
    @Suppress("TooGenericExceptionCaught")
    private suspend fun readEntries(): List<ChangelogEntry>? =
        try {
            parseChangelog(Res.readBytes(CHANGELOG_RESOURCE).decodeToString()).publishedEntries()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            println("Changelog: failed to read the bundled changelog: ${e.message}")
            null
        }

    private companion object {
        const val CHANGELOG_RESOURCE = "files/CHANGELOG.md"
    }
}
