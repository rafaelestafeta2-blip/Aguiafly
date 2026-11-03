package com.example.data.model

import com.squareup.moshi.Json

data class DuffelOfferRequestEnvelope(
    @Json(name = "data") val data: DuffelOfferRequestPayload
)

data class DuffelOfferRequestPayload(
    @Json(name = "slices") val slices: List<DuffelSliceRequest>,
    @Json(name = "passengers") val passengers: List<DuffelPassengerRequest>,
    @Json(name = "cabin_class") val cabinClass: String
)

data class DuffelSliceRequest(
    @Json(name = "origin") val origin: String,
    @Json(name = "destination") val destination: String,
    @Json(name = "departure_date") val departureDate: String
)

data class DuffelPassengerRequest(
    @Json(name = "type") val type: String = "adult"
)

data class DuffelOfferResponseWrapper(
    @Json(name = "data") val data: DuffelOfferRequestData
)

data class DuffelOfferRequestData(
    @Json(name = "id") val id: String,
    @Json(name = "offers") val offers: List<DuffelOffer> = emptyList()
)

data class DuffelOffer(
    @Json(name = "id") val id: String,
    @Json(name = "total_amount") val totalAmount: String,
    @Json(name = "total_currency") val totalCurrency: String,
    @Json(name = "owner") val owner: DuffelCarrier,
    @Json(name = "slices") val slices: List<DuffelSlice> = emptyList(),
    @Json(name = "passengers") val passengers: List<DuffelOfferPassenger> = emptyList(),
    @Json(name = "conditions") val conditions: DuffelConditions? = null
)

data class DuffelCarrier(
    @Json(name = "name") val name: String,
    @Json(name = "iata_code") val iataCode: String? = null
)

data class DuffelSlice(
    @Json(name = "duration") val duration: String? = null,
    @Json(name = "origin") val origin: DuffelPlace,
    @Json(name = "destination") val destination: DuffelPlace,
    @Json(name = "segments") val segments: List<DuffelSegment> = emptyList()
)

data class DuffelPlace(
    @Json(name = "iata_code") val iataCode: String,
    @Json(name = "name") val name: String,
    @Json(name = "city_name") val cityName: String? = null
)

data class DuffelSegment(
    @Json(name = "id") val id: String,
    @Json(name = "departing_at") val departingAt: String,
    @Json(name = "arriving_at") val arrivingAt: String,
    @Json(name = "duration") val duration: String? = null,
    @Json(name = "origin") val origin: DuffelPlace? = null,
    @Json(name = "destination") val destination: DuffelPlace? = null,
    @Json(name = "marketing_carrier_flight_number") val flightNumber: String? = null,
    @Json(name = "aircraft") val aircraft: DuffelAircraft? = null,
    @Json(name = "operating_carrier") val operatingCarrier: DuffelCarrier? = null
)

data class DuffelAircraft(
    @Json(name = "iata_code") val iataCode: String? = null,
    @Json(name = "name") val name: String
)

data class DuffelOfferPassenger(
    @Json(name = "id") val id: String,
    @Json(name = "type") val type: String? = "adult"
)

data class DuffelConditions(
    @Json(name = "refund_before_departure") val refundBeforeDeparture: DuffelConditionRule? = null,
    @Json(name = "change_before_departure") val changeBeforeDeparture: DuffelConditionRule? = null
)

data class DuffelConditionRule(
    @Json(name = "allowed") val allowed: Boolean = false,
    @Json(name = "penalty_amount") val penaltyAmount: String? = null,
    @Json(name = "penalty_currency") val penaltyCurrency: String? = null
)

data class DuffelOrderCreateEnvelope(
    @Json(name = "data") val data: DuffelOrderCreatePayload
)

data class DuffelOrderCreatePayload(
    @Json(name = "type") val type: String = "instant",
    @Json(name = "selected_offers") val selectedOffers: List<String>,
    @Json(name = "payments") val payments: List<DuffelPaymentPayload>,
    @Json(name = "passengers") val passengers: List<DuffelOrderPassengerPayload>
)

data class DuffelPaymentPayload(
    @Json(name = "type") val type: String = "balance",
    @Json(name = "currency") val currency: String = "EUR",
    @Json(name = "amount") val amount: String
)

data class DuffelOrderPassengerPayload(
    @Json(name = "id") val id: String,
    @Json(name = "given_name") val givenName: String,
    @Json(name = "family_name") val familyName: String,
    @Json(name = "gender") val gender: String = "m",
    @Json(name = "title") val title: String = "mr",
    @Json(name = "born_on") val bornOn: String = "1990-05-15",
    @Json(name = "email") val email: String,
    @Json(name = "phone_number") val phoneNumber: String
)

data class DuffelOrderResponseWrapper(
    @Json(name = "data") val data: DuffelOrderData
)

data class DuffelOrderData(
    @Json(name = "id") val id: String,
    @Json(name = "booking_reference") val bookingReference: String,
    @Json(name = "total_amount") val totalAmount: String,
    @Json(name = "total_currency") val totalCurrency: String
)

/**
 * Payload para POST /air/order_cancellations (Duffel API v2)
 */
data class DuffelOrderCancellationCreateEnvelope(
    @Json(name = "data") val data: DuffelOrderCancellationCreatePayload
)

data class DuffelOrderCancellationCreatePayload(
    @Json(name = "order_id") val orderId: String
)

data class DuffelOrderCancellationResponseWrapper(
    @Json(name = "data") val data: DuffelOrderCancellationData
)

data class DuffelOrderCancellationData(
    @Json(name = "id") val id: String,
    @Json(name = "order_id") val orderId: String,
    @Json(name = "refund_amount") val refundAmount: String = "0.00",
    @Json(name = "refund_currency") val refundCurrency: String = "EUR",
    @Json(name = "refund_to") val refundTo: String? = "balance",
    @Json(name = "expires_at") val expiresAt: String? = null,
    @Json(name = "confirmed_at") val confirmedAt: String? = null
)

