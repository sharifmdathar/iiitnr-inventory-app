package com.iiitnr.inventoryapp.ui.components.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.iiitnr.inventoryapp.data.models.Component
import com.iiitnr.inventoryapp.ui.components.common.InfoChip

@Composable
fun ComponentCard(
    component: Component,
    isReadOnly: Boolean = false,
    cartQuantity: Int = 0,
    cartQuantities: Map<String, Int>,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAddToCart: (() -> Unit)? = null,
    onUpdateCartQuantity: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = component.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    if (!component.description.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = component.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Row {
                    if (!isReadOnly && cartQuantities.isEmpty()) {
                        IconButton(onClick = onEdit) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Edit",
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                        IconButton(onClick = onDelete) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                    if (cartQuantity == 0) {
                        IconButton(
                            onClick = { onAddToCart?.invoke() },
                            enabled = component.availableQuantity > 0,
                        ) {
                            Icon(
                                Icons.Default.AddShoppingCart,
                                contentDescription = "Add to cart",
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { onUpdateCartQuantity?.invoke(-1) },
                            ) {
                                Icon(
                                    Icons.Default.Remove,
                                    contentDescription = "Decrease quantity",
                                )
                            }
                            Text(
                                text = cartQuantity.toString(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            IconButton(
                                onClick = { onUpdateCartQuantity?.invoke(1) },
                                enabled = cartQuantity < component.availableQuantity,
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = "Increase quantity",
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                InfoChip(
                    label = "Quantity",
                    value = "${component.availableQuantity}/${component.totalQuantity}",
                )
                if (!component.category.isNullOrBlank()) {
                    InfoChip("Category", component.category.replace('_', ' '))
                }
                if (!component.location.isNullOrBlank()) {
                    InfoChip("Location", component.location.replace('_', ' '))
                }
            }
        }
    }
}
