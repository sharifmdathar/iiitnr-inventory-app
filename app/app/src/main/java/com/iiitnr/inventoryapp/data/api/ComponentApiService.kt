package com.iiitnr.inventoryapp.data.api

import com.iiitnr.inventoryapp.data.models.ComponentRequest
import com.iiitnr.inventoryapp.data.models.ComponentResponse
import com.iiitnr.inventoryapp.data.models.ComponentsResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface ComponentApiService {
    @GET("components")
    suspend fun getComponents(@Header("Authorization") token: String): Response<ComponentsResponse>

    @POST("components")
    suspend fun createComponent(
        @Header("Authorization") token: String, @Body request: ComponentRequest
    ): Response<ComponentResponse>

    @PUT("components/{id}")
    suspend fun updateComponent(
        @Header("Authorization") token: String,
        @Path("id") id: String,
        @Body request: ComponentRequest
    ): Response<ComponentResponse>

    @DELETE("components/{id}")
    suspend fun deleteComponent(
        @Header("Authorization") token: String, @Path("id") id: String
    ): Response<Unit>
}
