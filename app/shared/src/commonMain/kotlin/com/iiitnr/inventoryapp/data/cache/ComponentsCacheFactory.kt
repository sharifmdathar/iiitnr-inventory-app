package com.iiitnr.inventoryapp.data.cache

fun createComponentsCache(driverFactory: DriverFactory): ComponentsCache =
    DefaultComponentsCache(DatabaseModule.getDatabase(driverFactory))
