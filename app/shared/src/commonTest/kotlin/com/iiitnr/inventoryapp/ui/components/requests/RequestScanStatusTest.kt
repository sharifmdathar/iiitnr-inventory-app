package com.iiitnr.inventoryapp.ui.components.requests

import com.iiitnr.inventoryapp.data.models.RequestStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RequestScanStatusTest {
    @Test
    fun approvedScansToIssued() {
        assertEquals(RequestStatus.ISSUED, nextScannedRequestStatus(RequestStatus.APPROVED))
    }

    @Test
    fun issuedScansToReturned() {
        assertEquals(RequestStatus.RETURNED, nextScannedRequestStatus(RequestStatus.ISSUED))
    }

    @Test
    fun renewedScansToReturned() {
        assertEquals(RequestStatus.RETURNED, nextScannedRequestStatus(RequestStatus.RENEWED))
    }

    @Test
    fun expiredScansToReturned() {
        assertEquals(RequestStatus.RETURNED, nextScannedRequestStatus(RequestStatus.EXPIRED))
    }

    @Test
    fun terminalOrUnactionableStatusesHaveNoNextScanStatus() {
        listOf(
            RequestStatus.PENDING,
            RequestStatus.REJECTED,
            RequestStatus.REQUESTED_RENEW,
            RequestStatus.RETURNED,
        ).forEach { status ->
            assertNull(nextScannedRequestStatus(status), "$status should not have next scan status")
        }
    }
}
