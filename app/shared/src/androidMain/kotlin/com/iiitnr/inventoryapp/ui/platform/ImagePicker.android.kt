package com.iiitnr.inventoryapp.ui.platform

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import kotlinx.coroutines.CompletableDeferred
import java.lang.ref.WeakReference

private const val TAG = "ImagePicker"

internal object ImagePickerState {
    var activityRef: WeakReference<Activity>? = null

    val activity: Activity?
        get() = activityRef?.get()

    var launcher: ActivityResultLauncher<String>? = null
    var pendingResult: CompletableDeferred<ImageResult?>? = null
}

fun setImagePickerActivity(activity: Activity) {
    ImagePickerState.activityRef = WeakReference(activity)
}

private fun consumePendingResult(): CompletableDeferred<ImageResult?>? {
    val deferred = ImagePickerState.pendingResult
    ImagePickerState.pendingResult = null
    return deferred
}

private fun queryContentDisplayName(
    context: Context,
    uri: Uri,
): String? {
    if (uri.scheme != "content") return null
    val cursor = context.contentResolver.query(uri, null, null, null, null) ?: return null
    return cursor.use { c ->
        if (!c.moveToFirst()) {
            null
        } else {
            val nameIndex = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex == -1) null else c.getString(nameIndex)
        }
    }
}

private fun resolveGalleryFilename(
    context: Context,
    uri: Uri,
): String {
    var filename = queryContentDisplayName(context, uri)
    if (filename == null) {
        filename = uri.lastPathSegment ?: "image"
    }
    if (!filename.contains('.')) {
        val mimeType = context.contentResolver.getType(uri)
        val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
        if (extension != null) {
            filename = "$filename.$extension"
        }
    }
    return filename
}

private fun readImageBytes(
    context: Context,
    uri: Uri,
): ByteArray? = context.contentResolver.openInputStream(uri)?.use { input -> input.readBytes() }

private fun handleGalleryResult(uri: Uri?) {
    Log.d(TAG, "Gallery result: $uri")
    val deferred = consumePendingResult()
    val ctx = ImagePickerState.activity
    if (uri != null && ctx != null) {
        try {
            val bytes = readImageBytes(ctx, uri)
            deferred?.complete(
                if (bytes != null) {
                    ImageResult(bytes, resolveGalleryFilename(ctx, uri))
                } else {
                    null
                },
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error processing gallery image", e)
            deferred?.complete(null)
        }
    } else {
        deferred?.complete(null)
    }
}

@RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
@Composable
fun ImagePickerLauncher() {
    val launcher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent(),
        ) { uri: Uri? ->
            handleGalleryResult(uri)
        }

    DisposableEffect(Unit) {
        ImagePickerState.launcher = launcher
        onDispose {
            ImagePickerState.launcher = null
        }
    }
}

actual suspend fun pickImage(): ImageResult? {
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

    try {
        launcher.launch("image/*")
    } catch (e: Exception) {
        Log.e(TAG, "Error launching gallery", e)
        ImagePickerState.pendingResult = null
        return null
    }

    return deferred.await()
}
