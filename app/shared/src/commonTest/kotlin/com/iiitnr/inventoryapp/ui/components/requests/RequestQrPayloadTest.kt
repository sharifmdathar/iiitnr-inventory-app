package com.iiitnr.inventoryapp.ui.components.requests

import kotlin.test.Test
import kotlin.test.assertEquals

class RequestQrPayloadTest {
    @Test
    fun qrPrefixMatchesScannerParsingContract() {
        val requestId = "request-123"
        val payload = REQUEST_QR_PREFIX + requestId

        assertEquals("inventoryapp://fulfill/request-123", payload)
        assertEquals(requestId, payload.removePrefix(REQUEST_QR_PREFIX))
    }
}
