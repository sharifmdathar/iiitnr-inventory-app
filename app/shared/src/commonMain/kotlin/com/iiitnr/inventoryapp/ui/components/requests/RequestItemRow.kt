package com.iiitnr.inventoryapp.ui.components.requests

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iiitnr.inventoryapp.data.models.Component
import com.iiitnr.inventoryapp.data.models.RequestItem
import com.iiitnr.inventoryapp.ui.components.components.ComponentImage
import com.iiitnr.inventoryapp.ui.theme.AppTheme

@Composable
fun RequestItemRow(
    item: RequestItem,
    modifier: Modifier = Modifier,
) {
    val itemName = item.component?.name ?: item.componentId ?: "Unknown Component"
    val componentImageUrl = item.component?.imageUrl

    val requestedQty = item.quantity
    val issuedQty = item.fulfilledQuantity
    val returnedQty = item.returnedQuantity

    val isPartial = (issuedQty in 1..<requestedQty) || (returnedQty in 1..<issuedQty)

    val quantityColor =
        if (isPartial) {
            MaterialTheme.colorScheme.tertiary
        } else {
            MaterialTheme.colorScheme.primary
        }

    val displayQuantity =
        if (issuedQty > 0 || returnedQty > 0) {
            "$returnedQty / $issuedQty / $requestedQty"
        } else {
            "$requestedQty"
        }

    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 2.dp),
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

            Text(
                text = displayQuantity,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                fontWeight = FontWeight.Bold,
                color = quantityColor,
            )
        }
    }
}

@Preview
@Composable
fun RequestItemRowPreview() {
    val sampleComponent =
        Component(
            id = "comp1",
            name = "Arduino Uno",
            description = "Microcontroller board",
            imageUrl = null,
            totalQuantity = 10,
            availableQuantity = 5,
            createdAt = "2023-01-01T00:00:00Z",
            updatedAt = "2023-01-01T00:00:00Z",
        )

    val pendingItem =
        RequestItem(
            id = "item1",
            requestId = "req1",
            componentId = "comp1",
            quantity = 5,
            fulfilledQuantity = 0,
            returnedQuantity = 0,
            component = sampleComponent,
        )

    val partialItem =
        RequestItem(
            id = "item2",
            requestId = "req1",
            componentId = "comp1",
            quantity = 5,
            fulfilledQuantity = 3,
            returnedQuantity = 1,
            component = sampleComponent,
        )

    AppTheme {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.background,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text("Example Request", style = MaterialTheme.typography.titleSmall)
                RequestItemRow(item = pendingItem)
                RequestItemRow(item = partialItem)
            }
        }
    }
}
