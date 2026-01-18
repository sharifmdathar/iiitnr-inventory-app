package com.iiitnr.inventoryapp.data.api

import com.iiitnr.inventoryapp.data.models.CreateRequestPayload
import com.iiitnr.inventoryapp.data.models.RequestResponse
import com.iiitnr.inventoryapp.data.models.RequestsResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface RequestApiService {
    @POST("requests")
    suspend fun createRequest(
        @Header("Authorization") token: String,
        @Body payload: CreateRequestPayload
    ): Response<RequestResponse>

    @GET("requests")
    suspend fun getRequests(
        @Header("Authorization") token: String,
        @Query("status") status: String? = null
    ): Response<RequestsResponse>
}
