package com.iiitnr.inventoryapp.data.cache

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.iiitnr.inventoryapp.db.AppDatabase

actual class DriverFactory {
    actual fun createDriver(): SqlDriver = NativeSqliteDriver(AppDatabase.Schema, "inventory.db")
}
