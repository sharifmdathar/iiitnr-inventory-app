package com.iiitnr.inventoryapp.ui.components.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ComponentsTopBar(
    onNavigateToHome: () -> Unit,
    onNavigateToRequests: () -> Unit,
    pendingRequestsCount: Int? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Components",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onNavigateToRequests) {
                Box {
                    Text(
                        text = "Requests",
                        color = MaterialTheme.colorScheme.primary,
                    )
                    if (pendingRequestsCount != null && pendingRequestsCount > 0) {
                        val count =
                            if (pendingRequestsCount > 99) "99+" else pendingRequestsCount.toString()
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
            TextButton(onClick = onNavigateToHome) {
                Text(
                    "Profile",
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
