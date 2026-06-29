package com.iiitnr.inventoryapp.ui.components.requests

internal fun nextScannedRequestStatus(status: String): String? =
    when (status.uppercase()) {
        "APPROVED" -> "FULFILLED"
        "FULFILLED", "RENEWED", "EXPIRED" -> "RETURNED"
        else -> null
    }
