package com.iiitnr.inventoryapp.data.cache

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.iiitnr.inventoryapp.db.AppDatabase

actual class DriverFactory {
    actual fun createDriver(): SqlDriver {
        val databasePath = getDatabasePath()
        val driver: SqlDriver = JdbcSqliteDriver("jdbc:sqlite:$databasePath")
        migrateSchema(driver)
        return driver
    }

    private fun getDatabasePath(): String {
        val osName = System.getProperty("os.name").lowercase()
        val userHome = System.getProperty("user.home")
        val appName = "IIITNRInventoryApp"

        val dir =
            when {
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

/**
 * The JDBC driver has no schema callback of its own, so the schema version is
 * tracked in `PRAGMA user_version` and the migrations are applied here.
 */
internal fun migrateSchema(driver: SqlDriver) {
    val schema = AppDatabase.Schema
    val currentVersion = readUserVersion(driver)

    if (currentVersion == 0L) {
        // Either a brand new database, or one created before the version was
        // tracked — in which case the tables already exist.
        val created = runCatching { schema.create(driver).value }.isSuccess
        if (!created) {
            schema.migrate(driver, 1L, schema.version).value
        }
    } else if (currentVersion < schema.version) {
        schema.migrate(driver, currentVersion, schema.version).value
    }

    writeUserVersion(driver, schema.version)
}

private fun readUserVersion(driver: SqlDriver): Long =
    driver
        .executeQuery(
            identifier = null,
            sql = "PRAGMA user_version",
            mapper = { cursor ->
                val version = if (cursor.next().value) cursor.getLong(0) ?: 0L else 0L
                QueryResult.Value(version)
            },
            parameters = 0,
        ).value

private fun writeUserVersion(
    driver: SqlDriver,
    version: Long,
) {
    driver.execute(null, "PRAGMA user_version = $version", 0)
}
