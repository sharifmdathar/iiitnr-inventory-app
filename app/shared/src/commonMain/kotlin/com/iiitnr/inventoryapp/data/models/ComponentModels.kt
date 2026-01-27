package com.iiitnr.inventoryapp.data.models

import kotlinx.serialization.Serializable

enum class ComponentCategory(val label: String) {
    SENSORS("Sensors"),
    ACTUATORS("Actuators"),
    MICROCONTROLLERS("Microcontrollers"),
    MICROPROCESSORS("Microprocessors"),
    OTHERS("Others");

    companion object {
        val labels: List<String> = entries.map { it.label }
    }
}

enum class ComponentLocation(val label: String) {
    IOT_LAB("IoT Lab"),
    ROBO_LAB("Robo Lab"),
    VLSI_LAB("VLSI Lab");

    companion object {
        val labels: List<String> = entries.map { it.label }
    }
}

@Serializable
data class Component(
    val id: String,
    val name: String,
    val description: String? = null,
    val totalQuantity: Int,
    val availableQuantity: Int,
    val category: String? = null,
    val location: String? = null,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class ComponentRequest(
    val name: String,
    val description: String? = null,
    val totalQuantity: Int = 0,
    val availableQuantity: Int? = null,
    val category: String? = null,
    val location: String? = null
)

@Serializable
data class ComponentsResponse(
    val components: List<Component>
)

@Serializable
data class ComponentResponse(
    val component: Component
)
