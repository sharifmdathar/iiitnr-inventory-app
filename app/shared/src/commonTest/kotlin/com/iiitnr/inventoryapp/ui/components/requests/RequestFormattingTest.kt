package com.iiitnr.inventoryapp.ui.components.requests

import com.iiitnr.inventoryapp.data.models.RequestItem
import com.iiitnr.inventoryapp.data.models.RequestStatus
import com.iiitnr.inventoryapp.data.models.User
import com.iiitnr.inventoryapp.data.models.UserRole
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
        assertEquals("Renewal Requested", requestStatusDisplayLabel(RequestStatus.REQUESTED_RENEW))
        assertEquals("Renewed", requestStatusDisplayLabel(RequestStatus.RENEWED))
        assertEquals("Issued", requestStatusDisplayLabel(RequestStatus.ISSUED))
        assertEquals("Expired", requestStatusDisplayLabel(RequestStatus.EXPIRED))
    }

    @Test
    fun snackbarMessageMatchesKnownStatusActions() {
        assertEquals("Request approved", requestStatusActionSnackbarMessage(RequestStatus.APPROVED))
        assertEquals("Request rejected", requestStatusActionSnackbarMessage(RequestStatus.REJECTED))
        assertEquals("Request issued", requestStatusActionSnackbarMessage(RequestStatus.ISSUED))
        assertEquals("Request marked returned", requestStatusActionSnackbarMessage(RequestStatus.RETURNED))
        assertEquals("Renewal requested", requestStatusActionSnackbarMessage(RequestStatus.REQUESTED_RENEW))
        assertEquals("Renewal approved", requestStatusActionSnackbarMessage(RequestStatus.RENEWED))
    }

    @Test
    fun snackbarMessageIsNullForStatusesWithoutDirectActionMessage() {
        assertNull(requestStatusActionSnackbarMessage(RequestStatus.PENDING))
        assertNull(requestStatusActionSnackbarMessage(RequestStatus.EXPIRED))
    }

    @Test
    fun compactUserLabelPrefersNameWithBranchAndBatch() {
        val user =
            User(
                id = "user-1",
                email = "madhav24100@iiitnr.edu.in",
                name = "Madhav",
                role = UserRole.STUDENT,
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
                role = UserRole.LA,
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
                role = UserRole.FACULTY,
            )

        assertEquals("Requested from: Name: Dr. Shailesh Khapre", buildUserDetailsLabel("Requested from", user))
    }

    @Test
    fun requestItemQuantitySummaryShowsOnlyRequestedQuantityWhenNoProgress() {
        val item =
            RequestItem(
                id = "item-1",
                quantity = 5,
                fulfilledQuantity = 0,
                returnedQuantity = 0,
            )

        assertEquals("Requested 5", requestItemQuantitySummary(item))
    }

    @Test
    fun requestItemQuantitySummaryLabelsEveryLifecycleQuantityWhenProgressed() {
        val item =
            RequestItem(
                id = "item-1",
                quantity = 5,
                fulfilledQuantity = 3,
                returnedQuantity = 1,
            )

        assertEquals("Requested 5 • Issued 3 • Returned 1", requestItemQuantitySummary(item))
    }
}
