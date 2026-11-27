package com.iiitnr.inventoryapp.ui.platform

import kotlinx.browser.document
import org.khronos.webgl.ArrayBuffer
import org.w3c.dom.HTMLInputElement
import org.w3c.files.FileReader
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsString
import kotlin.js.js

/** Saves [content] as a CSV file via a Blob download (the web equivalent of a save dialog). */
fun exportCsvWeb(
    filename: String,
    content: String,
): Boolean =
    try {
        downloadCsvFile(filename, content)
        true
    } catch (_: Throwable) {
        false
    }

/** Opens a file picker restricted to CSV files and returns its text via [onResult]. */
fun pickCsvFileWeb(onResult: (String?) -> Unit) {
    val input = document.createElement("input") as HTMLInputElement
    input.type = "file"
    input.accept = ".csv,text/csv,text/plain"
    input.onchange = {
        val file = input.files?.item(0)
        if (file == null) {
            onResult(null)
        } else {
            val reader = FileReader()
            reader.onload = {
                val buffer = reader.result as? ArrayBuffer
                onResult(buffer?.let { decodeUtf8(it).toString() })
            }
            reader.onerror = {
                onResult(null)
            }
            reader.readAsArrayBuffer(file)
        }
    }
    input.click()
}

@OptIn(ExperimentalWasmJsInterop::class)
private fun decodeUtf8(buffer: ArrayBuffer): JsString = js("new TextDecoder('utf-8').decode(buffer)")

@OptIn(ExperimentalWasmJsInterop::class)
private fun downloadCsvFile(
    filename: String,
    content: String,
) {
    js(
        """(function() {
            const blob = new Blob([content], { type: 'text/csv;charset=utf-8' });
            const url = URL.createObjectURL(blob);
            const anchor = document.createElement('a');
            anchor.href = url;
            anchor.download = filename;
            document.body.appendChild(anchor);
            anchor.click();
            anchor.remove();
            window.setTimeout(function() { URL.revokeObjectURL(url); }, 1000);
        })()""",
    )
}
