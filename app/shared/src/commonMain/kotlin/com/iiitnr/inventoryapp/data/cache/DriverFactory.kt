package com.iiitnr.inventoryapp.data.cache

import app.cash.sqldelight.db.SqlDriver
import com.iiitnr.inventoryapp.db.AppDatabase

expect class DriverFactory {
    fun createDriver(): SqlDriver
}

fun createDatabase(factory: DriverFactory): AppDatabase {
    val driver = factory.createDriver()
    return AppDatabase(driver)
}

object DatabaseModule {
    private var database: AppDatabase? = null

    fun getDatabase(factory: DriverFactory): AppDatabase {
        if (database == null) {
            database = createDatabase(factory)
        }
        return database!!
    }
}
