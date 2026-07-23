package com.iiitnr.inventoryapp.ui.components.common

import com.iiitnr.inventoryapp.data.models.RequestStatus
import kotlin.test.Test
import kotlin.test.assertEquals

class StatusLabelTest {
    @Test
    fun statusLabelFormatsKnownRequestStatuses() {
        assertEquals("Pending", requestStatusLabel(RequestStatus.PENDING))
        assertEquals("Approved", requestStatusLabel(RequestStatus.APPROVED))
        assertEquals("Issued", requestStatusLabel(RequestStatus.ISSUED))
        assertEquals("Returned", requestStatusLabel(RequestStatus.RETURNED))
        assertEquals("Renewed", requestStatusLabel(RequestStatus.RENEWED))
        assertEquals("Expired", requestStatusLabel(RequestStatus.EXPIRED))
    }

    @Test
    fun statusLabelUsesSpecialRenewalCopy() {
        assertEquals("Renewal Requested", requestStatusLabel(RequestStatus.REQUESTED_RENEW))
    }
}
