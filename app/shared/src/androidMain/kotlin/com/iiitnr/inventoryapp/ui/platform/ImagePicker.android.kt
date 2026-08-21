package com.iiitnr.inventoryapp.ui.platform

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.StrictMode
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.lang.ref.WeakReference

private const val TAG = "ImagePicker"

class TakePictureWithChooser : ActivityResultContract<Uri, Boolean>() {
    @RequiresApi(Build.VERSION_CODES.CUPCAKE)
    override fun createIntent(
        context: Context,
        input: Uri,
    ): Intent {
        val captureIntent =
            Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                .putExtra(MediaStore.EXTRA_OUTPUT, input)
                .addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)

        return Intent
            .createChooser(captureIntent, "Select Camera App")
            .addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    override fun parseResult(
        resultCode: Int,
        intent: Intent?,
    ): Boolean = resultCode == Activity.RESULT_OK
}

internal object ImagePickerState {
    var activityRef: WeakReference<Activity>? = null

    val activity: Activity?
        get() = activityRef?.get()

    var launcher: ActivityResultLauncher<String>? = null
    var cameraLauncher: ActivityResultLauncher<Uri>? = null
    var permissionLauncher: ActivityResultLauncher<String>? = null
    var pendingResult: CompletableDeferred<ImageResult?>? = null
    var tempUri: Uri? = null
    var oldVmPolicy: StrictMode.VmPolicy? = null
}

fun setImagePickerActivity(activity: Activity) {
    ImagePickerState.activityRef = WeakReference(activity)
}

@RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
@Composable
fun ImagePickerLauncher() {
    val launcher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent(),
        ) { uri: Uri? ->
            Log.d(TAG, "Gallery result: $uri")
            val deferred = ImagePickerState.pendingResult
            ImagePickerState.pendingResult = null
            if (uri != null) {
                val ctx = ImagePickerState.activity
                if (ctx != null) {
                    try {
                        ctx.contentResolver.openInputStream(uri)?.use { input ->
                            val bytes = input.readBytes()

                            var filename: String? = null
                            if (uri.scheme == "content") {
                                val cursor = ctx.contentResolver.query(uri, null, null, null, null)
                                cursor.use { cursor ->
                                    if (cursor != null && cursor.moveToFirst()) {
                                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                                        if (nameIndex != -1) {
                                            filename = cursor.getString(nameIndex)
                                        }
                                    }
                                }
                            }

                            if (filename == null) {
                                filename = uri.lastPathSegment ?: "image"
                            }
                            if (!filename.contains('.')) {
                                val mimeType = ctx.contentResolver.getType(uri)
                                val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
                                if (extension != null) {
                                    filename = "$filename.$extension"
                                }
                            }

                            deferred?.complete(ImageResult(bytes, filename))
                        } ?: deferred?.complete(null)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing gallery image", e)
                        deferred?.complete(null)
                    }
                } else {
                    deferred?.complete(null)
                }
            } else {
                deferred?.complete(null)
            }
        }

    val cameraLauncher =
        rememberLauncherForActivityResult(
            contract = TakePictureWithChooser(),
        ) { success ->
            Log.d(TAG, "Camera result: success=$success")

            ImagePickerState.oldVmPolicy?.let {
                StrictMode.setVmPolicy(it)
                ImagePickerState.oldVmPolicy = null
            }

            val deferred = ImagePickerState.pendingResult
            ImagePickerState.pendingResult = null
            val uri = ImagePickerState.tempUri
            ImagePickerState.tempUri = null

            if (success && uri != null) {
                val ctx = ImagePickerState.activity
                if (ctx != null) {
                    try {
                        ctx.contentResolver.openInputStream(uri)?.use { input ->
                            val bytes = input.readBytes()
                            val filename = "camera_capture_${System.currentTimeMillis()}.jpg"
                            deferred?.complete(ImageResult(bytes, filename))
                        } ?: deferred?.complete(null)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing camera image", e)
                        deferred?.complete(null)
                    }
                } else {
                    deferred?.complete(null)
                }
            } else {
                deferred?.complete(null)
            }
        }

    val permissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
        ) { granted ->
            Log.d(TAG, "Permission result: granted=$granted")
        }

    DisposableEffect(Unit) {
        ImagePickerState.launcher = launcher
        ImagePickerState.cameraLauncher = cameraLauncher
        ImagePickerState.permissionLauncher = permissionLauncher
        onDispose {
            ImagePickerState.launcher = null
            ImagePickerState.cameraLauncher = null
            ImagePickerState.permissionLauncher = null
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

@RequiresApi(Build.VERSION_CODES.GINGERBREAD)
actual suspend fun takePhoto(): ImageResult? {
    val activity = ImagePickerState.activity ?: return null

    if (ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
        ImagePickerState.permissionLauncher?.launch(Manifest.permission.CAMERA)
        Toast
            .makeText(
                activity,
                "Camera permission required. Please click again after granting.",
                Toast.LENGTH_LONG,
            ).show()
        return null
    }

    val deferred = CompletableDeferred<ImageResult?>()
    if (ImagePickerState.pendingResult != null) return null
    ImagePickerState.pendingResult = deferred

    val launcher = ImagePickerState.cameraLauncher
    if (launcher == null) {
        ImagePickerState.pendingResult = null
        return null
    }

    try {
        if (ImagePickerState.oldVmPolicy == null) {
            ImagePickerState.oldVmPolicy = StrictMode.getVmPolicy()
        }
        StrictMode.setVmPolicy(StrictMode.VmPolicy.Builder().build())

        val captureDir = activity.externalCacheDir ?: activity.cacheDir
        val tempFile =
            withContext(Dispatchers.IO) {
                File.createTempFile("capture_", ".jpg", captureDir)
            }
        val uri = Uri.fromFile(tempFile)

        ImagePickerState.tempUri = uri
        Log.d(TAG, "Launching camera chooser with URI: $uri")

        launcher.launch(uri)
    } catch (e: Exception) {
        Log.e(TAG, "Error launching camera", e)
        ImagePickerState.pendingResult = null
        ImagePickerState.oldVmPolicy?.let {
            StrictMode.setVmPolicy(it)
            ImagePickerState.oldVmPolicy = null
        }
        Toast.makeText(activity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        return null
    }

    return deferred.await()
}
