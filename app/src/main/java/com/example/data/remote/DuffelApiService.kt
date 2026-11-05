package com.example.data.remote

import com.example.data.model.DuffelOfferRequestEnvelope
import com.example.data.model.DuffelOfferResponseWrapper
import com.example.data.model.DuffelOrderCancellationCreateEnvelope
import com.example.data.model.DuffelOrderCancellationResponseWrapper
import com.example.data.model.DuffelOrderCreateEnvelope
import com.example.data.model.DuffelOrderResponseWrapper
import com.example.data.model.DuffelPlacesResponseWrapper
import com.example.data.model.DuffelSeatMapsResponseWrapper
import com.example.data.model.DuffelSingleOfferResponseWrapper
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path
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

    /**
     * 1. Re-validação da Oferta (Pre-Checkout) — GET /air/offers/:id
     * Consulta a oferta na Duffel antes de iniciar qualquer transação financeira na Stripe,
     * verificando `expires_at` e `total_amount`.
     */
    @Headers(
        "Duffel-Version: v2",
        "Accept: application/json"
    )
    @GET("air/offers/{offerId}")
    suspend fun getOfferById(
        @Header("Authorization") bearerToken: String,
        @Path("offerId") offerId: String
    ): DuffelSingleOfferResponseWrapper

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
        "Accept: application/json",
        "Content-Type: application/json"
    )
    @POST("air/order_cancellations")
    suspend fun createOrderCancellation(
        @Header("Authorization") bearerToken: String,
        @Body body: DuffelOrderCancellationCreateEnvelope
    ): DuffelOrderCancellationResponseWrapper

    @Headers(
        "Duffel-Version: v2",
        "Accept: application/json",
        "Content-Type: application/json"
    )
    @POST("air/order_cancellations/{cancellationId}/actions/confirm")
    suspend fun confirmOrderCancellation(
        @Header("Authorization") bearerToken: String,
        @Path("cancellationId") cancellationId: String
    ): DuffelOrderCancellationResponseWrapper

    @Headers(
        "Duffel-Version: v2",
        "Accept: application/json"
    )
    @GET("places/suggestions")
    suspend fun getPlaceSuggestions(
        @Header("Authorization") bearerToken: String,
        @Query("query") query: String
    ): DuffelPlacesResponseWrapper

    /**
     * Sincronização em Tempo Real com a API da Duffel (GET /air/seat_maps?offer_id=...)
     * Retorna os assentos da aeronave com status de ocupação e serviços disponíveis.
     */
    @Headers(
        "Duffel-Version: v2",
        "Accept: application/json"
    )
    @GET("air/seat_maps")
    suspend fun getSeatMaps(
        @Header("Authorization") bearerToken: String,
        @Query("offer_id") offerId: String
    ): DuffelSeatMapsResponseWrapper
}

