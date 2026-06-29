package com.iiitnr.inventoryapp.ui.components.common

import kotlin.test.Test
import kotlin.test.assertEquals

class StatusLabelTest {
    @Test
    fun statusLabelFormatsKnownRequestStatuses() {
        assertEquals("Pending", requestStatusLabel("PENDING"))
        assertEquals("Approved", requestStatusLabel("APPROVED"))
        assertEquals("Fulfilled", requestStatusLabel("FULFILLED"))
        assertEquals("Returned", requestStatusLabel("RETURNED"))
        assertEquals("Renewed", requestStatusLabel("RENEWED"))
        assertEquals("Expired", requestStatusLabel("EXPIRED"))
    }

    @Test
    fun statusLabelUsesSpecialRenewalCopy() {
        assertEquals("Renewal Requested", requestStatusLabel("REQUESTED_RENEW"))
    }

    @Test
    fun statusLabelIsCaseInsensitive() {
        assertEquals("Fulfilled", requestStatusLabel("fulfilled"))
        assertEquals("Renewal Requested", requestStatusLabel("requested_renew"))
    }
}
