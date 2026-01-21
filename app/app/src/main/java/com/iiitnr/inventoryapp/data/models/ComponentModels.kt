package com.iiitnr.inventoryapp.data.models

enum class ComponentCategory(val label: String) {
    SENSORS("Sensors"), ACTUATORS("Actuators"), MICROCONTROLLERS("Microcontrollers"), MICROPROCESSORS(
        "Microprocessors"
    ),
    OTHERS("Others");

    companion object {
        val labels: List<String> = entries.map { it.label }
    }
}

enum class ComponentLocation(val label: String) {
    IOT_LAB("IoT Lab"), ROBO_LAB("Robo Lab"), VLSI_LAB("VLSI Lab");

    companion object {
        val labels: List<String> = entries.map { it.label }
    }
}

data class Component(
    val id: String,
    val name: String,
    val description: String?,
    val quantity: Int,
    val category: String?,
    val location: String?,
    val createdAt: String,
    val updatedAt: String
)

data class ComponentRequest(
    val name: String,
    val description: String? = null,
    val quantity: Int = 0,
    val category: String? = null,
    val location: String? = null
)

data class ComponentsResponse(
    val components: List<Component>
)

data class ComponentResponse(
    val component: Component
)
