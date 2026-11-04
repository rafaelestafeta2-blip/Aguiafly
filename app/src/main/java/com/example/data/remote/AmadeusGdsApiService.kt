package com.example.data.remote

import com.squareup.moshi.Json
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Integração oficial com a Amadeus for Developers Self-Service GDS API (https://test.api.amadeus.com / https://api.amadeus.com).
 * A Amadeus é o principal GDS global utilizado pela eDreams ODIGEO e pela TAP Air Portugal (TP),
 * Gol (G3), LATAM (LA), Azul (AD), Iberia (IB), Air Europa (UX), Air France (AF), KLM (KL) e Lufthansa (LH).
 * Conta com cota gratuita mensal no plano Self-Service para busca de ofertas (v2/shopping/flight-offers).
 */
interface AmadeusGdsApiService {

    @FormUrlEncoded
    @POST("v1/security/oauth2/token")
    suspend fun requestAccessToken(
        @Field("grant_type") grantType: String = "client_credentials",
        @Field("client_id") clientId: String,
        @Field("client_secret") clientSecret: String
    ): AmadeusTokenResponseDto

    @GET("v2/shopping/flight-offers")
    suspend fun searchFlightOffers(
        @Header("Authorization") bearerToken: String,
        @Query("originLocationCode") originIata: String,
        @Query("destinationLocationCode") destinationIata: String,
        @Query("departureDate") departureDate: String,
        @Query("adults") adults: Int = 1,
        @Query("travelClass") travelClass: String = "ECONOMY",
        @Query("currencyCode") currencyCode: String = "EUR",
        @Query("max") maxOffers: Int = 35
    ): AmadeusFlightOffersResponseDto
}

data class AmadeusTokenResponseDto(
    @Json(name = "access_token") val accessToken: String = "",
    @Json(name = "token_type") val tokenType: String = "Bearer",
    @Json(name = "expires_in") val expiresIn: Int = 1799
)

data class AmadeusFlightOffersResponseDto(
    @Json(name = "data") val data: List<AmadeusFlightOfferDto> = emptyList(),
    @Json(name = "dictionaries") val dictionaries: AmadeusDictionariesDto? = null
)

data class AmadeusDictionariesDto(
    @Json(name = "carriers") val carriers: Map<String, String> = emptyMap(),
    @Json(name = "aircraft") val aircraft: Map<String, String> = emptyMap()
)

data class AmadeusFlightOfferDto(
    @Json(name = "id") val id: String = "",
    @Json(name = "validatingAirlineCodes") val validatingAirlineCodes: List<String> = emptyList(),
    @Json(name = "price") val price: AmadeusPriceDto = AmadeusPriceDto(),
    @Json(name = "itineraries") val itineraries: List<AmadeusItineraryDto> = emptyList()
)

data class AmadeusPriceDto(
    @Json(name = "currency") val currency: String = "EUR",
    @Json(name = "total") val total: String = "0.00",
    @Json(name = "grandTotal") val grandTotal: String? = null
)

data class AmadeusItineraryDto(
    @Json(name = "duration") val duration: String? = null,
    @Json(name = "segments") val segments: List<AmadeusSegmentDto> = emptyList()
)

data class AmadeusSegmentDto(
    @Json(name = "departure") val departure: AmadeusEndpointDto = AmadeusEndpointDto(),
    @Json(name = "arrival") val arrival: AmadeusEndpointDto = AmadeusEndpointDto(),
    @Json(name = "carrierCode") val carrierCode: String = "TP",
    @Json(name = "number") val number: String = "35",
    @Json(name = "aircraft") val aircraft: AmadeusAircraftDto? = null,
    @Json(name = "duration") val duration: String? = null
)

data class AmadeusEndpointDto(
    @Json(name = "iataCode") val iataCode: String = "",
    @Json(name = "terminal") val terminal: String? = null,
    @Json(name = "at") val at: String = ""
)

data class AmadeusAircraftDto(
    @Json(name = "code") val code: String = "339"
)
