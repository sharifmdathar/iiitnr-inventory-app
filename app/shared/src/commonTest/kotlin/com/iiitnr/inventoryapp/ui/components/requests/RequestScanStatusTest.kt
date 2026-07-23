package com.iiitnr.inventoryapp.ui.components.requests

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RequestScanStatusTest {
    @Test
    fun approvedScansToIssued() {
        assertEquals("ISSUED", nextScannedRequestStatus("APPROVED"))
    }

    @Test
    fun issuedScansToReturned() {
        assertEquals("RETURNED", nextScannedRequestStatus("ISSUED"))
    }

    @Test
    fun renewedScansToReturned() {
        assertEquals("RETURNED", nextScannedRequestStatus("RENEWED"))
    }

    @Test
    fun expiredScansToReturned() {
        assertEquals("RETURNED", nextScannedRequestStatus("EXPIRED"))
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
