package com.iiitnr.inventoryapp.ui.platform

import androidx.compose.runtime.Composable

expect fun isQrScanAvailable(): Boolean

@Composable
expect fun QrScannerContent(
    onResult: (String) -> Unit,
    onCancel: () -> Unit,
)
