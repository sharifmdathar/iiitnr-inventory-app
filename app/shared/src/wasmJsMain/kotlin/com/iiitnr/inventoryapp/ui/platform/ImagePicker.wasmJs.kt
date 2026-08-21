package com.iiitnr.inventoryapp.ui.platform

import kotlinx.browser.document
import kotlinx.coroutines.suspendCancellableCoroutine
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array
import org.khronos.webgl.toByteArray
import org.w3c.dom.HTMLInputElement
import org.w3c.files.FileReader
import kotlin.coroutines.resume

@OptIn(ExperimentalWasmJsInterop::class)
actual suspend fun pickImage(): ImageResult? =
    suspendCancellableCoroutine { continuation ->
        val input = document.createElement("input") as HTMLInputElement
        input.type = "file"
        input.accept = "image/*"
        input.onchange = {
            val file = input.files?.item(0)
            if (file == null) {
                continuation.resume(null)
            } else {
                val reader = FileReader()
                reader.onload = {
                    val buffer = reader.result as? ArrayBuffer
                    if (buffer == null) {
                        continuation.resume(null)
                    } else {
                        continuation.resume(ImageResult(buffer.toByteArray(), file.name))
                    }
                }
                reader.onerror = {
                    continuation.resume(null)
                }
                reader.readAsArrayBuffer(file)
            }
        }
        input.click()
    }

actual suspend fun takePhoto(): ImageResult? = null

private fun ArrayBuffer.toByteArray(): ByteArray = Int8Array(this, 0, this.byteLength).toByteArray()
