package com.iiitnr.inventoryapp.data.cache

import com.iiitnr.inventoryapp.data.models.Component

internal fun com.iiitnr.inventoryapp.db.Component.toAppComponent(): Component =
    Component(
        id = id,
        name = name,
        description = description,
        imageUrl = imageUrl,
        totalQuantity = totalQuantity.toInt(),
        availableQuantity = availableQuantity.toInt(),
        category = category,
        location = location,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
