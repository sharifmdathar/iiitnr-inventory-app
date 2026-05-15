package com.iiitnr.inventoryapp.ui.components.requests

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun RenewReasonDialog(
    reason: String,
    onReasonChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val canSubmit = reason.trim().isNotEmpty()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Request renewal") },
        text = {
            OutlinedTextField(
                value = reason,
                onValueChange = onReasonChange,
                label = { Text("Reason") },
                placeholder = { Text("Why do you need an extension?") },
                singleLine = false,
                minLines = 2,
                maxLines = 4,
                modifier = modifier.fillMaxWidth().padding(vertical = 8.dp),
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = canSubmit,
            ) {
                Text(
                    "Submit",
                    color =
                        if (canSubmit) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        },
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
