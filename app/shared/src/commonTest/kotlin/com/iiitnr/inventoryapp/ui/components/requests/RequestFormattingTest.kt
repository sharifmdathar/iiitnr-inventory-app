package com.iiitnr.inventoryapp.ui.components.requests

import com.iiitnr.inventoryapp.data.models.User
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RequestFormattingTest {
    @Test
    fun displayLabelFormatsUnderscoreStatus() {
        assertEquals("Requested Renew", "REQUESTED_RENEW".toDisplayLabel())
        assertEquals("Pending", "PENDING".toDisplayLabel())
    }

    @Test
    fun requestStatusDisplayLabelUsesProductCopy() {
        assertEquals("Renewal Requested", requestStatusDisplayLabel("REQUESTED_RENEW"))
        assertEquals("Renewed", requestStatusDisplayLabel("RENEWED"))
        assertEquals("Fulfilled", requestStatusDisplayLabel("FULFILLED"))
    }

    @Test
    fun snackbarMessageMatchesKnownStatusActions() {
        assertEquals("Request approved", requestStatusActionSnackbarMessage("APPROVED"))
        assertEquals("Request marked fulfilled", requestStatusActionSnackbarMessage("FULFILLED"))
        assertEquals("Request marked returned", requestStatusActionSnackbarMessage("RETURNED"))
        assertEquals("Renewal requested", requestStatusActionSnackbarMessage("REQUESTED_RENEW"))
        assertEquals("Renewal approved", requestStatusActionSnackbarMessage("RENEWED"))
    }

    @Test
    fun snackbarMessageIsNullForUnknownStatus() {
        assertNull(requestStatusActionSnackbarMessage("UNKNOWN"))
    }

    @Test
    fun compactUserLabelPrefersNameWithBranchAndBatch() {
        val user =
            User(
                id = "user-1",
                email = "madhav24100@iiitnr.edu.in",
                name = "Madhav",
                role = "STUDENT",
                branch = "CSE",
                batch = "2024-2028",
            )

        assertEquals("Madhav (CSE 2024–2028)", compactUserLabel(user))
    }

    @Test
    fun compactUserLabelFallsBackToEmail() {
        val user =
            User(
                id = "user-1",
                email = "ta@iiitnr.edu.in",
                role = "TA",
            )

        assertEquals("ta@iiitnr.edu.in", compactUserLabel(user))
    }

    @Test
    fun userDetailsLabelSkipsMissingDetails() {
        val user =
            User(
                id = "user-1",
                email = "faculty@iiitnr.edu.in",
                name = "Dr. Shailesh Khapre",
                role = "FACULTY",
            )

        assertEquals("Requested from: Name: Dr. Shailesh Khapre", buildUserDetailsLabel("Requested from", user))
    }
}
