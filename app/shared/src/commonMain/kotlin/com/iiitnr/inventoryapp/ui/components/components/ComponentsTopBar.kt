package com.iiitnr.inventoryapp.ui.components.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iiitnr.inventoryapp.ui.components.common.AppTopBar

@Composable
fun ComponentsTopBar(
    onNavigateToHome: () -> Unit,
    onNavigateToRequests: () -> Unit,
    pendingRequestsCount: Int? = null,
    role: String? = null,
    showExportCsv: Boolean = false,
    onExportCsv: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    AppTopBar(
        title = "Components",
        modifier = modifier,
        role = role,
        actions = {
            if (showExportCsv && onExportCsv != null) {
                IconButton(onClick = onExportCsv) {
                    Icon(
                        imageVector = Icons.Outlined.FileDownload,
                        contentDescription = "Export CSV",
                    )
                }
            }
            TextButton(onClick = onNavigateToRequests) {
                Box {
                    Text(
                        text = "Requests",
                        color = MaterialTheme.colorScheme.primary,
                    )
                    if (pendingRequestsCount != null && pendingRequestsCount > 0) {
                        val count = if (pendingRequestsCount > 99) "99+" else pendingRequestsCount.toString()
                        Box(
                            modifier =
                                Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = 4.dp, y = (-4).dp)
                                    .size(if (pendingRequestsCount >= 10) 16.dp else 14.dp)
                                    .background(MaterialTheme.colorScheme.error, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = count,
                                style =
                                    MaterialTheme.typography.labelSmall.copy(
                                        lineHeight = 12.sp,
                                    ),
                                color = MaterialTheme.colorScheme.onError,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
            IconButton(
                onClick = onNavigateToHome,
            ) {
                Icon(
                    imageVector = Icons.Outlined.AccountCircle,
                    contentDescription = "Profile",
                )
            }
        },
    )
}
