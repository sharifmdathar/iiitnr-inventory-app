package com.iiitnr.inventoryapp.ui.components.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.iiitnr.inventoryapp.data.models.Component

@Composable
fun ComponentsList(
    components: List<Component>,
    isReadOnly: Boolean,
    cartQuantities: Map<String, Int>,
    onEdit: (Component) -> Unit,
    onDelete: (Component) -> Unit,
    onAddToCart: (Component) -> Unit,
    onUpdateCartQuantity: (Component, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)
    ) {
        items(components) { component ->
            ComponentCard(
                component = component,
                isReadOnly = isReadOnly,
                cartQuantity = cartQuantities[component.id] ?: 0,
                cartQuantities = cartQuantities,
                onEdit = { onEdit(component) },
                onDelete = { onDelete(component) },
                onAddToCart = { onAddToCart(component) },
                onUpdateCartQuantity = { delta -> onUpdateCartQuantity(component, delta) }
            )
        }
    }
}
