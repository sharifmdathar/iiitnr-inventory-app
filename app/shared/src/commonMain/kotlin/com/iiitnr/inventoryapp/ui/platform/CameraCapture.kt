package com.iiitnr.inventoryapp.ui.platform

import androidx.compose.runtime.Composable

@Composable
expect fun CameraCaptureContent(
    onResult: (ImageResult) -> Unit,
    onCancel: () -> Unit,
)
