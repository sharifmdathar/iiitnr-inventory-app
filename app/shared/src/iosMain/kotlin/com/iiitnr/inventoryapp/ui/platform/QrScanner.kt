package com.iiitnr.inventoryapp.ui.platform

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kashif.cameraK.compose.CameraPreviewView
import com.kashif.cameraK.compose.rememberCameraKState
import com.kashif.cameraK.enums.CameraLens
import com.kashif.cameraK.state.CameraConfiguration
import com.kashif.cameraK.state.CameraKState
import com.kashif.qrscannerplugin.rememberQRScannerPlugin
import kotlinx.coroutines.flow.collectLatest

actual fun isQrScanAvailable(): Boolean = true

@Composable
actual fun QrScannerContent(
    onResult: (String) -> Unit,
    onCancel: () -> Unit,
) {
    val qrScannerPlugin = rememberQRScannerPlugin()
    val cameraState by rememberCameraKState(
        config = CameraConfiguration(
            cameraLens = CameraLens.BACK,
        ),
        setupPlugins = { stateHolder ->
            qrScannerPlugin.attachToStateHolder(stateHolder)
        },
    )

    LaunchedEffect(qrScannerPlugin) {
        qrScannerPlugin.getQrCodeFlow().collectLatest { qrCodeString ->
            if (qrCodeString.isNotBlank()) {
                onResult(qrCodeString)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (val state = cameraState) {
            is CameraKState.Initializing -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Initializing Camera...")
                }
            }
            is CameraKState.Ready -> {
                CameraPreviewView(
                    controller = state.controller,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            is CameraKState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Error: ${state.message}")
                }
            }
        }

        TextButton(
            onClick = onCancel,
            modifier = Modifier.padding(16.dp).align(Alignment.TopEnd),
        ) {
            Text("Cancel")
        }
    }
}
