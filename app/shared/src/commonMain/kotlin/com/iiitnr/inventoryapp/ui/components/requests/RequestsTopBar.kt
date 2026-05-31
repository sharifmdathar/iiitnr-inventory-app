package com.iiitnr.inventoryapp.ui.components.requests

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.iiitnr.inventoryapp.ui.components.common.AppTopBar

@Composable
fun RequestsTopBar(
    onNavigateBack: () -> Unit,
    onScanRequestClick: (() -> Unit)? = null,
    role: String? = null,
    modifier: Modifier = Modifier,
) {
    AppTopBar(
        title = "Requests",
        modifier = modifier,
        role = role,
        onNavigateBack = onNavigateBack,
        actions = {
            if (onScanRequestClick != null) {
                IconButton(onClick = onScanRequestClick) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = "Scan request QR or enter request ID",
                    )
                }
            }
        },
    )
}
