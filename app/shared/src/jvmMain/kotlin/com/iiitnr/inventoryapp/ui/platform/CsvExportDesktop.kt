package com.iiitnr.inventoryapp.ui.platform

import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

fun exportComponentsCsvDesktop(
    filename: String,
    content: String,
): Boolean =
    try {
        val chooser =
            JFileChooser().apply {
                dialogTitle = "Save Components CSV"
                selectedFile = File(filename)
                fileFilter = FileNameExtensionFilter("CSV files (*.csv)", "csv")
            }
        val result = chooser.showSaveDialog(null)
        if (result == JFileChooser.APPROVE_OPTION) {
            var file = chooser.selectedFile
            if (!file.name.endsWith(".csv", ignoreCase = true)) {
                file = File(file.absolutePath + ".csv")
            }
            file.writeText(content, Charsets.UTF_8)
            true
        } else {
            false
        }
    } catch (_: Exception) {
        false
    }
