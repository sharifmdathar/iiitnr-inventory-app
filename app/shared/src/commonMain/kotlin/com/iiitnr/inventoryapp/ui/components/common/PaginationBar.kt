package com.iiitnr.inventoryapp.ui.components.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PaginationBar(
    currentOffset: Int,
    pageSize: Int,
    totalCount: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    val currentPage = (currentOffset / pageSize) + 1
    val totalPages = ((totalCount + pageSize - 1) / pageSize).coerceAtLeast(1)

    Surface(tonalElevation = 2.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onPrevious,
                enabled = currentOffset > 0,
            ) {
                Icon(Icons.Default.ChevronLeft, "Previous")
            }
            Text(
                text = "Page $currentPage of $totalPages",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
            IconButton(
                onClick = onNext,
                enabled = currentOffset + pageSize < totalCount,
            ) {
                Icon(Icons.Default.ChevronRight, "Next")
            }
            Spacer(Modifier.padding(horizontal = 6.dp))
            Text(
                text = "$totalCount total",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
