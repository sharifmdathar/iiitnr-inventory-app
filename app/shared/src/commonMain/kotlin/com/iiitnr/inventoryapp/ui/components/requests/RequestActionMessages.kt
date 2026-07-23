package com.iiitnr.inventoryapp.ui.components.requests

import com.iiitnr.inventoryapp.data.models.RequestStatus

fun requestStatusActionSnackbarMessage(status: RequestStatus): String? =
    when (status) {
        RequestStatus.APPROVED -> "Request approved"
        RequestStatus.REJECTED -> "Request rejected"
        RequestStatus.ISSUED -> "Request issued"
        RequestStatus.RETURNED -> "Request marked returned"
        RequestStatus.REQUESTED_RENEW -> "Renewal requested"
        RequestStatus.RENEWED -> "Renewal approved"
        else -> null
    }
