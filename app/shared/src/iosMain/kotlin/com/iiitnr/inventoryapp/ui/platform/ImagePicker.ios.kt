package com.iiitnr.inventoryapp.ui.platform

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSData
import platform.Foundation.NSLog
import platform.UIKit.UIApplication
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIImagePickerController
import platform.UIKit.UIImagePickerControllerDelegateProtocol
import platform.UIKit.UIImagePickerControllerEditedImage
import platform.UIKit.UIImagePickerControllerOriginalImage
import platform.UIKit.UIImagePickerControllerSourceTypePhotoLibrary
import platform.UIKit.UINavigationControllerDelegateProtocol
import platform.UIKit.UIWindow
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.posix.memcpy
import kotlin.coroutines.resume

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

private object ImagePickerRuntime {
	var picker: UIImagePickerController? = null
	var delegate: ImagePickerDelegate? = null

	fun clear() {
		picker = null
		delegate = null
	}
}

private class ImagePickerDelegate(
	private val onResult: (ImageResult?) -> Unit,
) : NSObject(), UIImagePickerControllerDelegateProtocol, UINavigationControllerDelegateProtocol {
	override fun imagePickerController(
		picker: UIImagePickerController,
		didFinishPickingMediaWithInfo: Map<Any?, *>,
	) {
		val selectedImage =
			didFinishPickingMediaWithInfo[UIImagePickerControllerOriginalImage] as? UIImage
				?: didFinishPickingMediaWithInfo[UIImagePickerControllerEditedImage] as? UIImage

		val imageData: NSData? = selectedImage?.let { UIImageJPEGRepresentation(it, 0.9) }
		val imageBytes = imageData?.toByteArray()
		val filename = "image.jpg"

		picker.dismissViewControllerAnimated(true, completion = null)
		ImagePickerRuntime.clear()
		onResult(
			if (imageBytes != null) {
				ImageResult(imageBytes, filename)
			} else {
				null
			},
		)
	}

	override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
		picker.dismissViewControllerAnimated(true, completion = null)
		ImagePickerRuntime.clear()
		onResult(null)
	}
}

private fun topViewController(): platform.UIKit.UIViewController? {
	val window =
		UIApplication.sharedApplication.windows.firstOrNull { (it as? UIWindow)?.isKeyWindow == true }
			?: UIApplication.sharedApplication.keyWindow
	var controller = window?.rootViewController

	while (controller?.presentedViewController != null) {
		controller = controller?.presentedViewController
	}

	return controller
}

@OptIn(ExperimentalForeignApi::class)
actual suspend fun pickImage(): ImageResult? =
	suspendCancellableCoroutine { continuation ->
		dispatch_async(dispatch_get_main_queue()) {
			val presenter = topViewController()
			if (presenter == null) {
				NSLog("Image picker unavailable: no presenting view controller")
				continuation.resume(null)
				return@dispatch_async
			}

			if (ImagePickerRuntime.picker != null) {
				continuation.resume(null)
				return@dispatch_async
			}

			val picker = UIImagePickerController().apply {
				sourceType = UIImagePickerControllerSourceTypePhotoLibrary
			}
			val delegate = ImagePickerDelegate { result ->
				continuation.resume(result)
			}

			ImagePickerRuntime.picker = picker
			ImagePickerRuntime.delegate = delegate
			picker.delegate = delegate

			continuation.invokeOnCancellation {
				dispatch_async(dispatch_get_main_queue()) {
					picker.dismissViewControllerAnimated(true, completion = null)
					ImagePickerRuntime.clear()
				}
			}

			presenter.presentViewController(picker, animated = true, completion = null)
		}
	}
