package com.iiitnr.inventoryapp.data.models

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ModelSerializationTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun requestDecodesWithDefaultOptionalFields() {
        val request =
            json.decodeFromString<Request>(
                """
                {
                  "id": "request-1",
                  "userId": "user-1",
                  "targetFacultyId": "faculty-1",
                  "projectTitle": "Line following car",
                  "status": "APPROVED",
                  "createdAt": "2026-06-01T10:00:00Z",
                  "updatedAt": "2026-06-01T10:00:00Z"
                }
                """.trimIndent(),
            )

        assertEquals(emptyList(), request.items)
        assertNull(request.fulfilledAt)
        assertNull(request.returnedAt)
        assertNull(request.user)
        assertNull(request.targetFaculty)
    }

    @Test
    fun requestDecodesNestedItemsAndComponents() {
        val response =
            json.decodeFromString<RequestResponse>(
                """
                {
                  "request": {
                    "id": "request-1",
                    "userId": "user-1",
                    "targetFacultyId": "faculty-1",
                    "projectTitle": "Line following car",
                    "status": "FULFILLED",
                    "createdAt": "2026-06-01T10:00:00Z",
                    "updatedAt": "2026-06-01T10:00:00Z",
                    "items": [
                      {
                        "id": "item-1",
                        "componentId": "component-1",
                        "quantity": 2,
                        "component": {
                          "id": "component-1",
                          "name": "IR Sensor",
                          "totalQuantity": 10,
                          "availableQuantity": 8,
                          "createdAt": "2026-06-01T10:00:00Z",
                          "updatedAt": "2026-06-01T10:00:00Z"
                        }
                      }
                    ]
                  }
                }
                """.trimIndent(),
            )

        val item = response.request.items.single()
        assertEquals("component-1", item.componentId)
        assertEquals(2, item.quantity)
        assertEquals("IR Sensor", item.component?.name)
    }

    @Test
    fun updateRequestStatusPayloadOmitsNullRenewReason() {
        val encoded = json.encodeToString(UpdateRequestStatusPayload(status = "RETURNED"))

        assertTrue(encoded.contains("\"status\":\"RETURNED\""))
        assertTrue(!encoded.contains("lastRenewReason"))
    }

    @Test
    fun componentCategoryLabelsRemainStableForFilters() {
        assertEquals(
            listOf("Sensors", "Actuators", "Microcontrollers", "Microprocessors", "Others"),
            ComponentCategory.labels,
        )
    }

    @Test
    fun componentLocationLabelsRemainStableForFilters() {
        assertEquals(
            listOf("IoT Lab", "Robo Lab", "VLSI Lab"),
            ComponentLocation.labels,
        )
    }
}
