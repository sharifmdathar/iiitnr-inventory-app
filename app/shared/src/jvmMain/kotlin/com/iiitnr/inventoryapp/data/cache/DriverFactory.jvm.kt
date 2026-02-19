package com.iiitnr.inventoryapp.data.cache

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.iiitnr.inventoryapp.db.AppDatabase

actual class DriverFactory {
    actual fun createDriver(): SqlDriver {
        val driver: SqlDriver = JdbcSqliteDriver("jdbc:sqlite:inventory.db")
        try {
            AppDatabase.Schema.create(driver)
        } catch (e: Exception) {
        }
        return driver
    }
}
