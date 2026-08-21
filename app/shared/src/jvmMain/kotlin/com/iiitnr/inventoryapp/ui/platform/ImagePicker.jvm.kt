package com.iiitnr.inventoryapp.ui.platform

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

actual suspend fun pickImage(): ImageResult? =
    withContext(Dispatchers.IO) {
        val chooser =
            JFileChooser().apply {
                dialogTitle = "Select Component Image"
                fileFilter =
                    FileNameExtensionFilter(
                        "Image files (JPG, PNG, WEBP)",
                        "jpg",
                        "jpeg",
                        "png",
                        "webp",
                        "gif",
                        "avif",
                    )
            }

        val result = chooser.showOpenDialog(null)
        if (result == JFileChooser.APPROVE_OPTION) {
            val file = chooser.selectedFile
            if (file.exists()) {
                ImageResult(
                    bytes = file.readBytes(),
                    filename = file.name,
                )
            } else {
                null
            }
        } else {
            null
        }
    }

actual suspend fun takePhoto(): ImageResult? = null
