package com.iiitnr.inventoryapp.data.cache

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.iiitnr.inventoryapp.db.AppDatabase

actual class DriverFactory(
    private val context: Context,
) {
    actual fun createDriver(): SqlDriver = AndroidSqliteDriver(AppDatabase.Schema, context, "inventory.db")
}
