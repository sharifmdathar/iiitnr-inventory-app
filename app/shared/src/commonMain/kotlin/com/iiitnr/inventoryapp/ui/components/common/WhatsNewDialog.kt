package com.iiitnr.inventoryapp.ui.components.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.iiitnr.inventoryapp.data.ChangeKind
import com.iiitnr.inventoryapp.data.ChangelogEntry
import com.iiitnr.inventoryapp.ui.theme.LocalIsDarkTheme
import com.iiitnr.inventoryapp.ui.theme.inventoryColors

@Composable
fun WhatsNewDialog(
    entries: List<ChangelogEntry>,
    onDismiss: () -> Unit,
) {
    if (entries.isEmpty()) return

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Got it") }
        },
        title = {
            Text(if (entries.size == 1) "What's new in v${entries.first().version}" else "What's new")
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                entries.forEachIndexed { entryIndex, entry ->
                    if (entryIndex > 0) {
                        item { Spacer(modifier = Modifier.height(4.dp)) }
                    }
                    if (entries.size > 1) {
                        item {
                            Column {
                                Text(
                                    text = "v${entry.version}",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }
                    }
                    items(entry.changes) { change ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            change.kind?.let { kind -> ChangeKindPill(kind) }
                            Text(
                                text = change.text,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(4.dp)) }
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    )
}

@Composable
private fun ChangeKindPill(kind: ChangeKind) {
    val color = changeKindColor(kind)
    val isDark = LocalIsDarkTheme.current

    Surface(
        modifier = Modifier.clip(RoundedCornerShape(7.dp)),
        color = color.copy(alpha = if (isDark) 0.24f else 0.14f),
        contentColor = color,
        shape = RoundedCornerShape(7.dp),
    ) {
        Text(
            text = kind.label,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
        )
    }
}

@Composable
private fun changeKindColor(kind: ChangeKind): Color {
    val colors = MaterialTheme.inventoryColors
    return when (kind) {
        ChangeKind.NEW -> colors.success
        ChangeKind.FIXED -> colors.danger
        ChangeKind.IMPROVED -> colors.info
        ChangeKind.UI -> colors.actionCyan
        ChangeKind.SECURITY -> colors.warning
    }
}
