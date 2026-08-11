package com.iiitnr.inventoryapp.utils

import com.iiitnr.inventoryapp.data.models.RequestStatus

fun String.toDisplayLabel(): String =
    lowercase().split('_').joinToString(" ") { part -> part.replaceFirstChar { it.uppercaseChar() } }

fun requestStatusDisplayLabel(status: RequestStatus): String =
    when (status) {
        RequestStatus.REQUESTED_RENEW -> "Renewal Requested"
        RequestStatus.RENEWED -> "Renewed"
        else -> status.name.toDisplayLabel()
    }
