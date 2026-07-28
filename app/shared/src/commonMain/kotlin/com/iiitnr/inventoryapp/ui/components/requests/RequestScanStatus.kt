package com.iiitnr.inventoryapp.ui.components.requests

import com.iiitnr.inventoryapp.data.models.RequestStatus

internal fun nextScannedRequestStatus(status: RequestStatus): RequestStatus? =
    when (status) {
        RequestStatus.APPROVED -> RequestStatus.ISSUED
        RequestStatus.ISSUED, RequestStatus.RENEWED, RequestStatus.EXPIRED,
        RequestStatus.PARTIALLY_ISSUED, RequestStatus.PARTIALLY_RETURNED,
        -> RequestStatus.RETURNED
        else -> null
    }
