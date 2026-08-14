package com.iiitnr.inventoryapp.di

import com.iiitnr.inventoryapp.data.storage.createTokenManager
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module =
    module {
        single { createTokenManager() }
        // Add components cache if needed for wasm
    }
