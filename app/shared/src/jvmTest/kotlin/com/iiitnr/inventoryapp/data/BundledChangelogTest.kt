package com.iiitnr.inventoryapp.data

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class BundledChangelogTest {
    private val file = File("../../CHANGELOG.md")

    private val entries: List<ChangelogEntry> by lazy {
        assertTrue(file.exists(), "CHANGELOG.md is missing at ${file.path}")
        parseChangelog(file.readText())
    }

    @Test
    fun buildStepCopiesTheRootFileIntoComposeResources() {
        val generated = File("src/commonMain/composeResources/files/CHANGELOG.md")

        assertTrue(generated.isFile, "Gradle did not copy CHANGELOG.md into Compose resources")
        assertEquals(file.readText(), generated.readText(), "Generated CHANGELOG.md is stale")
    }

    @Test
    fun everyBulletHasARecognizedKind() {
        val untagged =
            entries
                .flatMap { entry -> entry.changes.map { entry.version to it } }
                .filter { (_, change) -> change.kind == null }
                .map { (version, change) -> "$version: \"${change.text}\"" }

        assertEquals(emptyList(), untagged, "Bullets missing a `kind:` prefix in CHANGELOG.md")
    }

    @Test
    fun everyVersionIsSemanticOrUnreleased() {
        val bad =
            entries
                .map { it.version }
                .filterNot { it.equals("Unreleased", ignoreCase = true) }
                .filterNot { version -> version.split('.').all { it.toIntOrNull() != null } }

        assertEquals(emptyList(), bad, "CHANGELOG.md has headers that are neither `Unreleased` nor numeric versions")
    }

    @Test
    fun currentVersionHasAnEntry() {
        val current = Version.CURRENT_VERSION
        val unreleaseLike =
            entries.none { it.version == current } &&
                entries.any { it.version.equals("Unreleased", ignoreCase = true) }

        assertTrue(
            !unreleaseLike,
            "v$current ships but CHANGELOG.md still has an Unreleased section — rename it to $current",
        )
        assertNotNull(entries.entryFor(current), "CHANGELOG.md has no entry for the current version v$current")
    }
}
