package com.iiitnr.inventoryapp.ui.platform

import android.app.Activity
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import kotlinx.coroutines.CompletableDeferred

internal object ImagePickerState {
    var activity: Activity? = null
    var launcher: androidx.activity.result.ActivityResultLauncher<String>? = null
    var pendingResult: CompletableDeferred<ImageResult?>? = null
}

fun setImagePickerActivity(activity: Activity) {
    ImagePickerState.activity = activity
}

@Composable
fun ImagePickerLauncher() {
    val launcher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent(),
        ) { uri: Uri? ->
            val deferred = ImagePickerState.pendingResult
            ImagePickerState.pendingResult = null
            if (uri != null) {
                val ctx = ImagePickerState.activity
                if (ctx != null) {
                    try {
                        ctx.contentResolver.openInputStream(uri)?.use { input ->
                            val bytes = input.readBytes()
                            val filename = uri.lastPathSegment?.substringAfterLast('/') ?: "image"
                            deferred?.complete(ImageResult(bytes, filename))
                        } ?: deferred?.complete(null)
                    } catch (_: Exception) {
                        deferred?.complete(null)
                    }
                } else {
                    deferred?.complete(null)
                }
            } else {
                deferred?.complete(null)
            }
        }

    DisposableEffect(Unit) {
        ImagePickerState.launcher = launcher
        onDispose {
            ImagePickerState.launcher = null
        }
    }
}

actual suspend fun pickImage(): ImageResult? {
    val activity = ImagePickerState.activity
    if (activity == null) {
        return null
    }

    val deferred = CompletableDeferred<ImageResult?>()
    if (ImagePickerState.pendingResult != null) {
        return null
    }
    ImagePickerState.pendingResult = deferred

    val launcher = ImagePickerState.launcher
    if (launcher == null) {
        ImagePickerState.pendingResult = null
        return null
    }

    launcher.launch("image/*")

    return deferred.await()
}
