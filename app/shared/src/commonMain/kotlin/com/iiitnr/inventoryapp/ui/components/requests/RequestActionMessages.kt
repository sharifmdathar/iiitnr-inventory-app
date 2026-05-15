package com.iiitnr.inventoryapp.ui.components.requests

fun requestStatusActionSnackbarMessage(status: String): String? =
    when (status) {
        "APPROVED" -> "Request approved"
        "REJECTED" -> "Request rejected"
        "FULFILLED" -> "Request marked fulfilled"
        "RETURNED" -> "Request marked returned"
        "REQUESTED_RENEW" -> "Renewal requested"
        "RENEWED" -> "Renewal approved"
        else -> null
    }
