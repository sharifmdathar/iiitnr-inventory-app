package com.iiitnr.inventoryapp.shared

import androidx.compose.ui.window.ComposeUIViewController
import com.iiitnr.inventoryapp.data.storage.createTokenManager
import com.iiitnr.inventoryapp.ui.theme.AppTheme

fun mainViewController() =
    ComposeUIViewController(
        configure = {
            enforceStrictPlistSanityCheck = false
        },
    ) {
        val tokenManager = createTokenManager()
        AppTheme {
            App(tokenManager = tokenManager)
        }
    }
