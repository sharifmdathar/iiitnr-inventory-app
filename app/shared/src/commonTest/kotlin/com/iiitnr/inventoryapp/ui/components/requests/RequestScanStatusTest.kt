package com.iiitnr.inventoryapp.ui.components.requests

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RequestScanStatusTest {
    @Test
    fun approvedScansToFulfilled() {
        assertEquals("FULFILLED", nextScannedRequestStatus("APPROVED"))
    }

    @Test
    fun fulfilledScansToReturned() {
        assertEquals("RETURNED", nextScannedRequestStatus("FULFILLED"))
    }

    @Test
    fun renewedScansToReturned() {
        assertEquals("RETURNED", nextScannedRequestStatus("RENEWED"))
    }

    @Test
    fun scanStatusMappingIsCaseInsensitive() {
        assertEquals("RETURNED", nextScannedRequestStatus("renewed"))
    }

    @Test
    fun terminalOrUnactionableStatusesHaveNoNextScanStatus() {
        listOf(
            "PENDING",
            "REJECTED",
            "REQUESTED_RENEW",
            "RETURNED",
            "UNKNOWN",
        ).forEach { status ->
            assertNull(nextScannedRequestStatus(status), "$status should not have next scan status")
        }
    }
}
