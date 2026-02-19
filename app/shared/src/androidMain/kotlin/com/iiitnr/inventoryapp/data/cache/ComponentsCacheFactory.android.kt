package com.iiitnr.inventoryapp.data.cache

fun createComponentsCache(platformContext: Any?): ComponentsCache? =
    (platformContext as? android.content.Context)?.let { context ->
        createComponentsCache(DriverFactory(context))
    }
