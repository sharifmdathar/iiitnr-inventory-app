package com.iiitnr.inventoryapp.ui.platform

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

actual fun isQrScanAvailable(): Boolean = false

@Composable
actual fun QrScannerContent(
    onResult: (String) -> Unit,
    onCancel: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Camera QR scanning is disabled on Desktop. Use OTP or Request ID.", color = Color.White)
        }

        TextButton(
            onClick = onCancel,
            modifier = Modifier.padding(16.dp).align(Alignment.TopEnd),
        ) {
            Text("Cancel", color = Color.White)
        }
    }
}
