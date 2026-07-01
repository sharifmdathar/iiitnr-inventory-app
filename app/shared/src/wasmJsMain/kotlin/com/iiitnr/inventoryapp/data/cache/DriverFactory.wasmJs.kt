package com.iiitnr.inventoryapp.data.cache

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.worker.WebWorkerDriver
import org.w3c.dom.Worker

actual class DriverFactory {
    actual fun createDriver(): SqlDriver =
        WebWorkerDriver(
            Worker("sqldelight-worker.js"),
        )
}
