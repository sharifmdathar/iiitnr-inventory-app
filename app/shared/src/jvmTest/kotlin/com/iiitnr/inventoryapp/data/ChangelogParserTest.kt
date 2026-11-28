package com.iiitnr.inventoryapp.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ChangelogParserTest {
    @Test
    fun parsesVersionsAndTaggedBullets() {
        val entries =
            parseChangelog(
                """
                # Changelog

                ## 1.17.10
                - new: Pay fines in the app
                - fix: Rate limiting no longer locks the app

                ## 1.17.9
                - ui: Search bar corner radius
                """.trimIndent(),
            )

        assertEquals(2, entries.size)
        assertEquals("1.17.10", entries[0].version)
        assertEquals(2, entries[0].changes.size)
        assertEquals(ChangeKind.NEW, entries[0].changes[0].kind)
        assertEquals("Pay fines in the app", entries[0].changes[0].text)
        assertEquals(ChangeKind.FIXED, entries[0].changes[1].kind)
        assertEquals("1.17.9", entries[1].version)
        assertEquals(ChangeKind.UI, entries[1].changes[0].kind)
    }

    @Test
    fun kindPrefixesAreCaseInsensitive() {
        val entry = parseChangelog("## 1.0.0\n- FIX: Something broke\n- New: Something added")[0]

        assertEquals(listOf(ChangeKind.NEW, ChangeKind.FIXED), entry.changes.map { it.kind })
        assertEquals(listOf("Something added", "Something broke"), entry.changes.map { it.text })
    }

    @Test
    fun acceptsAliases() {
        val entry =
            parseChangelog(
                "## 1.0.0\n- fixed: A\n- feat: B\n- feature: C\n- improvement: D",
            )[0]

        assertEquals(
            listOf(ChangeKind.NEW, ChangeKind.NEW, ChangeKind.IMPROVED, ChangeKind.FIXED),
            entry.changes.map { it.kind },
        )
        assertEquals(listOf("B", "C", "D", "A"), entry.changes.map { it.text })
    }

    @Test
    fun groupsBulletsByKindPriorityAndKeepsRelativeOrder() {
        val entry =
            parseChangelog(
                """
                ## 1.0.0
                - fix: First fix
                - new: First new
                - security: Safer
                - ui: Shiny
                - improved: Faster
                - new: Second new
                - fix: Second fix
                """.trimIndent(),
            )[0]

        assertEquals(
            listOf(
                ChangeKind.NEW,
                ChangeKind.NEW,
                ChangeKind.IMPROVED,
                ChangeKind.UI,
                ChangeKind.FIXED,
                ChangeKind.FIXED,
                ChangeKind.SECURITY,
            ),
            entry.changes.map { it.kind },
        )
        assertEquals(
            listOf("First new", "Second new", "Faster", "Shiny", "First fix", "Second fix", "Safer"),
            entry.changes.map { it.text },
        )
    }

    @Test
    fun unknownOrMissingPrefixKeepsTheTextWithoutAKind() {
        val entry = parseChangelog("## 1.0.0\n- typoed: Gone\n- Just a plain bullet")[0]

        assertNull(entry.changes[0].kind)
        assertEquals("typoed: Gone", entry.changes[0].text)
        assertNull(entry.changes[1].kind)
        assertEquals("Just a plain bullet", entry.changes[1].text)
    }

    @Test
    fun ignoresProseAndComments() {
        val entries =
            parseChangelog(
                """
                <!-- release notes comment -->
                # Changelog
                Some intro paragraph.

                ## 1.0.0
                - new: Real bullet
                """.trimIndent(),
            )

        assertEquals(1, entries.size)
        assertEquals(1, entries[0].changes.size)
    }

    @Test
    fun ignoresHtmlCommentBlocksContainingFakeEntries() {
        val entries =
            parseChangelog(
                """
                <!--
                ## 9.9.9
                - new: Commented-out release
                - fix: Commented-out fix
                -->
                ## 1.0.0
                - new: Real bullet <!-- inline note -->
                - fix: Another real bullet
                """.trimIndent(),
            )

        assertEquals(listOf("1.0.0"), entries.map { it.version })
        assertEquals(
            listOf("Real bullet", "Another real bullet"),
            entries[0].changes.map { it.text },
        )
    }

    @Test
    fun dropsVersionsWithoutBullets() {
        val entries = parseChangelog("## 1.0.1\n\n## 1.0.0\n- fix: A")

        assertEquals(listOf("1.0.0"), entries.map { it.version })
    }

    @Test
    fun publishedEntriesDropUnreleasedAndOtherHeadings() {
        val entries =
            parseChangelog(
                """
                ## Unreleased
                - new: Coming soon

                ## 1.17.10
                - fix: Released
                """.trimIndent(),
            )

        assertEquals(listOf("1.17.10"), entries.publishedEntries().map { it.version })
    }

    @Test
    fun lookupsFilterByNewerVersions() {
        val entries = parseChangelog("## 1.17.10\n- new: A\n\n## 1.17.9\n- new: B")

        assertEquals("1.17.10", entries.entryFor("1.17.10")?.version)
        assertNull(entries.entryFor("1.17.11"))
        assertEquals(listOf("1.17.10"), entries.entriesSince("1.17.9").map { it.version })
        assertTrue(entries.entriesSince("1.17.10").isEmpty())
    }
}
