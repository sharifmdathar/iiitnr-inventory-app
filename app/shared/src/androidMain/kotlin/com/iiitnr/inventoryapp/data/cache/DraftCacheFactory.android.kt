package com.iiitnr.inventoryapp.data.cache

fun createDraftCache(platformContext: Any?): DraftCache? =
    (platformContext as? android.content.Context)?.let { context ->
        createDraftCache(DriverFactory(context))
    }
