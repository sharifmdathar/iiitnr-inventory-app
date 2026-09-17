package com.iiitnr.inventoryapp.ui.platform

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kashif.cameraK.compose.CameraPreviewView
import com.kashif.cameraK.compose.rememberCameraKState
import com.kashif.cameraK.enums.CameraLens
import com.kashif.cameraK.result.ImageCaptureResult
import com.kashif.cameraK.state.CameraConfiguration
import com.kashif.cameraK.state.CameraKState
import kotlinx.coroutines.launch
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.posix.memcpy

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    val size = this.length.toInt()
    if (size == 0) return ByteArray(0)
    val result = ByteArray(size)
    result.usePinned { pinned ->
        memcpy(pinned.addressOf(0), this.bytes, this.length)
    }
    return result
}

@Composable
actual fun CameraCaptureContent(
    onResult: (ImageResult) -> Unit,
    onCancel: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val cameraState by rememberCameraKState(
        config = CameraConfiguration(
            cameraLens = CameraLens.BACK,
        ),
    )

    var isCapturing by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        when (val state = cameraState) {
            is CameraKState.Initializing -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.White)
                }
            }
            is CameraKState.Ready -> {
                CameraPreviewView(
                    controller = state.controller,
                    modifier = Modifier.fillMaxSize(),
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = {
                            if (!isCapturing) {
                                isCapturing = true
                                scope.launch {
                                    when (val result = state.controller.takePictureToFile()) {
                                        is ImageCaptureResult.SuccessWithFile -> {
                                            val nsData = NSData.dataWithContentsOfURL(NSURL.fileURLWithPath(result.filePath))
                                            if (nsData != null) {
                                                onResult(ImageResult(nsData.toByteArray(), "capture.jpg"))
                                            } else {
                                                isCapturing = false
                                            }
                                        }
                                        else -> {
                                            isCapturing = false
                                        }
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .size(72.dp)
                            .background(Color.White, CircleShape)
                    ) {
                        if (isCapturing) {
                            CircularProgressIndicator(modifier = Modifier.size(40.dp))
                        } else {
                            Icon(
                                imageVector = Icons.Default.Camera,
                                contentDescription = "Capture",
                                tint = Color.Black,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }
                }
            }
            is CameraKState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Error: ${state.message}", color = Color.White)
                }
            }
        }

        TextButton(
            onClick = onCancel,
            modifier = Modifier.padding(16.dp).align(Alignment.TopStart),
        ) {
            Text("Cancel", color = Color.White)
        }
    }
}
