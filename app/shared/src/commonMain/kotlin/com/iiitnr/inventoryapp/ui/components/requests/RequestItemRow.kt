package com.iiitnr.inventoryapp.ui.components.requests

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iiitnr.inventoryapp.data.models.RequestItem
import com.iiitnr.inventoryapp.ui.components.components.ComponentImage

@Composable
fun RequestItemRow(
    item: RequestItem,
    modifier: Modifier = Modifier,
    showFulfilled: Boolean = false,
) {
    val itemName = item.component?.name ?: item.componentId ?: "Unknown Component"
    val componentImageUrl = item.component?.imageUrl
    val fulfilledQty = item.fulfilledQuantity
    val isPartiallyFulfilled = showFulfilled && fulfilledQty > 0 && fulfilledQty < item.quantity
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ComponentImage(
            imageUrl = componentImageUrl,
            modifier = Modifier.size(40.dp),
        )
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = itemName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
            if (showFulfilled && fulfilledQty > 0) {
                Text(
                    text =
                        if (isPartiallyFulfilled) {
                            "x$fulfilledQty/${item.quantity}"
                        } else {
                            "x${item.quantity}"
                        },
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                    fontWeight = FontWeight.Bold,
                    color =
                        if (isPartiallyFulfilled) {
                            MaterialTheme.colorScheme.tertiary
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                )
            } else {
                Text(
                    text = "x${item.quantity}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
