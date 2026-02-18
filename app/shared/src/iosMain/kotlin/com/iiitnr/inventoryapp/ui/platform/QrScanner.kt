package com.iiitnr.inventoryapp.ui.platform

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

actual fun isQrScanAvailable(): Boolean {
    // TODO: Implement QR Scanner for iOS using AVCaptureSession
    return false
}

@Composable
actual fun QrScannerContent(
    onResult: (String) -> Unit,
    onCancel: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("QR Scanner not implemented for iOS yet")
    }
}
