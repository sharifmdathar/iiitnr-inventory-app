package com.iiitnr.inventoryapp.data

import com.iiitnr.inventoryapp.data.Version.isVersionNewer

enum class ChangeKind(
    val label: String,
    val priority: Int,
) {
    NEW("NEW", 0),
    IMPROVED("IMPROVED", 1),
    UI("UI", 2),
    FIXED("FIXED", 3),
    SECURITY("SECURITY", 4),
    ;

    companion object {
        private val PREFIXES =
            mapOf(
                "new" to NEW,
                "feat" to NEW,
                "feature" to NEW,
                "fix" to FIXED,
                "fixed" to FIXED,
                "improved" to IMPROVED,
                "improvement" to IMPROVED,
                "ui" to UI,
                "security" to SECURITY,
            )

        /** Case-insensitive prefix lookup; null for unknown or missing kinds. */
        fun fromPrefix(raw: String): ChangeKind? = PREFIXES[raw.trim().lowercase()]
    }
}

data class Change(
    val kind: ChangeKind?,
    val text: String,
)

data class ChangelogEntry(
    val version: String,
    val changes: List<Change>,
)

fun parseChangelog(markdown: String): List<ChangelogEntry> {
    val entries = mutableListOf<ChangelogEntry>()
    var current: MutableList<Change>? = null
    var currentVersion = ""

    for (rawLine in stripHtmlComments(markdown).lines()) {
        val line = rawLine.trim()
        if (line.startsWith("## ")) {
            currentVersion = line.removePrefix("## ").trim()
            current = mutableListOf()
            entries += ChangelogEntry(currentVersion, current)
        } else if (line.startsWith("- ") && current != null) {
            val body = line.removePrefix("- ")
            val separator = body.indexOf(':')
            val kind =
                if (separator > 0) ChangeKind.fromPrefix(body.take(separator)) else null
            val text = if (kind != null) body.substring(separator + 1).trim() else body
            current += Change(kind, text)
        }
    }

    return entries
        .filter { it.changes.isNotEmpty() }
        .map { entry ->
            entry.copy(changes = entry.changes.sortedBy { it.kind?.priority ?: Int.MAX_VALUE })
        }
}

private fun stripHtmlComments(markdown: String): String {
    val output = StringBuilder(markdown.length)
    var index = 0
    var start = markdown.indexOf("<!--", index)
    while (start >= 0) {
        val end = markdown.indexOf("-->", start + 4)
        if (end < 0) return output.append(markdown, index, start).toString()
        output.append(markdown, index, start)
        index = end + 3
        start = markdown.indexOf("<!--", index)
    }
    output.append(markdown, index, markdown.length)
    return output.toString()
}

private val publishedVersion = Regex("""^\d+(\.\d+)*$""")

/** Entries the app may show; `Unreleased` and other headings stay internal. */
fun List<ChangelogEntry>.publishedEntries(): List<ChangelogEntry> = filter { publishedVersion.matches(it.version) }

fun List<ChangelogEntry>.entryFor(version: String): ChangelogEntry? = firstOrNull { it.version == version }

fun List<ChangelogEntry>.entriesSince(version: String): List<ChangelogEntry> =
    filter { isVersionNewer(version, it.version) }
