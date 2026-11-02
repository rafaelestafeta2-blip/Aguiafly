package com.example.data.remote

import com.example.data.model.DuffelOfferRequestEnvelope
import com.example.data.model.DuffelOfferResponseWrapper
import com.example.data.model.DuffelOrderCreateEnvelope
import com.example.data.model.DuffelOrderResponseWrapper
import com.example.data.model.DuffelPlacesResponseWrapper
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query

interface DuffelApiService {

    @Headers(
        "Duffel-Version: v2",
        "Accept: application/json",
        "Content-Type: application/json"
    )
    @POST("air/offer_requests")
    suspend fun createOfferRequest(
        @Header("Authorization") bearerToken: String,
        @Query("return_offers") returnOffers: Boolean = true,
        @Body body: DuffelOfferRequestEnvelope
    ): DuffelOfferResponseWrapper

    @Headers(
        "Duffel-Version: v2",
        "Accept: application/json",
        "Content-Type: application/json"
    )
    @POST("air/orders")
    suspend fun createInstantOrder(
        @Header("Authorization") bearerToken: String,
        @Body body: DuffelOrderCreateEnvelope
    ): DuffelOrderResponseWrapper

    @Headers(
        "Duffel-Version: v2",
        "Accept: application/json"
    )
    @GET("places/suggestions")
    suspend fun getPlaceSuggestions(
        @Header("Authorization") bearerToken: String,
        @Query("query") query: String
    ): DuffelPlacesResponseWrapper
}
