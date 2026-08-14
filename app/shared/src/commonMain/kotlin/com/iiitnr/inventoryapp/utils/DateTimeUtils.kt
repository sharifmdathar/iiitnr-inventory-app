package com.iiitnr.inventoryapp.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import com.iiitnr.inventoryapp.data.models.Request
import com.iiitnr.inventoryapp.data.models.RequestStatus
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlin.math.abs

val LocalToday =
    staticCompositionLocalOf<LocalDate> {
        LocalDateTime.parse("2026-08-12T00:00:00").date
    }

val currentToday: LocalDate
    @Composable @ReadOnlyComposable
    get() = LocalToday.current

fun getRelativeDays(
    dateTimeString: String?,
    today: LocalDate,
): String {
    if (dateTimeString == null) return ""
    val dateTime =
        try {
            LocalDateTime.parse(dateTimeString.replace(' ', 'T'))
        } catch (e: Exception) {
            return ""
        }
    val date = dateTime.date

    val days = (date.toEpochDays() - today.toEpochDays())

    return if (days == 0L) {
        "Today"
    } else if (days < 0L) {
        "${abs(days)}d ago"
    } else {
        "Due in ${abs(days)}d"
    }
}

fun buildDatesLine(
    request: Request,
    today: LocalDate,
): String {
    val tokens = mutableListOf<String>()
    request.returnDueAt?.let {
        if (request.status != RequestStatus.RETURNED) {
            var s = getRelativeDays(it, today)
            if (s.startsWith("Due in ")) s = "Due: " + s.removePrefix("Due in ")
            tokens += s
        }
    }
    tokens += "Created: ${getRelativeDays(request.createdAt, today)}"
    request.fulfilledAt?.let { tokens += "Fulfilled: ${getRelativeDays(it, today)}" }
    request.lastRenewDate?.let { tokens += "Renewed: ${getRelativeDays(it, today)}" }
    request.returnedAt?.let { tokens += "Returned: ${getRelativeDays(it, today)}" }
    return tokens.joinToString(" · ")
}
