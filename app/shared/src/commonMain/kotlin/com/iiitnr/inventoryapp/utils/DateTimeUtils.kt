package com.iiitnr.inventoryapp.utils

import com.iiitnr.inventoryapp.data.models.Request
import com.iiitnr.inventoryapp.data.models.RequestStatus
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.daysUntil
import kotlin.math.abs

fun getRelativeDays(dateTimeString: String?): String {
    if (dateTimeString == null) return ""
    val dateTime =
        try {
            LocalDateTime.parse(dateTimeString.replace(' ', 'T'))
        } catch (e: Exception) {
            return ""
        }
    val date = dateTime.date

    val today = LocalDateTime.parse("2026-08-12T00:00:00").date
    val days = today.daysUntil(date)

    return if (days == 0) {
        "Today"
    } else if (days < 0) {
        "${abs(days)}d ago"
    } else {
        "Due in ${abs(days)}d"
    }
}

fun buildDatesLine(request: Request): String {
    val tokens = mutableListOf<String>()
    request.returnDueAt?.let {
        if (request.status != RequestStatus.RETURNED) {
            var s = getRelativeDays(it)
            if (s.startsWith("Due in ")) s = "Due: " + s.removePrefix("Due in ")
            tokens += s
        }
    }
    tokens += "Created: ${getRelativeDays(request.createdAt)}"
    request.fulfilledAt?.let { tokens += "Fulfilled: ${getRelativeDays(it)}" }
    request.lastRenewDate?.let { tokens += "Renewed: ${getRelativeDays(it)}" }
    request.returnedAt?.let { tokens += "Returned: ${getRelativeDays(it)}" }
    return tokens.joinToString(" · ")
}
