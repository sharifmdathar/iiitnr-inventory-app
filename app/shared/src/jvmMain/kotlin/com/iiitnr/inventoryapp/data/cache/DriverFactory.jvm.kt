package com.iiitnr.inventoryapp.data.cache

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.iiitnr.inventoryapp.db.AppDatabase

actual class DriverFactory {
    actual fun createDriver(): SqlDriver {
        val databasePath = getDatabasePath()
        val driver: SqlDriver = JdbcSqliteDriver("jdbc:sqlite:$databasePath")
        try {
            AppDatabase.Schema.create(driver)
        } catch (_: Exception) {
        }
        return driver
    }

    private fun getDatabasePath(): String {
        val osName = System.getProperty("os.name").lowercase()
        val userHome = System.getProperty("user.home")
        val appName = "IIITNRInventoryApp"

        val dir = when {
            osName.contains("win") -> {
                val appData = System.getenv("APPDATA")
                if (appData != null) {
                    java.io.File(appData, appName)
                } else {
                    java.io.File(userHome, "AppData/Roaming/$appName")
                }
            }
            osName.contains("mac") -> {
                java.io.File(userHome, "Library/Application Support/$appName")
            }
            else -> {
                // Linux and others
                val xdgDataHome = System.getenv("XDG_DATA_HOME")
                if (xdgDataHome != null) {
                    java.io.File(xdgDataHome, appName)
                } else {
                    java.io.File(userHome, ".local/share/$appName")
                }
            }
        }

        if (!dir.exists()) {
            dir.mkdirs()
        }

        return java.io.File(dir, "inventory.db").absolutePath
    }
}
