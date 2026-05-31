package com.iiitnr.inventoryapp.ui.components.requests

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AssignmentReturn
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.iiitnr.inventoryapp.ui.components.common.AppTopBar

@Composable
fun RequestsTopBar(
    onNavigateBack: () -> Unit,
    onFulfillByQrClick: (() -> Unit)? = null,
    onReturnByQrClick: (() -> Unit)? = null,
    role: String? = null,
    modifier: Modifier = Modifier,
) {
    AppTopBar(
        title = "Requests",
        modifier = modifier,
        role = role,
        onNavigateBack = onNavigateBack,
        actions = {
            if (onReturnByQrClick != null) {
                IconButton(onClick = onReturnByQrClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.AssignmentReturn,
                        contentDescription = "Record return by request ID or scan QR",
                    )
                }
            }
            if (onFulfillByQrClick != null) {
                IconButton(onClick = onFulfillByQrClick) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = "Fulfill by request ID or scan QR",
                    )
                }
            }
        },
    )
}
