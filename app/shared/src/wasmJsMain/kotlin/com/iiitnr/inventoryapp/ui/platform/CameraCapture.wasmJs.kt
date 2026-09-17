package com.iiitnr.inventoryapp.ui.platform

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
actual fun CameraCaptureContent(
    onResult: (ImageResult) -> Unit,
    onCancel: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Camera capture not yet implemented for Web")
        TextButton(onClick = onCancel, modifier = Modifier.align(Alignment.TopStart)) {
            Text("Cancel")
        }
    }
}
