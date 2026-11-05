package com.example.data.remote

import com.squareup.moshi.Json
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

data class BackendIdentityDocumentDto(
    @Json(name = "type") val type: String = "passport",
    @Json(name = "unique_identifier") val uniqueIdentifier: String,
    @Json(name = "expires_on") val expiresOn: String,
    @Json(name = "issuing_country_code") val issuingCountryCode: String
)

data class BackendRawPassengerDto(
    @Json(name = "id") val id: String,
    @Json(name = "given_name") val givenName: String,
    @Json(name = "family_name") val familyName: String,
    @Json(name = "born_on") val bornOn: String,
    @Json(name = "gender") val gender: String,
    @Json(name = "title") val title: String,
    @Json(name = "phone_number") val phoneNumber: String,
    @Json(name = "email") val email: String,
    @Json(name = "identity_documents") val identityDocuments: List<BackendIdentityDocumentDto>? = null
)

data class BackendItineraryDto(
    @Json(name = "airlineName") val airlineName: String,
    @Json(name = "airlineIata") val airlineIata: String,
    @Json(name = "flightNumber") val flightNumber: String,
    @Json(name = "originIata") val originIata: String,
    @Json(name = "originCity") val originCity: String,
    @Json(name = "destinationIata") val destinationIata: String,
    @Json(name = "destinationCity") val destinationCity: String,
    @Json(name = "departureIso") val departureIso: String,
    @Json(name = "arrivalIso") val arrivalIso: String,
    @Json(name = "cabinClass") val cabinClass: String,
    @Json(name = "assignedSeat") val assignedSeat: String
)

data class BackendAtomicCheckoutRequest(
    @Json(name = "userId") val userId: String,
    @Json(name = "offerId") val offerId: String,
    @Json(name = "expectedNetAmountEur") val expectedNetAmountEur: Double,
    @Json(name = "ancillariesAmountEur") val ancillariesAmountEur: Double,
    @Json(name = "paymentMethodOrCardToken") val paymentMethodOrCardToken: String,
    @Json(name = "passengers") val passengers: List<BackendRawPassengerDto>,
    @Json(name = "itinerary") val itinerary: BackendItineraryDto,
    @Json(name = "autoCheckInEnabled") val autoCheckInEnabled: Boolean = true
)

data class BackendTripBookingSummaryDto(
    @Json(name = "bookingId") val bookingId: String = "",
    @Json(name = "status") val status: String = "confirmed",
    @Json(name = "statusBadgeLabel") val statusBadgeLabel: String = "",
    @Json(name = "pnrLocator") val pnrLocator: String = "",
    @Json(name = "duffelOrderId") val duffelOrderId: String = "",
    @Json(name = "passengerFullNameIata") val passengerFullNameIata: String = "",
    @Json(name = "eTicketNumbers") val eTicketNumbers: List<String> = emptyList(),
    @Json(name = "confirmationPdfUrl") val confirmationPdfUrl: String = "",
    @Json(name = "iataBcbpPayload") val iataBcbpPayload: String = "",
    @Json(name = "totalPaidEur") val totalPaidEur: Double = 0.0
)

data class BackendPriceChangeDetailsDto(
    @Json(name = "offerId") val offerId: String? = null,
    @Json(name = "previousNetAmountEur") val previousNetAmountEur: Double? = null,
    @Json(name = "currentNetAmountEur") val currentNetAmountEur: Double? = null,
    @Json(name = "previousGrandTotalEur") val previousGrandTotalEur: Double? = null,
    @Json(name = "currentGrandTotalEur") val currentGrandTotalEur: Double? = null,
    @Json(name = "currency") val currency: String? = "EUR",
    @Json(name = "duffelErrorCode") val duffelErrorCode: String? = null,
    @Json(name = "field") val field: String? = null,
    @Json(name = "exactDuffelMessage") val exactDuffelMessage: String? = null
)

data class BackendAtomicCheckoutResponse(
    @Json(name = "success") val success: Boolean = false,
    @Json(name = "code") val code: String? = null,
    @Json(name = "errorCode") val errorCode: String? = null,
    @Json(name = "message") val message: String? = null,
    @Json(name = "error") val error: String? = null,
    @Json(name = "field") val field: String? = null,
    @Json(name = "details") val details: BackendPriceChangeDetailsDto? = null,
    @Json(name = "booking") val booking: BackendTripBookingSummaryDto? = null
)

interface AguiaFlyBackendMiddlewareService {

    @POST("api/flights/checkout/atomic-issue")
    suspend fun executeAtomicCheckout(
        @Body request: BackendAtomicCheckoutRequest
    ): BackendAtomicCheckoutResponse

    @GET("api/flights/trips/{userId}")
    suspend fun getUserTrips(
        @Path("userId") userId: String
    ): Map<String, Any>
}
