package com.iiitnr.inventoryapp.di

import com.iiitnr.inventoryapp.data.api.ApiClient
import com.iiitnr.inventoryapp.data.api.AuthApiService
import com.iiitnr.inventoryapp.data.api.ComponentApiService
import com.iiitnr.inventoryapp.data.api.RequestApiService
import com.iiitnr.inventoryapp.data.api.AuditLogApiService
import com.iiitnr.inventoryapp.data.api.UserApiService
import com.iiitnr.inventoryapp.data.api.VersionApiService
import com.iiitnr.inventoryapp.data.storage.createTokenManager
import org.koin.dsl.module

val apiModule = module {
    single { ApiClient.client }
    single { ApiClient.authApiService }
    single { ApiClient.componentApiService }
    single { ApiClient.requestApiService }
    single { ApiClient.auditLogApiService }
    single { ApiClient.userApiService }
    single { ApiClient.versionApiService }
}

val storageModule = module {
    single { createTokenManager() }
}

val appModule = module {
    includes(apiModule, storageModule)
}
