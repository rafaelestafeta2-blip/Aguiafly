package com.example.data.remote

import com.squareup.moshi.Json
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

/**
 * Integração oficial com a Kiwi.com Tequila API (https://api.tequila.kiwi.com).
 * Agrega mais de 750 companhias aéreas globais (GDS tradicionais + NDC + Low-Cost)
 * incluindo TAP Air Portugal (TP), Gol (G3), LATAM (LA), Azul (AD), Air Europa (UX),
 * Iberia (IB), Vueling (VY), Ryanair (FR), easyJet (U2), Air France (AF), KLM (KL),
 * British Airways (BA) e Lufthansa (LH).
 */
interface KiwiTequilaApiService {

    @GET("v2/search")
    suspend fun searchFlights(
        @Header("apikey") apiKey: String,
        @Query("fly_from") flyFrom: String,
        @Query("fly_to") flyTo: String,
        @Query("date_from") dateFromDdMmYyyy: String,
        @Query("date_to") dateToDdMmYyyy: String,
        @Query("adults") adults: Int = 1,
        @Query("selected_cabins") selectedCabins: String = "M",
        @Query("curr") currency: String = "EUR",
        @Query("limit") limit: Int = 40,
        @Query("sort") sort: String = "price"
    ): KiwiTequilaSearchResponseDto

    @GET("locations/query")
    suspend fun searchLocations(
        @Header("apikey") apiKey: String,
        @Query("term") term: String,
        @Query("location_types") locationTypes: String = "airport",
        @Query("limit") limit: Int = 8
    ): KiwiLocationsResponseDto
}

data class KiwiTequilaSearchResponseDto(
    @Json(name = "currency") val currency: String = "EUR",
    @Json(name = "data") val data: List<KiwiTequilaFlightDto> = emptyList()
)

data class KiwiTequilaFlightDto(
    @Json(name = "id") val id: String = "",
    @Json(name = "flyFrom") val flyFrom: String = "",
    @Json(name = "flyTo") val flyTo: String = "",
    @Json(name = "cityFrom") val cityFrom: String = "",
    @Json(name = "cityTo") val cityTo: String = "",
    @Json(name = "local_departure") val localDeparture: String = "",
    @Json(name = "local_arrival") val localArrival: String = "",
    @Json(name = "duration") val duration: KiwiDurationDto? = null,
    @Json(name = "price") val price: Double = 0.0,
    @Json(name = "airlines") val airlines: List<String> = emptyList(),
    @Json(name = "route") val route: List<KiwiRouteSegmentDto> = emptyList(),
    @Json(name = "availability") val availability: KiwiAvailabilityDto? = null
)

data class KiwiDurationDto(
    @Json(name = "departure") val departureSeconds: Int = 0,
    @Json(name = "return") val returnSeconds: Int = 0,
    @Json(name = "total") val totalSeconds: Int = 0
)

data class KiwiRouteSegmentDto(
    @Json(name = "id") val id: String = "",
    @Json(name = "flyFrom") val flyFrom: String = "",
    @Json(name = "flyTo") val flyTo: String = "",
    @Json(name = "cityFrom") val cityFrom: String = "",
    @Json(name = "cityTo") val cityTo: String = "",
    @Json(name = "airline") val airline: String = "TP",
    @Json(name = "flight_no") val flightNo: Int = 35,
    @Json(name = "operating_carrier") val operatingCarrier: String? = null,
    @Json(name = "equipment") val equipment: String? = null,
    @Json(name = "local_departure") val localDeparture: String = "",
    @Json(name = "local_arrival") val localArrival: String = ""
)

data class KiwiAvailabilityDto(
    @Json(name = "seats") val seats: Int? = null
)

data class KiwiLocationsResponseDto(
    @Json(name = "locations") val locations: List<KiwiLocationDto> = emptyList()
)

data class KiwiLocationDto(
    @Json(name = "id") val id: String = "",
    @Json(name = "code") val code: String = "",
    @Json(name = "name") val name: String = "",
    @Json(name = "city") val city: KiwiCityDto? = null
)

data class KiwiCityDto(
    @Json(name = "name") val name: String = "",
    @Json(name = "country") val country: KiwiCountryDto? = null
)

data class KiwiCountryDto(
    @Json(name = "name") val name: String = "",
    @Json(name = "code") val code: String = ""
)
