package com.iiitnr.inventoryapp.shared

import androidx.compose.ui.window.ComposeUIViewController
import com.iiitnr.inventoryapp.data.storage.createTokenManager

fun mainViewController() =
    ComposeUIViewController(
        configure = {
            enforceStrictPlistSanityCheck = false
        },
    ) {
        val tokenManager = createTokenManager()
        App(tokenManager = tokenManager)
    }
