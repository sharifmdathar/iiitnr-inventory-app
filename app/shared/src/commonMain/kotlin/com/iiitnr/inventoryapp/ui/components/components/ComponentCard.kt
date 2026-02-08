package com.iiitnr.inventoryapp.ui.components.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ImageNotSupported
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import coil3.compose.SubcomposeAsyncImage
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
    var enlargedImageUrl by remember { mutableStateOf<String?>(null) }

    if (enlargedImageUrl != null) {
        Dialog(
            onDismissRequest = { enlargedImageUrl = null },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            AsyncImage(
                model = enlargedImageUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize().clickable(onClick = { enlargedImageUrl = null }),
                contentScale = ContentScale.Fit,
            )
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors =
            CardDefaults.cardColors(
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
                ComponentImage(
                    imageUrl = component.imageUrl,
                    modifier = Modifier.size(64.dp),
                    onClick = component.imageUrl?.let { { enlargedImageUrl = it } },
                )
                Spacer(modifier = modifier.width(12.dp))

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

@Composable
internal fun ComponentImage(
    imageUrl: String?,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(8.dp)
    val placeholderContent = @Composable {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.ImageNotSupported,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
    if (!imageUrl.isNullOrBlank()) {
        val imageModifier =
            if (onClick != null) {
                modifier.clip(shape).clickable(onClick = onClick)
            } else {
                modifier.clip(shape)
            }
        SubcomposeAsyncImage(
            model = imageUrl,
            contentDescription = "Component image",
            modifier = imageModifier,
            contentScale = ContentScale.Crop,
            loading = { placeholderContent() },
            error = { placeholderContent() },
        )
    } else {
        Surface(
            modifier = modifier.clip(shape),
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            placeholderContent()
        }
    }
}
