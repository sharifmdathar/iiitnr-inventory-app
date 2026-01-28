package com.iiitnr.inventoryapp.ui.components.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.iiitnr.inventoryapp.data.models.Component
import com.iiitnr.inventoryapp.data.models.User

@Composable
fun CartDialog(
    components: List<Component>,
    cartQuantities: Map<String, Int>,
    cartError: String?,
    isSubmitting: Boolean,
    facultyOptions: List<User>,
    selectedFacultyId: String?,
    isLoadingFaculty: Boolean,
    onSelectFaculty: (String?) -> Unit,
    projectTitle: String,
    onProjectTitleChange: (String) -> Unit,
    onUpdateQuantity: (Component, Int) -> Unit,
    onRemoveItem: (Component) -> Unit,
    onDismiss: () -> Unit,
    onSubmit: () -> Unit,
) {
    var isFacultyExpanded by remember { mutableStateOf(false) }
    val cartItems = cartQuantities.mapNotNull { (componentId, quantity) ->
        components.find { it.id == componentId }?.let { it to quantity }
    }

    AlertDialog(onDismissRequest = onDismiss, title = { Text("Shopping Cart") }, text = {
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        ) {
            if (cartItems.isEmpty()) {
                Text(
                    "Cart is empty",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                cartItems.forEach { (component, quantity) ->
                    CartItemRow(
                        component = component,
                        quantity = quantity,
                        onUpdateQuantity = { delta -> onUpdateQuantity(component, delta) },
                        onRemove = { onRemoveItem(component) },
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            FacultyDropdownField(
                facultyOptions = facultyOptions,
                selectedFacultyId = selectedFacultyId,
                isLoading = isLoadingFaculty,
                expanded = isFacultyExpanded,
                onExpandedChange = { isFacultyExpanded = it },
                onSelect = onSelectFaculty,
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = projectTitle,
                onValueChange = onProjectTitleChange,
                label = { Text("Project Title") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            if (cartError != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = cartError,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }, confirmButton = {
        TextButton(
            onClick = onSubmit,
            enabled = !isSubmitting && cartItems.isNotEmpty() && selectedFacultyId != null && projectTitle.isNotBlank(),
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp))
            } else {
                Text("Submit Request")
            }
        }
    }, dismissButton = {
        TextButton(onClick = onDismiss, enabled = !isSubmitting) {
            Text("Cancel")
        }
    })
}

@Composable
private fun CartItemRow(
    component: Component,
    quantity: Int,
    onUpdateQuantity: (Int) -> Unit,
    onRemove: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = component.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = "Available: ${component.availableQuantity}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            IconButton(onClick = { onUpdateQuantity(-1) }) {
                Icon(Icons.Default.Remove, contentDescription = "Decrease")
            }
            Text(
                text = quantity.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            IconButton(
                onClick = { onUpdateQuantity(1) },
                enabled = quantity < component.availableQuantity,
            ) {
                Icon(Icons.Default.Add, contentDescription = "Increase")
            }
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Remove from cart",
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FacultyDropdownField(
    facultyOptions: List<User>,
    selectedFacultyId: String?,
    isLoading: Boolean,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSelect: (String?) -> Unit,
) {
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = onExpandedChange,
    ) {
        OutlinedTextField(
            value = facultyOptions.find { it.id == selectedFacultyId }
                ?.let { it.name ?: it.email } ?: "",
            onValueChange = {},
            label = { Text("Target Faculty") },
            modifier = Modifier.fillMaxWidth().menuAnchor(
                type = ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                enabled = !isLoading,
            ),
            readOnly = true,
            singleLine = true,
            placeholder = { Text("Select faculty...") },
            trailingIcon = {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp))
                } else {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                }
            },
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
        ) {
            facultyOptions.forEach { faculty ->
                DropdownMenuItem(text = { Text(faculty.name ?: faculty.email) }, onClick = {
                    onSelect(faculty.id)
                    onExpandedChange(false)
                })
            }
        }
    }
}
