package com.iiitnr.inventoryapp.data.cache

fun createDraftCache(driverFactory: DriverFactory): DraftCache =
    DefaultDraftCache(DatabaseModule.getDatabase(driverFactory))
