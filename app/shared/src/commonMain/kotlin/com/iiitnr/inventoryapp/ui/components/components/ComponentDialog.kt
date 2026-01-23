package com.iiitnr.inventoryapp.ui.components.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.iiitnr.inventoryapp.data.models.Component
import com.iiitnr.inventoryapp.data.models.ComponentCategory
import com.iiitnr.inventoryapp.data.models.ComponentLocation
import com.iiitnr.inventoryapp.data.models.ComponentRequest

@Composable
fun ComponentDialog(
    component: Component?, onDismiss: () -> Unit, onSave: (ComponentRequest) -> Unit
) {
    var name by remember { mutableStateOf(component?.name ?: "") }
    var description by remember { mutableStateOf(component?.description ?: "") }
    var quantity by remember { mutableStateOf(component?.quantity?.toString() ?: "0") }
    var category by remember { mutableStateOf(component?.category ?: "") }
    var location by remember { mutableStateOf(component?.location?.replace('_', ' ') ?: "") }
    var isLoading by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (component != null) "Edit Component" else "Add Component") },
        text = {
            ComponentDialogFields(
                name = name,
                description = description,
                quantity = quantity,
                category = category,
                location = location,
                onNameChange = { name = it },
                onDescriptionChange = { description = it },
                onQuantityChange = { if (it.all { char -> char.isDigit() }) quantity = it },
                onCategoryChange = { category = it },
                onLocationChange = { location = it })
        },
        confirmButton = {
            TextButton(
                onClick = {
                    isLoading = true
                    onSave(
                        ComponentRequest(
                            name = name.trim(),
                            description = description.trim().takeIf { it.isNotBlank() },
                            quantity = quantity.toIntOrNull() ?: 0,
                            category = category.trim().takeIf { it.isNotBlank() },
                            location = location.trim().takeIf { it.isNotBlank() })
                    )
                    isLoading = false
                }, enabled = !isLoading && name.isNotBlank()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp))
                } else {
                    Text("Save")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) {
                Text("Cancel")
            }
        })
}

@Composable
private fun ComponentDialogFields(
    name: String,
    description: String,
    quantity: String,
    category: String,
    location: String,
    onNameChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onQuantityChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onLocationChange: (String) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val categoryOptions = ComponentCategory.labels
        val locationOptions = ComponentLocation.labels

        var isCategoryExpanded by remember { mutableStateOf(false) }
        var isLocationExpanded by remember { mutableStateOf(false) }

        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("Name *") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = description,
            onValueChange = onDescriptionChange,
            label = { Text("Description") },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 3
        )

        OutlinedTextField(
            value = quantity,
            onValueChange = onQuantityChange,
            label = { Text("Quantity") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )

        CategoryDropdownField(
            value = category,
            options = categoryOptions,
            expanded = isCategoryExpanded,
            onExpandedChange = { isCategoryExpanded = it },
            onSelect = onCategoryChange
        )

        LocationDropdownField(
            value = location,
            options = locationOptions,
            expanded = isLocationExpanded,
            onExpandedChange = { isLocationExpanded = it },
            onSelect = onLocationChange
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryDropdownField(
    value: String,
    options: List<String>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSelect: (String) -> Unit
) {
    ExposedDropdownMenuBox(
        expanded = expanded, onExpandedChange = onExpandedChange
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            label = { Text("Category") },
            modifier = Modifier.fillMaxWidth().menuAnchor(
                    type = ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true
                ),
            readOnly = true,
            singleLine = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) })
        DropdownMenu(
            expanded = expanded, onDismissRequest = { onExpandedChange(false) }) {
            options.forEach { option ->
                DropdownMenuItem(text = { Text(option) }, onClick = {
                    onSelect(option)
                    onExpandedChange(false)
                })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LocationDropdownField(
    value: String,
    options: List<String>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSelect: (String) -> Unit
) {
    ExposedDropdownMenuBox(
        expanded = expanded, onExpandedChange = onExpandedChange
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            label = { Text("Location") },
            modifier = Modifier.fillMaxWidth().menuAnchor(
                    type = ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true
                ),
            readOnly = true,
            singleLine = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) })
        DropdownMenu(
            expanded = expanded, onDismissRequest = { onExpandedChange(false) }) {
            options.forEach { option ->
                DropdownMenuItem(text = { Text(option) }, onClick = {
                    onSelect(option)
                    onExpandedChange(false)
                })
            }
        }
    }
}
