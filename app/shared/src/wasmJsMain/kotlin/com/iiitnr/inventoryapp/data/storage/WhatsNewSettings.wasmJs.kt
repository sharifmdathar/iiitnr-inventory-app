package com.iiitnr.inventoryapp.data.storage

import kotlinx.browser.window

@Suppress("TooGenericExceptionCaught")
actual fun createWebKeyValueStore(): KeyValueStore? =
    object : KeyValueStore {
        override fun get(key: String): String? = runCatching { window.localStorage.getItem(key) }.getOrNull()

        override fun put(
            key: String,
            value: String,
        ) {
            runCatching { window.localStorage.setItem(key, value) }
        }
    }
